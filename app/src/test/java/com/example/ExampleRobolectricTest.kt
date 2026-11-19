package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.TunnelConfigParser
import com.example.data.VpnProtocol
import com.example.ui.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("VioletTunnel", appName)
    }

    @Test
    fun `parse valid WireGuard config`() {
        val sample = TunnelConfigParser.sampleWireGuardTemplate(
            endpointHost = "de-fra.violettunnel.net",
            endpointPort = 51820,
            dns = "9.9.9.9"
        )
        val parsed = TunnelConfigParser.parseWireGuard(sample)
        assertTrue(parsed.isSuccess)
        val cfg = parsed.getOrThrow()
        assertEquals("de-fra.violettunnel.net:51820", cfg.peerEndpoint)
        assertEquals("9.9.9.9", cfg.interfaceDns)
        assertEquals(1420, cfg.mtu)
    }

    @Test
    fun `parse valid OpenVPN TCP config`() {
        val sample = TunnelConfigParser.sampleOpenVpnTemplate(
            remoteHost = "jp-tyo.violettunnel.net",
            remotePort = 443,
            proto = "tcp",
            dns = "1.1.1.1"
        )
        val parsed = TunnelConfigParser.parseOpenVpn(sample)
        assertTrue(parsed.isSuccess)
        val cfg = parsed.getOrThrow()
        assertEquals("jp-tyo.violettunnel.net", cfg.remoteHost)
        assertEquals(443, cfg.remotePort)
        assertEquals(VpnProtocol.OPENVPN_TCP, cfg.protocol)
        assertEquals("1.1.1.1", cfg.dnsServer)
    }

    @Test
    fun `format duration and throughput`() {
        assertEquals("01:02:05", FormatUtils.formatDuration(3725L))
        assertEquals("1.5 KB/s", FormatUtils.formatSpeed(1536L))
    }
}
