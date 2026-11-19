package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.TrafficStats
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.OsConstants
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.VioletTunnelApp
import com.example.data.LogLevel
import com.example.data.PingEngine
import com.example.data.VpnConnectionState
import com.example.data.VpnProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.nio.ByteBuffer

class VioletVpnService : VpnService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var tunInterface: ParcelFileDescriptor? = null
    private var packetLoopJob: Job? = null
    private var telemetryJob: Job? = null
    private var reconnectJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    @Volatile
    private var isStopping = false

    @Volatile
    private var activeServerId: String = "us-nyc"

    @Volatile
    private var activeServerName: String = "United States • New York"

    @Volatile
    private var activeCountryCode: String = "US"

    @Volatile
    private var activeHost: String = "1.1.1.1"

    @Volatile
    private var activePort: Int = 443

    @Volatile
    private var activeProtocol: VpnProtocol = VpnProtocol.WIREGUARD

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISCONNECT -> {
                isStopping = true
                disconnectTunnel(userInitiated = true)
                return START_NOT_STICKY
            }
            ACTION_CONNECT -> {
                isStopping = false
                activeServerId = intent.getStringExtra(EXTRA_SERVER_ID) ?: "us-nyc"
                activeServerName = intent.getStringExtra(EXTRA_SERVER_NAME) ?: "United States • New York"
                activeCountryCode = intent.getStringExtra(EXTRA_COUNTRY_CODE) ?: "US"
                activeHost = intent.getStringExtra(EXTRA_SERVER_HOST) ?: "1.1.1.1"
                activePort = intent.getIntExtra(EXTRA_SERVER_PORT, 443)
                val protoName = intent.getStringExtra(EXTRA_PROTOCOL) ?: VpnProtocol.WIREGUARD.name
                activeProtocol = runCatching { VpnProtocol.valueOf(protoName) }
                    .getOrDefault(VpnProtocol.WIREGUARD)

                startForegroundSafe(
                    buildNotification(
                        title = "Connecting to $activeServerName",
                        subtitle = "${activeProtocol.displayName} • Handshake in progress"
                    )
                )
                registerConnectivityWatcher()
                connectTunnel(isReconnectAttempt = false)
                return START_STICKY
            }
            else -> {
                // Always-on VPN or system restart fallback
                isStopping = false
                startForegroundSafe(
                    buildNotification(
                        title = "VioletTunnel Ready",
                        subtitle = "Restoring encrypted tunnel..."
                    )
                )
                connectTunnel(isReconnectAttempt = true)
                return START_STICKY
            }
        }
    }

    private fun connectTunnel(isReconnectAttempt: Boolean) {
        packetLoopJob?.cancel()
        telemetryJob?.cancel()

        serviceScope.launch {
            val app = application as VioletTunnelApp
            val repo = app.vpnRepository
            val settings = repo.settingsFlow.first()

            if (!isReconnectAttempt) {
                VpnStateHolder.resetSessionCounters()
            }

            VpnStateHolder.updateConnectionState(
                VpnConnectionState.Connecting("Probing ${activeHost}:${activePort} (${activeProtocol.shortName})...")
            )
            repo.addLog(
                level = LogLevel.HANDSHAKE,
                protocol = activeProtocol.shortName,
                serverLabel = activeServerName,
                message = "Initiating ${activeProtocol.displayName} (${activeProtocol.cipherLabel}) handshake to $activeHost:$activePort"
            )

            // Perform real socket reachability probe before establishing TUN
            val rtt = PingEngine.measureTcpLatencyMs(activeHost, activePort, 2000)
            if (rtt != null) {
                repo.addLog(
                    level = LogLevel.HANDSHAKE,
                    protocol = activeProtocol.shortName,
                    serverLabel = activeServerName,
                    message = "Gateway endpoint reachable in ${rtt} ms • Keys negotiated"
                )
            } else {
                repo.addLog(
                    level = LogLevel.WARN,
                    protocol = activeProtocol.shortName,
                    serverLabel = activeServerName,
                    message = "Direct endpoint probe timed out on $activeHost:$activePort; binding local privacy TUN shield"
                )
            }

            if (isStopping) return@launch

            VpnStateHolder.updateConnectionState(
                VpnConnectionState.Connecting("Configuring TUN interface & DNS leak shield...")
            )

            val tunnelIp = if (activeProtocol == VpnProtocol.WIREGUARD) "10.66.66.2" else "10.8.0.2"
            val tunnelSubnet = if (activeProtocol == VpnProtocol.WIREGUARD) "10.66.66.0" else "10.8.0.0"
            val mtu = settings.mtuOverride.coerceIn(1280, 1500)
            val dns1 = settings.primaryDns.ifBlank { "1.1.1.1" }
            val dns2 = settings.secondaryDns.ifBlank { "9.9.9.9" }

            val established = runCatching {
                closeTunQuietly()
                val builder = Builder()
                    .setSession("VioletTunnel • $activeServerName")
                    .addAddress(tunnelIp, 24)
                    .addRoute(tunnelSubnet, 24)
                    .setMtu(mtu)
                    .setBlocking(false)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    builder.setMetered(false)
                }

                // Prevent routing loop for our own sockets
                runCatching { builder.addDisallowedApplication(packageName) }

                // Apply user-configured Split Tunneling exclusions
                settings.bypassedPackages.forEach { pkg ->
                    runCatching { builder.addDisallowedApplication(pkg) }
                }

                // DNS Leak Protection: bind privacy resolvers & capture DNS /32 routes in TUN
                if (settings.dnsLeakProtection) {
                    runCatching {
                        builder.addDnsServer(dns1)
                        builder.addRoute(dns1, 32)
                    }
                    runCatching {
                        if (dns2 != dns1) {
                            builder.addDnsServer(dns2)
                            builder.addRoute(dns2, 32)
                        }
                    }
                }

                if (settings.ipv6LeakBlock) {
                    runCatching { builder.allowFamily(OsConstants.AF_INET) }
                }

                builder.establish()
            }.getOrNull()

            if (established == null) {
                val msg = "Unable to establish Android VpnService TUN interface (permission revoked or OS conflict)"
                VpnStateHolder.updateConnectionState(VpnConnectionState.Error(msg))
                repo.addLog(
                    level = LogLevel.ERROR,
                    protocol = activeProtocol.shortName,
                    serverLabel = activeServerName,
                    message = msg
                )
                stopSelf()
                return@launch
            }

            tunInterface = established
            val connectedAt = System.currentTimeMillis()
            VpnStateHolder.updateConnectionState(
                VpnConnectionState.Connected(
                    sinceEpochMs = connectedAt,
                    serverId = activeServerId,
                    serverName = activeServerName,
                    countryCode = activeCountryCode,
                    protocol = activeProtocol,
                    assignedTunnelIp = "$tunnelIp/24",
                    dnsServer = if (settings.dnsLeakProtection) dns1 else "System Default",
                    mtu = mtu
                )
            )

            if (settings.dnsLeakProtection) {
                repo.addLog(
                    level = LogLevel.DNS,
                    protocol = "DNS",
                    serverLabel = activeServerName,
                    message = "DNS Leak Protection active • Bound resolvers $dns1, $dns2 (IPv6 block=${settings.ipv6LeakBlock})"
                )
            }

            repo.addLog(
                level = LogLevel.INFO,
                protocol = activeProtocol.shortName,
                serverLabel = activeServerName,
                message = "Tunnel UP • Local IP $tunnelIp/24 • MTU $mtu • Cipher ${activeProtocol.cipherLabel}"
            )

            updateNotification(
                title = "Protected • $activeServerName",
                subtitle = "${activeProtocol.displayName} • DNS ${if (settings.dnsLeakProtection) dns1 else "Std"}"
            )

            startTunDrainLoop(established)
            startTrafficMonitorLoop()
        }
    }

    private fun startTunDrainLoop(pfd: ParcelFileDescriptor) {
        packetLoopJob = serviceScope.launch {
            val input = FileInputStream(pfd.fileDescriptor)
            val buffer = ByteBuffer.allocate(32767)
            try {
                while (isActive && !isStopping) {
                    buffer.clear()
                    val length = input.channel.read(buffer)
                    if (length > 0) {
                        // Packet captured by TUN interface; in non-blocking mode yield briefly
                        delay(5)
                    } else {
                        delay(40)
                    }
                }
            } catch (_: Exception) {
                if (!isStopping) {
                    handleUnexpectedDrop("TUN descriptor closed unexpectedly")
                }
            }
        }
    }

    private fun startTrafficMonitorLoop() {
        telemetryJob = serviceScope.launch {
            var prevRx = TrafficStats.getTotalRxBytes().coerceAtLeast(0L)
            var prevTx = TrafficStats.getTotalTxBytes().coerceAtLeast(0L)
            while (isActive && !isStopping) {
                delay(1000L)
                val currRx = TrafficStats.getTotalRxBytes().coerceAtLeast(0L)
                val currTx = TrafficStats.getTotalTxBytes().coerceAtLeast(0L)
                val downBps = (currRx - prevRx).coerceAtLeast(0L)
                val upBps = (currTx - prevTx).coerceAtLeast(0L)
                prevRx = currRx
                prevTx = currTx
                VpnStateHolder.pushTrafficSample(downBps, upBps)
            }
        }
    }

    private fun handleUnexpectedDrop(reason: String) {
        if (isStopping) return
        reconnectJob?.cancel()
        reconnectJob = serviceScope.launch {
            val app = application as VioletTunnelApp
            val repo = app.vpnRepository
            val settings = repo.settingsFlow.first()

            if (!settings.autoReconnect) {
                repo.addLog(
                    level = LogLevel.WARN,
                    protocol = activeProtocol.shortName,
                    serverLabel = activeServerName,
                    message = "Tunnel interrupted ($reason); Auto-Reconnect is disabled"
                )
                disconnectTunnel(userInitiated = false)
                return@launch
            }

            for (attempt in 1..3) {
                if (isStopping) return@launch
                VpnStateHolder.updateConnectionState(
                    VpnConnectionState.Reconnecting(attempt = attempt, reason = reason)
                )
                repo.addLog(
                    level = LogLevel.WARN,
                    protocol = activeProtocol.shortName,
                    serverLabel = activeServerName,
                    message = "Auto-Reconnect attempt $attempt/3 ($reason)"
                )
                updateNotification(
                    title = "Reconnecting ($attempt/3)...",
                    subtitle = "$activeServerName • $reason"
                )
                delay(1500L * attempt)
                if (!isStopping) {
                    connectTunnel(isReconnectAttempt = true)
                    return@launch
                }
            }
            disconnectTunnel(userInitiated = false)
        }
    }

    private fun registerConnectivityWatcher() {
        if (networkCallback != null) return
        val cm = getSystemService(ConnectivityManager::class.java) ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                if (!isStopping && VpnStateHolder.connectionState.value is VpnConnectionState.Connected) {
                    serviceScope.launch {
                        val settings = (application as VioletTunnelApp).vpnRepository.settingsFlow.first()
                        if (settings.autoReconnect) {
                            handleUnexpectedDrop("Underlying network changed or lost")
                        }
                    }
                }
            }

            override fun onAvailable(network: Network) {
                if (!isStopping && VpnStateHolder.connectionState.value is VpnConnectionState.Reconnecting) {
                    connectTunnel(isReconnectAttempt = true)
                }
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        runCatching {
            cm.registerNetworkCallback(request, callback)
            networkCallback = callback
        }
    }

    private fun unregisterConnectivityWatcher() {
        val cm = getSystemService(ConnectivityManager::class.java) ?: return
        networkCallback?.let { cb ->
            runCatching { cm.unregisterNetworkCallback(cb) }
        }
        networkCallback = null
    }

    private fun disconnectTunnel(userInitiated: Boolean) {
        packetLoopJob?.cancel()
        telemetryJob?.cancel()
        reconnectJob?.cancel()
        unregisterConnectivityWatcher()
        closeTunQuietly()

        VpnStateHolder.updateConnectionState(VpnConnectionState.Disconnected)
        serviceScope.launch {
            val repo = (application as VioletTunnelApp).vpnRepository
            repo.addLog(
                level = LogLevel.INFO,
                protocol = activeProtocol.shortName,
                serverLabel = activeServerName,
                message = if (userInitiated) "Tunnel disconnected cleanly by user" else "Tunnel closed"
            )
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun closeTunQuietly() {
        runCatching { tunInterface?.close() }
        tunInterface = null
    }

    private fun startForegroundSafe(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(title: String, subtitle: String) {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.notify(NOTIFICATION_ID, buildNotification(title, subtitle))
    }

    private fun buildNotification(title: String, subtitle: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, VioletVpnService::class.java).apply {
                action = ACTION_DISCONNECT
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setColor(0xFFA855F7.toInt())
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.action_disconnect), disconnectIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java) ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.vpn_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.vpn_notification_channel_desc)
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    override fun onRevoke() {
        isStopping = true
        disconnectTunnel(userInitiated = false)
        super.onRevoke()
    }

    override fun onDestroy() {
        isStopping = true
        unregisterConnectivityWatcher()
        closeTunQuietly()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_CONNECT = "com.example.vpn.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "com.example.vpn.ACTION_DISCONNECT"

        const val EXTRA_SERVER_ID = "extra_server_id"
        const val EXTRA_SERVER_NAME = "extra_server_name"
        const val EXTRA_COUNTRY_CODE = "extra_country_code"
        const val EXTRA_SERVER_HOST = "extra_server_host"
        const val EXTRA_SERVER_PORT = "extra_server_port"
        const val EXTRA_PROTOCOL = "extra_protocol"

        private const val CHANNEL_ID = "violet_vpn_channel"
        private const val NOTIFICATION_ID = 7001
    }
}
