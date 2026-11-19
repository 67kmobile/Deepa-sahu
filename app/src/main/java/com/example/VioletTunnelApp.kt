package com.example

import android.app.Application
import com.example.data.SettingsRepository
import com.example.data.VpnRepository
import com.example.data.local.VpnDatabase

class VioletTunnelApp : Application() {

    val database: VpnDatabase by lazy { VpnDatabase.getInstance(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val vpnRepository: VpnRepository by lazy {
        VpnRepository(
            dao = database.vpnDao(),
            settingsRepository = settingsRepository
        )
    }
}
