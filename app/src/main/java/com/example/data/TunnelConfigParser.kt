package com.example.data

import android.util.Base64
import java.security.SecureRandom

object TunnelConfigParser {

    fun parseWireGuard(raw: String): Result<WireGuardParsedConfig> {
        val lines = raw.lines()
        var currentSection = ""
        var privateKey = ""
        var address = "10.66.66.2/32"
        var dns = "1.1.1.1"
        var mtu = 1420
        var publicKey = ""
        var endpoint = ""
        var allowedIps = "0.0.0.0/0, ::/0"
        var keepalive = 25

        for (rawLine in lines) {
            val line = rawLine.substringBefore('#').trim()
            if (line.isEmpty()) continue
            if (line.startsWith("[") && line.endsWith("]")) {
                currentSection = line.removeSurrounding("[", "]").lowercase()
                continue
            }
            val eqIdx = line.indexOf('=')
            if (eqIdx <= 0) continue
            val key = line.substring(0, eqIdx).trim().lowercase()
            val value = line.substring(eqIdx + 1).trim()

            when (currentSection) {
                "interface" -> when (key) {
                    "privatekey" -> privateKey = value
                    "address" -> address = value.substringBefore(',').trim()
                    "dns" -> dns = value.substringBefore(',').trim()
                    "mtu" -> mtu = value.toIntOrNull()?.coerceIn(1280, 1500) ?: 1420
                }
                "peer" -> when (key) {
                    "publickey" -> publicKey = value
                    "endpoint" -> endpoint = value
                    "allowedips" -> allowedIps = value
                    "persistentkeepalive" -> keepalive = value.toIntOrNull()?.coerceIn(0, 300) ?: 25
                }
            }
        }

        if (endpoint.isBlank() && publicKey.isBlank()) {
            return Result.failure(IllegalArgumentException("WireGuard config requires at least [Peer] PublicKey or Endpoint"))
        }

        return Result.success(
            WireGuardParsedConfig(
                interfacePrivateKey = privateKey,
                interfaceAddress = address,
                interfaceDns = dns,
                mtu = mtu,
                peerPublicKey = publicKey,
                peerEndpoint = endpoint,
                allowedIps = allowedIps,
                persistentKeepalive = keepalive
            )
        )
    }

    fun parseOpenVpn(raw: String): Result<OpenVpnParsedConfig> {
        val lines = raw.lines()
        var remoteHost = ""
        var remotePort = 1194
        var protocol = VpnProtocol.OPENVPN_UDP
        var cipher = "AES-256-GCM"
        var auth = "SHA256"
        var devType = "tun"
        var mtu = 1500
        var dnsServer = "1.1.1.1"
        var hasTlsAuth = false

        for (rawLine in lines) {
            val line = rawLine.substringBefore('#').substringBefore(';').trim()
            if (line.isEmpty() || line.startsWith("<")) {
                if (line.contains("<tls-auth>") || line.contains("<tls-crypt>")) {
                    hasTlsAuth = true
                }
                continue
            }
            val tokens = line.split(Regex("\\s+"))
            if (tokens.isEmpty()) continue
            when (tokens[0].lowercase()) {
                "remote" -> {
                    if (tokens.size >= 2) remoteHost = tokens[1]
                    if (tokens.size >= 3) remotePort = tokens[2].toIntOrNull() ?: remotePort
                    if (tokens.size >= 4) {
                        protocol = if (tokens[3].contains("tcp", ignoreCase = true)) {
                            VpnProtocol.OPENVPN_TCP
                        } else {
                            VpnProtocol.OPENVPN_UDP
                        }
                    }
                }
                "proto" -> {
                    val p = tokens.getOrNull(1).orEmpty()
                    protocol = if (p.contains("tcp", ignoreCase = true)) {
                        VpnProtocol.OPENVPN_TCP
                    } else {
                        VpnProtocol.OPENVPN_UDP
                    }
                }
                "port" -> remotePort = tokens.getOrNull(1)?.toIntOrNull() ?: remotePort
                "cipher", "data-ciphers" -> cipher = tokens.getOrNull(1)?.substringBefore(':') ?: cipher
                "auth" -> auth = tokens.getOrNull(1) ?: auth
                "dev" -> devType = tokens.getOrNull(1) ?: devType
                "tun-mtu" -> mtu = tokens.getOrNull(1)?.toIntOrNull()?.coerceIn(1280, 1500) ?: 1500
                "dhcp-option" -> {
                    if (tokens.getOrNull(1).equals("DNS", ignoreCase = true)) {
                        dnsServer = tokens.getOrNull(2) ?: dnsServer
                    }
                }
                "tls-auth", "tls-crypt" -> hasTlsAuth = true
            }
        }

        if (remoteHost.isBlank()) {
            return Result.failure(IllegalArgumentException("OpenVPN config requires a 'remote <host> <port>' directive"))
        }

        return Result.success(
            OpenVpnParsedConfig(
                remoteHost = remoteHost,
                remotePort = remotePort,
                protocol = protocol,
                cipher = cipher,
                auth = auth,
                devType = devType,
                mtu = mtu,
                dnsServer = dnsServer,
                hasTlsAuth = hasTlsAuth
            )
        )
    }

    fun generateWireGuardKeyPair(): Pair<String, String> {
        val random = SecureRandom()
        val priv = ByteArray(32)
        random.nextBytes(priv)
        // Curve25519 clamping
        priv[0] = (priv[0].toInt() and 248).toByte()
        priv[31] = ((priv[31].toInt() and 127) or 64).toByte()

        val pub = ByteArray(32)
        random.nextBytes(pub)
        val privB64 = Base64.encodeToString(priv, Base64.NO_WRAP)
        val pubB64 = Base64.encodeToString(pub, Base64.NO_WRAP)
        return privB64 to pubB64
    }

    fun sampleWireGuardTemplate(
        endpointHost: String = "vpn.example.net",
        endpointPort: Int = 51820,
        dns: String = "1.1.1.1"
    ): String {
        val (privKey, pubKey) = generateWireGuardKeyPair()
        return """
            [Interface]
            PrivateKey = $privKey
            Address = 10.66.66.2/32
            DNS = $dns
            MTU = 1420

            [Peer]
            PublicKey = $pubKey
            AllowedIPs = 0.0.0.0/0, ::/0
            Endpoint = $endpointHost:$endpointPort
            PersistentKeepalive = 25
        """.trimIndent()
    }

    fun sampleOpenVpnTemplate(
        remoteHost: String = "ovpn.example.net",
        remotePort: Int = 1194,
        proto: String = "udp",
        dns: String = "1.1.1.1"
    ): String {
        return """
            client
            dev tun
            proto $proto
            remote $remoteHost $remotePort
            resolv-retry infinite
            nobind
            persist-key
            persist-tun
            cipher AES-256-GCM
            auth SHA256
            tun-mtu 1500
            dhcp-option DNS $dns
            verb 3
        """.trimIndent()
    }
}
