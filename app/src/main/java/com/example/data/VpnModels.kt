package com.example.data

enum class VpnProtocol(
    val displayName: String,
    val shortName: String,
    val cipherLabel: String,
    val defaultPort: Int,
    val defaultMtu: Int,
    val transport: String
) {
    WIREGUARD(
        displayName = "WireGuard",
        shortName = "WG",
        cipherLabel = "ChaCha20-Poly1305 • Curve25519",
        defaultPort = 51820,
        defaultMtu = 1420,
        transport = "UDP Kernel-Style"
    ),
    OPENVPN_UDP(
        displayName = "OpenVPN (UDP)",
        shortName = "OVPN-UDP",
        cipherLabel = "AES-256-GCM • TLS 1.3",
        defaultPort = 1194,
        defaultMtu = 1500,
        transport = "UDP High-Speed"
    ),
    OPENVPN_TCP(
        displayName = "OpenVPN (TCP)",
        shortName = "OVPN-TCP",
        cipherLabel = "AES-256-GCM • SHA256",
        defaultPort = 443,
        defaultMtu = 1500,
        transport = "TCP Firewall-Safe"
    )
}

enum class ServerRegion(val label: String) {
    ALL("All"),
    AMERICAS("Americas"),
    EUROPE("Europe"),
    ASIA_PACIFIC("Asia-Pacific"),
    CUSTOM("Custom")
}

sealed class VpnConnectionState {
    data object Disconnected : VpnConnectionState()
    data class Connecting(val stepMessage: String) : VpnConnectionState()
    data class Connected(
        val sinceEpochMs: Long,
        val serverId: String,
        val serverName: String,
        val countryCode: String,
        val protocol: VpnProtocol,
        val assignedTunnelIp: String,
        val dnsServer: String,
        val mtu: Int
    ) : VpnConnectionState()
    data class Reconnecting(val attempt: Int, val reason: String) : VpnConnectionState()
    data class Error(val message: String) : VpnConnectionState()
}

data class VpnServer(
    val id: String,
    val countryName: String,
    val cityName: String,
    val countryCode: String,
    val flagEmoji: String,
    val host: String,
    val probePort: Int,
    val wgPort: Int,
    val ovpnPort: Int,
    val region: ServerRegion,
    val loadPercent: Int,
    val pingMs: Int? = null,
    val isPinging: Boolean = false,
    val isFavorite: Boolean = false,
    val isCustomProfile: Boolean = false,
    val customProtocol: VpnProtocol? = null,
    val customConfigRaw: String = "",
    val customDns: String? = null,
    val customMtu: Int? = null
)

data class WireGuardParsedConfig(
    val interfacePrivateKey: String = "",
    val interfaceAddress: String = "10.66.66.2/32",
    val interfaceDns: String = "1.1.1.1",
    val mtu: Int = 1420,
    val peerPublicKey: String = "",
    val peerEndpoint: String = "",
    val allowedIps: String = "0.0.0.0/0, ::/0",
    val persistentKeepalive: Int = 25
)

data class OpenVpnParsedConfig(
    val remoteHost: String = "",
    val remotePort: Int = 1194,
    val protocol: VpnProtocol = VpnProtocol.OPENVPN_UDP,
    val cipher: String = "AES-256-GCM",
    val auth: String = "SHA256",
    val devType: String = "tun",
    val mtu: Int = 1500,
    val dnsServer: String = "1.1.1.1",
    val hasTlsAuth: Boolean = false
)

enum class LogLevel(val badge: String) {
    INFO("INFO"),
    HANDSHAKE("CRYPTO"),
    DNS("DNS"),
    WARN("WARN"),
    ERROR("ERR")
}

data class VpnSettings(
    val selectedServerId: String = "us-nyc",
    val selectedProtocol: VpnProtocol = VpnProtocol.WIREGUARD,
    val autoReconnect: Boolean = true,
    val dnsLeakProtection: Boolean = true,
    val ipv6LeakBlock: Boolean = true,
    val primaryDns: String = "1.1.1.1",
    val secondaryDns: String = "9.9.9.9",
    val mtuOverride: Int = 1420,
    val favoriteIds: Set<String> = setOf("us-nyc", "jp-tyo", "ch-zrh"),
    val bypassedPackages: Set<String> = emptySet()
)

data class DnsPreset(
    val name: String,
    val primaryIp: String,
    val secondaryIp: String,
    val description: String
)

object DnsPresets {
    val all = listOf(
        DnsPreset("Cloudflare Zero-Log", "1.1.1.1", "1.0.0.1", "Ultra-low latency DNSSEC resolver"),
        DnsPreset("Quad9 Malware Shield", "9.9.9.9", "149.112.112.112", "Threat-blocking Swiss privacy DNS"),
        DnsPreset("AdGuard Privacy", "94.140.14.14", "94.140.15.15", "Blocks trackers, telemetry & ads"),
        DnsPreset("Google Public", "8.8.8.8", "8.8.4.4", "High-availability global anycast")
    )
}
