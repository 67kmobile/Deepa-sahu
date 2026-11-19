package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.VpnConnectionState
import com.example.data.VpnProtocol
import com.example.data.VpnServer
import com.example.ui.FormatUtils
import com.example.ui.VpnUiState
import com.example.ui.components.Glass3DCard
import com.example.ui.components.PingBadge
import com.example.ui.components.ShareDownloadDialog
import com.example.ui.components.ThroughputWaveformCanvas
import com.example.ui.components.Tunnel3DOrbButton
import com.example.ui.theme.AmberReconnect
import com.example.ui.theme.BorderViolet
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyanDownload
import com.example.ui.theme.ElectricAmethyst
import com.example.ui.theme.EmeraldSecure
import com.example.ui.theme.LavenderGlow
import com.example.ui.theme.MagentaUpload
import com.example.ui.theme.NeonOrchid
import com.example.ui.theme.SurfaceViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DashboardScreen(
    uiState: VpnUiState,
    onToggleConnect: () -> Unit,
    onOpenServers: () -> Unit,
    onOpenProtocols: () -> Unit,
    onRefreshPing: (VpnServer) -> Unit,
    onSelectProtocol: (VpnProtocol) -> Unit,
    onToggleDnsProtection: (Boolean) -> Unit,
    onToggleAutoReconnect: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showShareDialog by remember { mutableStateOf(false) }
    val conn = uiState.connectionState
    val server = uiState.selectedServer
    val activeProtocol = server.customProtocol ?: uiState.settings.selectedProtocol

    val statusColor = when (conn) {
        is VpnConnectionState.Connected -> EmeraldSecure
        is VpnConnectionState.Connecting -> ElectricAmethyst
        is VpnConnectionState.Reconnecting -> AmberReconnect
        is VpnConnectionState.Error -> CrimsonAlert
        VpnConnectionState.Disconnected -> TextSecondary
    }

    val statusLabel = when (conn) {
        is VpnConnectionState.Connected -> "Tunnel Encrypted"
        is VpnConnectionState.Connecting -> conn.stepMessage
        is VpnConnectionState.Reconnecting -> "Reconnecting (${conn.attempt}/3)"
        is VpnConnectionState.Error -> conn.message
        VpnConnectionState.Disconnected -> "Ready • Tap to Encrypt"
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Brand & Encryption Status Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NeonOrchid.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, ElectricAmethyst.copy(alpha = 0.45f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = "VioletTunnel shield",
                            tint = LavenderGlow,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "VioletTunnel",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = activeProtocol.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricAmethyst
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showShareDialog = true },
                        modifier = Modifier.testTag("share_app_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Send app download link",
                            tint = ElectricAmethyst,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = statusColor.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (conn is VpnConnectionState.Connected) "PROTECTED" else "STANDBY",
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3D Purple Orbital Connect Core
            Tunnel3DOrbButton(
                connectionState = conn,
                onClick = onToggleConnect
            )

            // Live Connection Timer & Status Subtitle
            Text(
                text = FormatUtils.formatDuration(uiState.elapsedSeconds),
                style = MaterialTheme.typography.displayMedium,
                color = if (conn is VpnConnectionState.Connected) LavenderGlow else TextSecondary,
                modifier = Modifier.testTag("connection_timer")
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = statusColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (conn is VpnConnectionState.Connected) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "TUN ${conn.assignedTunnelIp} • DNS ${conn.dnsServer} • MTU ${conn.mtu}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Selected Country Server Card
            Glass3DCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("active_server_card"),
                isHighlighted = conn is VpnConnectionState.Connected,
                onClick = onOpenServers
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = server.flagEmoji,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = server.countryName,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${server.cityName} • Load ${server.loadPercent}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    PingBadge(pingMs = server.pingMs, isPinging = server.isPinging)
                    IconButton(
                        onClick = { onRefreshPing(server) },
                        modifier = Modifier.testTag("ping_active_server_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Measure latency",
                            tint = ElectricAmethyst,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Choose server",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Live Download & Upload Speed Telemetry + Waveform
            Glass3DCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDownward,
                                    contentDescription = "Download speed",
                                    tint = CyanDownload,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "DOWNLOAD",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = FormatUtils.formatSpeed(uiState.downloadBps),
                                style = MaterialTheme.typography.titleLarge,
                                color = CyanDownload,
                                modifier = Modifier.testTag("download_speed_text")
                            )
                            Text(
                                text = "Total ${FormatUtils.formatBytes(uiState.totalDownloadBytes)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowUpward,
                                    contentDescription = "Upload speed",
                                    tint = MagentaUpload,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "UPLOAD",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = FormatUtils.formatSpeed(uiState.uploadBps),
                                style = MaterialTheme.typography.titleLarge,
                                color = MagentaUpload,
                                modifier = Modifier.testTag("upload_speed_text")
                            )
                            Text(
                                text = "Total ${FormatUtils.formatBytes(uiState.totalUploadBytes)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    ThroughputWaveformCanvas(
                        downSamples = uiState.speedHistoryDown,
                        upSamples = uiState.speedHistoryUp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Protocol Switcher (WireGuard / OpenVPN)
            Glass3DCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenProtocols
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Public,
                                contentDescription = null,
                                tint = ElectricAmethyst,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tunnel Protocol",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "Manage Profiles →",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricAmethyst
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VpnProtocol.entries.forEach { proto ->
                            val selected = activeProtocol == proto
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .testTag("quick_proto_${proto.name.lowercase()}")
                                    .clickable { onSelectProtocol(proto) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) NeonOrchid.copy(alpha = 0.28f) else SurfaceViolet.copy(alpha = 0.6f),
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) ElectricAmethyst else BorderViolet.copy(alpha = 0.5f)
                                )
                            ) {
                                Text(
                                    text = proto.shortName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) LavenderGlow else TextSecondary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Security Toggles: DNS Leak Protection + Auto Reconnect
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickShieldTile(
                    icon = Icons.Rounded.Dns,
                    title = "DNS Shield",
                    subtitle = if (uiState.settings.dnsLeakProtection) uiState.settings.primaryDns else "System DNS",
                    enabled = uiState.settings.dnsLeakProtection,
                    onClick = { onToggleDnsProtection(!uiState.settings.dnsLeakProtection) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_dns_shield_toggle")
                )
                QuickShieldTile(
                    icon = Icons.Rounded.Sync,
                    title = "Auto Reconnect",
                    subtitle = if (uiState.settings.autoReconnect) "Active" else "Manual",
                    enabled = uiState.settings.autoReconnect,
                    onClick = { onToggleAutoReconnect(!uiState.settings.autoReconnect) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_auto_reconnect_toggle")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showShareDialog) {
        ShareDownloadDialog(onDismiss = { showShareDialog = false })
    }
}

@Composable
private fun QuickShieldTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = if (enabled) EmeraldSecure else TextSecondary
    Glass3DCard(
        modifier = modifier,
        cornerRadius = 18.dp,
        isHighlighted = enabled,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.16f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
