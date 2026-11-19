package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.DnsPresets
import com.example.ui.VpnUiState
import com.example.ui.components.Glass3DCard
import com.example.ui.components.ShareDownloadDialog
import com.example.ui.theme.BorderViolet
import com.example.ui.theme.DeepPlum
import com.example.ui.theme.ElectricAmethyst
import com.example.ui.theme.EmeraldSecure
import com.example.ui.theme.LavenderGlow
import com.example.ui.theme.NeonOrchid
import com.example.ui.theme.SurfaceViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    uiState: VpnUiState,
    onToggleAutoReconnect: (Boolean) -> Unit,
    onToggleDnsLeakProtection: (Boolean) -> Unit,
    onToggleIpv6Block: (Boolean) -> Unit,
    onSelectDns: (String, String) -> Unit,
    onSetMtu: (Int) -> Unit,
    onToggleBypassApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showShareDialog by remember { mutableStateOf(false) }
    var showCustomDnsDialog by remember { mutableStateOf(false) }
    var customPrimary by remember(uiState.settings.primaryDns) {
        mutableStateOf(uiState.settings.primaryDns)
    }
    var customSecondary by remember(uiState.settings.secondaryDns) {
        mutableStateOf(uiState.settings.secondaryDns)
    }
    var showSplitApps by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 700.dp)
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Text(
                    text = "Security & Tunnel Guard",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Configure DNS leak protection, automatic handoff recovery, and MTU",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            item {
                Glass3DCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SettingsSwitchRow(
                            icon = Icons.Rounded.Dns,
                            title = "DNS Leak Protection",
                            subtitle = "Force all DNS lookups through encrypted TUN resolvers",
                            checked = uiState.settings.dnsLeakProtection,
                            onCheckedChange = onToggleDnsLeakProtection,
                            testTag = "settings_dns_leak_switch"
                        )
                        SettingsSwitchRow(
                            icon = Icons.Rounded.Sync,
                            title = "Auto Reconnect",
                            subtitle = "Automatically recover tunnel on Wi-Fi / Cellular network switch",
                            checked = uiState.settings.autoReconnect,
                            onCheckedChange = onToggleAutoReconnect,
                            testTag = "settings_auto_reconnect_switch"
                        )
                        SettingsSwitchRow(
                            icon = Icons.Rounded.Shield,
                            title = "IPv6 Leak Block",
                            subtitle = "Restrict tunnel address family to prevent dual-stack IPv6 leaks",
                            checked = uiState.settings.ipv6LeakBlock,
                            onCheckedChange = onToggleIpv6Block,
                            testTag = "settings_ipv6_block_switch"
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Privacy DNS Resolver",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    OutlinedButton(
                        onClick = { showCustomDnsDialog = true },
                        border = BorderStroke(1.dp, BorderViolet),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LavenderGlow)
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Custom IP")
                    }
                }
            }

            items(DnsPresets.all) { preset ->
                val isSelected = uiState.settings.primaryDns == preset.primaryIp
                Glass3DCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 18.dp,
                    isHighlighted = isSelected,
                    onClick = { onSelectDns(preset.primaryIp, preset.secondaryIp) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "${preset.primaryIp} / ${preset.secondaryIp} • ${preset.description}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Active DNS",
                                tint = EmeraldSecure
                            )
                        }
                    }
                }
            }

            item {
                Glass3DCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = null,
                                    tint = ElectricAmethyst,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Tunnel MTU Size",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceViolet
                            ) {
                                Text(
                                    text = "${uiState.settings.mtuOverride} B",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = LavenderGlow,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Slider(
                            value = uiState.settings.mtuOverride.toFloat(),
                            onValueChange = { onSetMtu((it / 10f).roundToInt() * 10) },
                            valueRange = 1280f..1500f,
                            colors = SliderDefaults.colors(
                                thumbColor = LavenderGlow,
                                activeTrackColor = NeonOrchid,
                                inactiveTrackColor = BorderViolet
                            )
                        )
                        Text(
                            text = "1420 recommended for WireGuard • 1500 for OpenVPN",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }

            item {
                Glass3DCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showSplitApps = !showSplitApps }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Apps,
                                contentDescription = null,
                                tint = ElectricAmethyst,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Split Tunneling Bypass",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${uiState.settings.bypassedPackages.size} apps bypassing VPN",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Text(
                            text = if (showSplitApps) "Hide" else "Configure",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricAmethyst
                        )
                    }
                }
            }

            if (showSplitApps) {
                if (uiState.installedApps.isEmpty()) {
                    item {
                        Text(
                            text = "No user-installed apps detected to exclude.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                } else {
                    items(uiState.installedApps, key = { it.packageName }) { app ->
                        val bypassed = app.packageName in uiState.settings.bypassedPackages
                        Glass3DCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 14.dp,
                            onClick = { onToggleBypassApp(app.packageName) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.appName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                                Checkbox(
                                    checked = bypassed,
                                    onCheckedChange = { onToggleBypassApp(app.packageName) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = NeonOrchid,
                                        uncheckedColor = BorderViolet
                                    )
                                )
                            }
                        }
                    }
                }
            }

            item {
                Glass3DCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("share_app_card"),
                    isHighlighted = true,
                    onClick = { showShareDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = null,
                                tint = LavenderGlow,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Send App Download Link",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Copy or share VioletTunnel download link",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Text(
                            text = "Send",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricAmethyst
                        )
                    }
                }
            }
        }
    }

    if (showShareDialog) {
        ShareDownloadDialog(onDismiss = { showShareDialog = false })
    }

    if (showCustomDnsDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDnsDialog = false },
            containerColor = DeepPlum,
            title = {
                Text("Custom Privacy DNS", color = TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customPrimary,
                        onValueChange = { customPrimary = it },
                        label = { Text("Primary DNS IPv4") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricAmethyst,
                            unfocusedBorderColor = BorderViolet,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = customSecondary,
                        onValueChange = { customSecondary = it },
                        label = { Text("Secondary DNS IPv4") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricAmethyst,
                            unfocusedBorderColor = BorderViolet,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customPrimary.isNotBlank()) {
                            onSelectDns(customPrimary.trim(), customSecondary.trim().ifBlank { customPrimary.trim() })
                            showCustomDnsDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonOrchid)
                ) {
                    Text("Apply DNS")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDnsDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) EmeraldSecure else ElectricAmethyst,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = LavenderGlow,
                checkedTrackColor = NeonOrchid,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = SurfaceViolet
            )
        )
    }
}
