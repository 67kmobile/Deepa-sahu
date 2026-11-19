package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.LogLevel
import com.example.data.VpnProtocol

@Entity(tableName = "connection_logs")
data class ConnectionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestampMs: Long = System.currentTimeMillis(),
    val level: String = LogLevel.INFO.name,
    val protocol: String = VpnProtocol.WIREGUARD.shortName,
    val serverLabel: String = "System",
    val message: String
)

@Entity(tableName = "custom_tunnels")
data class CustomTunnelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val countryCode: String,
    val protocolName: String,
    val host: String,
    val port: Int,
    val dnsServer: String,
    val mtu: Int,
    val configContent: String,
    val createdAtMs: Long = System.currentTimeMillis()
)
