package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent

object ShareUtils {

    const val DEFAULT_DOWNLOAD_LINK =
        "https://ais-pre-fodoikacwih7lr6meyenph-111472759329.asia-northeast1.run.app"

    fun shareApp(context: Context, downloadUrl: String = DEFAULT_DOWNLOAD_LINK) {
        val cleanUrl = downloadUrl.trim().ifEmpty { DEFAULT_DOWNLOAD_LINK }
        val shareText = buildString {
            appendLine("Download VioletTunnel VPN — Fast WireGuard & OpenVPN privacy tunnel with DNS leak protection.")
            append(cleanUrl)
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "VioletTunnel VPN App Download")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, "Send VioletTunnel Download Link"))
    }

    fun copyDownloadLink(context: Context, downloadUrl: String = DEFAULT_DOWNLOAD_LINK) {
        val cleanUrl = downloadUrl.trim().ifEmpty { DEFAULT_DOWNLOAD_LINK }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("VioletTunnel Download Link", cleanUrl))
    }

    fun shareTunnelConfig(context: Context, profileName: String, rawConfig: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "VioletTunnel Profile: $profileName")
            putExtra(Intent.EXTRA_TEXT, rawConfig)
        }
        context.startActivity(Intent.createChooser(intent, "Share $profileName Config"))
    }
}
