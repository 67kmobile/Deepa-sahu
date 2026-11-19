package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.LogLevel
import com.example.data.local.ConnectionLogEntity
import com.example.ui.FormatUtils
import com.example.ui.VpnUiState
import com.example.ui.components.Glass3DCard
import com.example.ui.theme.AmberReconnect
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyanDownload
import com.example.ui.theme.ElectricAmethyst
import com.example.ui.theme.EmeraldSecure
import com.example.ui.theme.LavenderGlow
import com.example.ui.theme.NeonOrchid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LogsScreen(
    uiState: VpnUiState,
    onRunDnsLeakTest: () -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf<LogLevel?>(null) }

    val filteredLogs = remember(uiState.logs, selectedFilter) {
        if (selectedFilter == null) uiState.logs
        else uiState.logs.filter { it.level == selectedFilter?.name }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 700.dp)
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Tunnel Telemetry & Logs",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "${uiState.logs.size} persisted events",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                Row {
                    IconButton(
                        onClick = {
                            val exportText = uiState.logs.joinToString("\n") { log ->
                                "[${FormatUtils.formatLogTime(log.timestampMs)}] [${log.level}] (${log.protocol}) ${log.serverLabel}: ${log.message}"
                            }.ifBlank { "No VPN logs recorded yet." }
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "VioletTunnel Connection Logs")
                                putExtra(Intent.EXTRA_TEXT, exportText)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share VPN Logs"))
                        },
                        modifier = Modifier.testTag("share_logs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share logs",
                            tint = ElectricAmethyst
                        )
                    }

                    IconButton(
                        onClick = onClearLogs,
                        modifier = Modifier.testTag("clear_logs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = "Clear logs",
                            tint = CrimsonAlert
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // DNS Leak Test Card
            Glass3DCard(
                modifier = Modifier.fillMaxWidth(),
                isHighlighted = uiState.lastDnsCheck?.leakDetected == false
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Dns,
                                contentDescription = null,
                                tint = EmeraldSecure,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "DNS Leak Verification",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Pinned Resolver: ${uiState.settings.primaryDns}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Button(
                            onClick = onRunDnsLeakTest,
                            enabled = !uiState.isRunningDnsTest,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonOrchid.copy(alpha = 0.28f),
                                contentColor = LavenderGlow
                            ),
                            border = BorderStroke(1.dp, ElectricAmethyst.copy(alpha = 0.6f)),
                            modifier = Modifier.testTag("run_dns_test_button")
                        ) {
                            if (uiState.isRunningDnsTest) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = LavenderGlow
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(if (uiState.isRunningDnsTest) "Testing" else "Run Check")
                        }
                    }

                    uiState.lastDnsCheck?.let { check ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = check.summary,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (check.leakDetected) AmberReconnect else EmeraldSecure
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Level Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonOrchid.copy(alpha = 0.30f),
                        selectedLabelColor = LavenderGlow
                    )
                )
                LogLevel.entries.forEach { level ->
                    FilterChip(
                        selected = selectedFilter == level,
                        onClick = { selectedFilter = level },
                        label = { Text(level.badge) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonOrchid.copy(alpha = 0.30f),
                            selectedLabelColor = LavenderGlow
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredLogs.isEmpty()) {
                Glass3DCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Terminal,
                            contentDescription = null,
                            tint = ElectricAmethyst,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No connection logs yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Connect the tunnel or run a DNS leak test to stream real-time events.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        LogEntryCard(log)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogEntryCard(log: ConnectionLogEntity) {
    val levelEnum = runCatching { LogLevel.valueOf(log.level) }.getOrDefault(LogLevel.INFO)
    val badgeColor = when (levelEnum) {
        LogLevel.HANDSHAKE -> ElectricAmethyst
        LogLevel.DNS -> CyanDownload
        LogLevel.INFO -> EmeraldSecure
        LogLevel.WARN -> AmberReconnect
        LogLevel.ERROR -> CrimsonAlert
    }

    Glass3DCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = levelEnum.badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${log.protocol} • ${log.serverLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = LavenderGlow
                    )
                }

                Text(
                    text = FormatUtils.formatLogTime(log.timestampMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = log.message,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}
