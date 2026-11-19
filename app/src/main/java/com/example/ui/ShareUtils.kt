package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri

object ShareUtils {

    const val GITHUB_RELEASES_LATEST =
        "https://github.com/67kmobile/Deepa-sahu/releases/latest"

    const val GITHUB_RELEASES_ALL =
        "https://github.com/67kmobile/Deepa-sahu/releases"

    const val DEFAULT_DOWNLOAD_LINK = GITHUB_RELEASES_LATEST

    fun shareApp(context: Context, downloadUrl: String = DEFAULT_DOWNLOAD_LINK) {
        val cleanUrl = downloadUrl.trim().ifEmpty { DEFAULT_DOWNLOAD_LINK }
        val shareText = buildString {
            appendLine("Download VioletTunnel VPN APK (WireGuard & OpenVPN with DNS leak protection):")
            append(cleanUrl)
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "VioletTunnel VPN APK Download")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, "Send VioletTunnel APK Link"))
    }

    fun copyDownloadLink(context: Context, downloadUrl: String = DEFAULT_DOWNLOAD_LINK) {
        val cleanUrl = downloadUrl.trim().ifEmpty { DEFAULT_DOWNLOAD_LINK }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("VioletTunnel APK Download Link", cleanUrl))
    }

    fun openDownloadPage(context: Context, downloadUrl: String = DEFAULT_DOWNLOAD_LINK) {
        val cleanUrl = downloadUrl.trim().ifEmpty { DEFAULT_DOWNLOAD_LINK }
        val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(viewIntent) }
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
