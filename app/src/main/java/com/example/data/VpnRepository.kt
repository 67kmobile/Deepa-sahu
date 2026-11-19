package com.example.data

import com.example.data.local.ConnectionLogEntity
import com.example.data.local.CustomTunnelEntity
import com.example.data.local.VpnDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.UUID

class VpnRepository(
    private val dao: VpnDao,
    private val settingsRepository: SettingsRepository
) {
    val settingsFlow: Flow<VpnSettings> = settingsRepository.settingsFlow
    val logsFlow: Flow<List<ConnectionLogEntity>> = dao.observeLogs()
    val customTunnelsFlow: Flow<List<CustomTunnelEntity>> = dao.observeCustomTunnels()

    private val pingMapFlow = MutableStateFlow<Map<String, Int?>>(emptyMap())
    private val pingingIdsFlow = MutableStateFlow<Set<String>>(emptySet())

    private val _isPingingAll = MutableStateFlow(false)
    val isPingingAll: StateFlow<Boolean> = _isPingingAll.asStateFlow()

    val serversFlow: Flow<List<VpnServer>> = combine(
        customTunnelsFlow,
        settingsFlow,
        pingMapFlow,
        pingingIdsFlow
    ) { customEntities, settings, pings, pingingIds ->
        val customServers = customEntities.map { entity ->
            val proto = runCatching { VpnProtocol.valueOf(entity.protocolName) }
                .getOrDefault(VpnProtocol.WIREGUARD)
            VpnServer(
                id = entity.id,
                countryName = entity.name,
                cityName = "${proto.shortName} Custom",
                countryCode = entity.countryCode,
                flagEmoji = "🔐",
                host = entity.host,
                probePort = entity.port,
                wgPort = entity.port,
                ovpnPort = entity.port,
                region = ServerRegion.CUSTOM,
                loadPercent = 10,
                pingMs = pings[entity.id],
                isPinging = entity.id in pingingIds,
                isFavorite = entity.id in settings.favoriteIds,
                isCustomProfile = true,
                customProtocol = proto,
                customConfigRaw = entity.configContent,
                customDns = entity.dnsServer,
                customMtu = entity.mtu
            )
        }

        val builtIn = ServerCatalog.builtInServers.map { server ->
            server.copy(
                pingMs = pings[server.id],
                isPinging = server.id in pingingIds,
                isFavorite = server.id in settings.favoriteIds
            )
        }

        customServers + builtIn
    }

    suspend fun selectServer(serverId: String) = settingsRepository.setSelectedServer(serverId)

    suspend fun selectProtocol(protocol: VpnProtocol) = settingsRepository.setSelectedProtocol(protocol)

    suspend fun setAutoReconnect(enabled: Boolean) {
        settingsRepository.setAutoReconnect(enabled)
        addLog(
            level = LogLevel.INFO,
            protocol = "SYS",
            serverLabel = "Policy",
            message = "Auto-Reconnect ${if (enabled) "enabled" else "disabled"}"
        )
    }

    suspend fun setDnsLeakProtection(enabled: Boolean) {
        settingsRepository.setDnsLeakProtection(enabled)
        addLog(
            level = LogLevel.DNS,
            protocol = "DNS",
            serverLabel = "Shield",
            message = "DNS Leak Protection ${if (enabled) "activated (strict TUN DNS routing)" else "relaxed"}"
        )
    }

    suspend fun setIpv6LeakBlock(enabled: Boolean) {
        settingsRepository.setIpv6LeakBlock(enabled)
        addLog(
            level = LogLevel.DNS,
            protocol = "DNS",
            serverLabel = "IPv6",
            message = "IPv6 Leak Block ${if (enabled) "enabled" else "disabled"}"
        )
    }

    suspend fun setDnsServers(primary: String, secondary: String) {
        settingsRepository.setDnsServers(primary, secondary)
        addLog(
            level = LogLevel.DNS,
            protocol = "DNS",
            serverLabel = "Resolver",
            message = "Pinned privacy DNS to $primary / $secondary"
        )
    }

    suspend fun setMtuOverride(mtu: Int) {
        settingsRepository.setMtuOverride(mtu)
        addLog(
            level = LogLevel.INFO,
            protocol = "TUN",
            serverLabel = "MTU",
            message = "Tunnel MTU updated to $mtu bytes"
        )
    }

    suspend fun toggleFavorite(serverId: String) = settingsRepository.toggleFavorite(serverId)

    suspend fun toggleBypassedPackage(packageName: String) =
        settingsRepository.toggleBypassedPackage(packageName)

    suspend fun pingServer(server: VpnServer): Int? {
        pingingIdsFlow.update { it + server.id }
        val rtt = PingEngine.measureTcpLatencyMs(server.host, server.probePort)
        pingMapFlow.update { it + (server.id to rtt) }
        pingingIdsFlow.update { it - server.id }
        return rtt
    }

    suspend fun pingAllServers(servers: List<VpnServer>) {
        if (_isPingingAll.value) return
        _isPingingAll.value = true
        try {
            coroutineScope {
                servers.map { server ->
                    async(Dispatchers.IO) {
                        pingServer(server)
                    }
                }.awaitAll()
            }
        } finally {
            _isPingingAll.value = false
        }
    }

    suspend fun importOrSaveCustomTunnel(
        name: String,
        protocol: VpnProtocol,
        rawConfig: String
    ): Result<CustomTunnelEntity> = withContext(Dispatchers.IO) {
        val trimmedName = name.trim().ifEmpty {
            if (protocol == VpnProtocol.WIREGUARD) "WireGuard Tunnel" else "OpenVPN Tunnel"
        }

        val entity = when (protocol) {
            VpnProtocol.WIREGUARD -> {
                val parsed = TunnelConfigParser.parseWireGuard(rawConfig).getOrElse {
                    return@withContext Result.failure(it)
                }
                val endpointHost = parsed.peerEndpoint.substringBefore(':').ifBlank { "1.1.1.1" }
                val endpointPort = parsed.peerEndpoint.substringAfter(':', "51820").toIntOrNull() ?: 51820
                CustomTunnelEntity(
                    id = "custom-${UUID.randomUUID().toString().take(8)}",
                    name = trimmedName,
                    countryCode = "WG",
                    protocolName = VpnProtocol.WIREGUARD.name,
                    host = endpointHost,
                    port = endpointPort,
                    dnsServer = parsed.interfaceDns,
                    mtu = parsed.mtu,
                    configContent = rawConfig.trim()
                )
            }
            VpnProtocol.OPENVPN_UDP, VpnProtocol.OPENVPN_TCP -> {
                val parsed = TunnelConfigParser.parseOpenVpn(rawConfig).getOrElse {
                    return@withContext Result.failure(it)
                }
                CustomTunnelEntity(
                    id = "custom-${UUID.randomUUID().toString().take(8)}",
                    name = trimmedName,
                    countryCode = "OVPN",
                    protocolName = parsed.protocol.name,
                    host = parsed.remoteHost,
                    port = parsed.remotePort,
                    dnsServer = parsed.dnsServer,
                    mtu = parsed.mtu,
                    configContent = rawConfig.trim()
                )
            }
        }

        dao.upsertCustomTunnel(entity)
        settingsRepository.setSelectedServer(entity.id)
        settingsRepository.setSelectedProtocol(VpnProtocol.valueOf(entity.protocolName))
        addLog(
            level = LogLevel.INFO,
            protocol = VpnProtocol.valueOf(entity.protocolName).shortName,
            serverLabel = entity.name,
            message = "Imported profile (${entity.host}:${entity.port}, MTU ${entity.mtu})"
        )
        Result.success(entity)
    }

    suspend fun deleteCustomTunnel(id: String) {
        dao.deleteCustomTunnel(id)
        addLog(
            level = LogLevel.INFO,
            protocol = "CFG",
            serverLabel = "Profile",
            message = "Removed custom tunnel profile $id"
        )
    }

    suspend fun addLog(
        level: LogLevel,
        protocol: String,
        serverLabel: String,
        message: String
    ) {
        dao.insertLog(
            ConnectionLogEntity(
                timestampMs = System.currentTimeMillis(),
                level = level.name,
                protocol = protocol,
                serverLabel = serverLabel,
                message = message
            )
        )
    }

    suspend fun clearLogs() = dao.clearLogs()
}
