package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.VpnService
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.VioletTunnelApp
import com.example.data.DnsLeakCheckResult
import com.example.data.LogLevel
import com.example.data.PingEngine
import com.example.data.ServerCatalog
import com.example.data.VpnConnectionState
import com.example.data.VpnProtocol
import com.example.data.VpnServer
import com.example.data.VpnSettings
import com.example.data.local.ConnectionLogEntity
import com.example.vpn.VioletVpnService
import com.example.vpn.VpnStateHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SplitAppItem(
    val packageName: String,
    val appName: String
)

data class VpnUiState(
    val settings: VpnSettings = VpnSettings(),
    val servers: List<VpnServer> = ServerCatalog.builtInServers,
    val selectedServer: VpnServer = ServerCatalog.builtInServers.first(),
    val connectionState: VpnConnectionState = VpnConnectionState.Disconnected,
    val elapsedSeconds: Long = 0L,
    val downloadBps: Long = 0L,
    val uploadBps: Long = 0L,
    val totalDownloadBytes: Long = 0L,
    val totalUploadBytes: Long = 0L,
    val speedHistoryDown: List<Float> = List(24) { 0f },
    val speedHistoryUp: List<Float> = List(24) { 0f },
    val logs: List<ConnectionLogEntity> = emptyList(),
    val isPingingAll: Boolean = false,
    val isRunningDnsTest: Boolean = false,
    val lastDnsCheck: DnsLeakCheckResult? = null,
    val installedApps: List<SplitAppItem> = emptyList()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as VioletTunnelApp).vpnRepository

    private val elapsedSecondsFlow = MutableStateFlow(0L)
    private val isRunningDnsTestFlow = MutableStateFlow(false)
    private val installedAppsFlow = MutableStateFlow<List<SplitAppItem>>(emptyList())

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    val uiState: StateFlow<VpnUiState> = combine(
        repo.settingsFlow,
        repo.serversFlow,
        VpnStateHolder.connectionState,
        elapsedSecondsFlow,
        VpnStateHolder.downloadBytesPerSec,
        VpnStateHolder.uploadBytesPerSec,
        VpnStateHolder.totalDownloadBytes,
        VpnStateHolder.totalUploadBytes,
        VpnStateHolder.speedHistoryDown,
        VpnStateHolder.speedHistoryUp,
        repo.logsFlow,
        repo.isPingingAll,
        isRunningDnsTestFlow,
        VpnStateHolder.lastDnsCheck,
        installedAppsFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val settings = values[0] as VpnSettings
        @Suppress("UNCHECKED_CAST")
        val servers = values[1] as List<VpnServer>
        val connState = values[2] as VpnConnectionState
        val elapsed = values[3] as Long
        val downBps = values[4] as Long
        val upBps = values[5] as Long
        val totalDown = values[6] as Long
        val totalUp = values[7] as Long
        @Suppress("UNCHECKED_CAST")
        val histDown = values[8] as List<Float>
        @Suppress("UNCHECKED_CAST")
        val histUp = values[9] as List<Float>
        @Suppress("UNCHECKED_CAST")
        val logs = values[10] as List<ConnectionLogEntity>
        val pingingAll = values[11] as Boolean
        val runningDns = values[12] as Boolean
        val lastDns = values[13] as DnsLeakCheckResult?
        @Suppress("UNCHECKED_CAST")
        val apps = values[14] as List<SplitAppItem>

        val selected = servers.firstOrNull { it.id == settings.selectedServerId }
            ?: servers.firstOrNull()
            ?: ServerCatalog.builtInServers.first()

        VpnUiState(
            settings = settings,
            servers = servers,
            selectedServer = selected,
            connectionState = connState,
            elapsedSeconds = elapsed,
            downloadBps = downBps,
            uploadBps = upBps,
            totalDownloadBytes = totalDown,
            totalUploadBytes = totalUp,
            speedHistoryDown = histDown,
            speedHistoryUp = histUp,
            logs = logs,
            isPingingAll = pingingAll,
            isRunningDnsTest = runningDns,
            lastDnsCheck = lastDns,
            installedApps = apps
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VpnUiState()
    )

    init {
        startConnectionTimerTicker()
        initialLatencySweep()
        loadInstalledApps()
    }

    private fun startConnectionTimerTicker() {
        viewModelScope.launch {
            while (isActive) {
                val state = VpnStateHolder.connectionState.value
                elapsedSecondsFlow.value = if (state is VpnConnectionState.Connected) {
                    ((System.currentTimeMillis() - state.sinceEpochMs) / 1000L).coerceAtLeast(0L)
                } else {
                    0L
                }
                delay(1000L)
            }
        }
    }

    private fun initialLatencySweep() {
        viewModelScope.launch {
            val servers = repo.serversFlow.first()
            repo.pingAllServers(servers)
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val pm = context.packageManager
            val apps = runCatching {
                pm.getInstalledApplications(0)
                    .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && it.packageName != context.packageName }
                    .map { info ->
                        SplitAppItem(
                            packageName = info.packageName,
                            appName = pm.getApplicationLabel(info).toString()
                        )
                    }
                    .sortedBy { it.appName.lowercase() }
            }.getOrDefault(emptyList())
            installedAppsFlow.value = apps
        }
    }

    fun prepareVpnIntent(context: Context): Intent? = VpnService.prepare(context)

    fun connectVpn(context: Context) {
        val current = uiState.value
        val server = current.selectedServer
        val protocol = server.customProtocol ?: current.settings.selectedProtocol
        val port = when (protocol) {
            VpnProtocol.WIREGUARD -> server.wgPort
            VpnProtocol.OPENVPN_UDP, VpnProtocol.OPENVPN_TCP -> server.ovpnPort
        }

        val intent = Intent(context, VioletVpnService::class.java).apply {
            action = VioletVpnService.ACTION_CONNECT
            putExtra(VioletVpnService.EXTRA_SERVER_ID, server.id)
            putExtra(VioletVpnService.EXTRA_SERVER_NAME, "${server.countryName} • ${server.cityName}")
            putExtra(VioletVpnService.EXTRA_COUNTRY_CODE, server.countryCode)
            putExtra(VioletVpnService.EXTRA_SERVER_HOST, server.host)
            putExtra(VioletVpnService.EXTRA_SERVER_PORT, port)
            putExtra(VioletVpnService.EXTRA_PROTOCOL, protocol.name)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun disconnectVpn(context: Context) {
        val intent = Intent(context, VioletVpnService::class.java).apply {
            action = VioletVpnService.ACTION_DISCONNECT
        }
        context.startService(intent)
    }

    fun selectServer(context: Context, server: VpnServer, connectImmediately: Boolean = false) {
        viewModelScope.launch {
            repo.selectServer(server.id)
            server.customProtocol?.let { repo.selectProtocol(it) }
            repo.pingServer(server)
            val isConnected = VpnStateHolder.connectionState.value is VpnConnectionState.Connected
            if (connectImmediately || isConnected) {
                if (prepareVpnIntent(context) == null) {
                    connectVpn(context)
                }
            }
        }
    }

    fun selectProtocol(context: Context, protocol: VpnProtocol) {
        viewModelScope.launch {
            repo.selectProtocol(protocol)
            repo.addLog(
                level = LogLevel.HANDSHAKE,
                protocol = protocol.shortName,
                serverLabel = uiState.value.selectedServer.countryName,
                message = "Switched tunnel engine to ${protocol.displayName} (${protocol.cipherLabel})"
            )
            if (VpnStateHolder.connectionState.value is VpnConnectionState.Connected) {
                if (prepareVpnIntent(context) == null) {
                    connectVpn(context)
                }
            }
        }
    }

    fun pingAllServers() {
        viewModelScope.launch {
            repo.pingAllServers(uiState.value.servers)
        }
    }

    fun pingServer(server: VpnServer) {
        viewModelScope.launch {
            repo.pingServer(server)
        }
    }

    fun toggleFavorite(serverId: String) {
        viewModelScope.launch { repo.toggleFavorite(serverId) }
    }

    fun setAutoReconnect(enabled: Boolean) {
        viewModelScope.launch { repo.setAutoReconnect(enabled) }
    }

    fun setDnsLeakProtection(enabled: Boolean) {
        viewModelScope.launch { repo.setDnsLeakProtection(enabled) }
    }

    fun setIpv6LeakBlock(enabled: Boolean) {
        viewModelScope.launch { repo.setIpv6LeakBlock(enabled) }
    }

    fun setDnsServers(primary: String, secondary: String) {
        viewModelScope.launch {
            repo.setDnsServers(primary, secondary)
            _toastEvents.tryEmit("DNS pinned to $primary")
        }
    }

    fun setMtuOverride(mtu: Int) {
        viewModelScope.launch { repo.setMtuOverride(mtu) }
    }

    fun toggleBypassedPackage(packageName: String) {
        viewModelScope.launch { repo.toggleBypassedPackage(packageName) }
    }

    fun importCustomProfile(name: String, protocol: VpnProtocol, rawConfig: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repo.importOrSaveCustomTunnel(name, protocol, rawConfig)
            result.onSuccess { entity ->
                _toastEvents.tryEmit("Saved ${entity.name} (${protocol.shortName})")
                onDone(true)
            }.onFailure { err ->
                _toastEvents.tryEmit(err.message ?: "Invalid configuration format")
                onDone(false)
            }
        }
    }

    fun deleteCustomProfile(id: String) {
        viewModelScope.launch {
            repo.deleteCustomTunnel(id)
            _toastEvents.tryEmit("Custom tunnel removed")
        }
    }

    fun runDnsLeakTest() {
        if (isRunningDnsTestFlow.value) return
        viewModelScope.launch {
            isRunningDnsTestFlow.value = true
            val state = uiState.value
            val vpnActive = state.connectionState is VpnConnectionState.Connected
            val result = PingEngine.runDnsLeakVerification(
                configuredDns = state.settings.primaryDns,
                dnsProtectionEnabled = state.settings.dnsLeakProtection,
                vpnActive = vpnActive
            )
            VpnStateHolder.updateDnsCheck(result)
            repo.addLog(
                level = if (result.leakDetected) LogLevel.WARN else LogLevel.DNS,
                protocol = "DNS",
                serverLabel = state.selectedServer.countryName,
                message = result.summary
            )
            isRunningDnsTestFlow.value = false
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repo.clearLogs()
            _toastEvents.tryEmit("Connection logs cleared")
        }
    }
}
