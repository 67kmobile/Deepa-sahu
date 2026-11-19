package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.vpnDataStore: DataStore<Preferences> by preferencesDataStore(name = "violet_vpn_prefs")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SELECTED_SERVER_ID = stringPreferencesKey("selected_server_id")
        val SELECTED_PROTOCOL = stringPreferencesKey("selected_protocol")
        val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        val DNS_LEAK_PROTECTION = booleanPreferencesKey("dns_leak_protection")
        val IPV6_LEAK_BLOCK = booleanPreferencesKey("ipv6_leak_block")
        val PRIMARY_DNS = stringPreferencesKey("primary_dns")
        val SECONDARY_DNS = stringPreferencesKey("secondary_dns")
        val MTU_OVERRIDE = intPreferencesKey("mtu_override")
        val FAVORITE_IDS = stringSetPreferencesKey("favorite_ids")
        val BYPASSED_PACKAGES = stringSetPreferencesKey("bypassed_packages")
    }

    val settingsFlow: Flow<VpnSettings> = context.vpnDataStore.data.map { prefs ->
        val protoName = prefs[Keys.SELECTED_PROTOCOL] ?: VpnProtocol.WIREGUARD.name
        val protocol = runCatching { VpnProtocol.valueOf(protoName) }.getOrDefault(VpnProtocol.WIREGUARD)
        VpnSettings(
            selectedServerId = prefs[Keys.SELECTED_SERVER_ID] ?: "us-nyc",
            selectedProtocol = protocol,
            autoReconnect = prefs[Keys.AUTO_RECONNECT] ?: true,
            dnsLeakProtection = prefs[Keys.DNS_LEAK_PROTECTION] ?: true,
            ipv6LeakBlock = prefs[Keys.IPV6_LEAK_BLOCK] ?: true,
            primaryDns = prefs[Keys.PRIMARY_DNS] ?: "1.1.1.1",
            secondaryDns = prefs[Keys.SECONDARY_DNS] ?: "9.9.9.9",
            mtuOverride = prefs[Keys.MTU_OVERRIDE] ?: protocol.defaultMtu,
            favoriteIds = prefs[Keys.FAVORITE_IDS] ?: setOf("us-nyc", "jp-tyo", "ch-zrh"),
            bypassedPackages = prefs[Keys.BYPASSED_PACKAGES] ?: emptySet()
        )
    }

    suspend fun setSelectedServer(serverId: String) {
        context.vpnDataStore.edit { it[Keys.SELECTED_SERVER_ID] = serverId }
    }

    suspend fun setSelectedProtocol(protocol: VpnProtocol) {
        context.vpnDataStore.edit {
            it[Keys.SELECTED_PROTOCOL] = protocol.name
            it[Keys.MTU_OVERRIDE] = protocol.defaultMtu
        }
    }

    suspend fun setAutoReconnect(enabled: Boolean) {
        context.vpnDataStore.edit { it[Keys.AUTO_RECONNECT] = enabled }
    }

    suspend fun setDnsLeakProtection(enabled: Boolean) {
        context.vpnDataStore.edit { it[Keys.DNS_LEAK_PROTECTION] = enabled }
    }

    suspend fun setIpv6LeakBlock(enabled: Boolean) {
        context.vpnDataStore.edit { it[Keys.IPV6_LEAK_BLOCK] = enabled }
    }

    suspend fun setDnsServers(primary: String, secondary: String) {
        context.vpnDataStore.edit {
            it[Keys.PRIMARY_DNS] = primary
            it[Keys.SECONDARY_DNS] = secondary
        }
    }

    suspend fun setMtuOverride(mtu: Int) {
        context.vpnDataStore.edit { it[Keys.MTU_OVERRIDE] = mtu.coerceIn(1280, 1500) }
    }

    suspend fun toggleFavorite(serverId: String) {
        context.vpnDataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITE_IDS] ?: setOf("us-nyc", "jp-tyo", "ch-zrh")
            prefs[Keys.FAVORITE_IDS] = if (serverId in current) current - serverId else current + serverId
        }
    }

    suspend fun toggleBypassedPackage(packageName: String) {
        context.vpnDataStore.edit { prefs ->
            val current = prefs[Keys.BYPASSED_PACKAGES] ?: emptySet()
            prefs[Keys.BYPASSED_PACKAGES] =
                if (packageName in current) current - packageName else current + packageName
        }
    }
}
