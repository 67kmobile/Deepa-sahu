package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.TunnelConfigParser
import com.example.data.VpnProtocol
import com.example.data.VpnServer
import com.example.ui.ShareUtils
import com.example.ui.VpnUiState
import com.example.ui.components.Glass3DCard
import com.example.ui.theme.BorderViolet
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DeepPlum
import com.example.ui.theme.ElectricAmethyst
import com.example.ui.theme.EmeraldSecure
import com.example.ui.theme.LavenderGlow
import com.example.ui.theme.NeonOrchid
import com.example.ui.theme.SurfaceViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ProtocolsScreen(
    uiState: VpnUiState,
    onSelectProtocol: (VpnProtocol) -> Unit,
    onImportCustomProfile: (String, VpnProtocol, String, (Boolean) -> Unit) -> Unit,
    onSelectCustomServer: (VpnServer) -> Unit,
    onDeleteCustomServer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var dialogProtocol by remember { mutableStateOf(VpnProtocol.WIREGUARD) }
    var dialogName by remember { mutableStateOf("") }
    var dialogRawConfig by remember {
        mutableStateOf(TunnelConfigParser.sampleWireGuardTemplate())
    }
    var parseError by remember { mutableStateOf<String?>(null) }

    val customServers = remember(uiState.servers) {
        uiState.servers.filter { it.isCustomProfile }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val content = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (!content.isNullOrBlank()) {
                val detectedProto = if (content.contains("[Interface]", ignoreCase = true) &&
                    content.contains("[Peer]", ignoreCase = true)
                ) {
                    VpnProtocol.WIREGUARD
                } else if (content.contains("proto tcp", ignoreCase = true)) {
                    VpnProtocol.OPENVPN_TCP
                } else {
                    VpnProtocol.OPENVPN_UDP
                }
                val fileName = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')
                    ?: "Imported Profile"
                dialogProtocol = detectedProto
                dialogName = fileName
                dialogRawConfig = content
                parseError = null
                showCreateDialog = true
            }
        }
    }

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
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            item {
                Text(
                    text = "Tunnel Protocols",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Switch between WireGuard and OpenVPN engines or import .conf / .ovpn tunnels",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            items(VpnProtocol.entries) { protocol ->
                val isSelected = uiState.settings.selectedProtocol == protocol
                ProtocolCard(
                    protocol = protocol,
                    isSelected = isSelected,
                    onClick = { onSelectProtocol(protocol) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            filePickerLauncher.launch(arrayOf("*/*"))
                        },
                        border = BorderStroke(1.dp, ElectricAmethyst.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LavenderGlow),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_config_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FileOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import .conf / .ovpn", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    Button(
                        onClick = {
                            dialogProtocol = VpnProtocol.WIREGUARD
                            dialogName = "My WireGuard Node"
                            dialogRawConfig = TunnelConfigParser.sampleWireGuardTemplate()
                            parseError = null
                            showCreateDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonOrchid,
                            contentColor = TextPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_custom_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Profile", maxLines = 1)
                    }
                }
            }

            item {
                Text(
                    text = "Custom Saved Tunnels (${customServers.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            if (customServers.isEmpty()) {
                item {
                    Glass3DCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VpnKey,
                                contentDescription = null,
                                tint = ElectricAmethyst,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No custom WireGuard or OpenVPN configs yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Import a .conf or .ovpn file from device storage or author one directly.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(customServers, key = { it.id }) { custom ->
                    val isSelected = custom.id == uiState.selectedServer.id
                    Glass3DCard(
                        modifier = Modifier.fillMaxWidth(),
                        isHighlighted = isSelected,
                        onClick = { onSelectCustomServer(custom) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = custom.countryName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = NeonOrchid.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = custom.customProtocol?.shortName ?: "CUSTOM",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LavenderGlow,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${custom.host}:${custom.probePort} • MTU ${custom.customMtu ?: 1420}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Active profile",
                                    tint = EmeraldSecure,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (custom.customConfigRaw.isNotBlank()) {
                                        ShareUtils.shareTunnelConfig(
                                            context = context,
                                            profileName = custom.countryName,
                                            rawConfig = custom.customConfigRaw
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("share_profile_${custom.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = "Share custom profile",
                                    tint = ElectricAmethyst
                                )
                            }

                            IconButton(onClick = { onDeleteCustomServer(custom.id) }) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete custom profile",
                                    tint = CrimsonAlert
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = DeepPlum,
            title = {
                Text(
                    text = "Configure Tunnel Profile",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(VpnProtocol.WIREGUARD, VpnProtocol.OPENVPN_UDP).forEach { proto ->
                            FilterChip(
                                selected = dialogProtocol == proto ||
                                    (proto == VpnProtocol.OPENVPN_UDP && dialogProtocol == VpnProtocol.OPENVPN_TCP),
                                onClick = {
                                    dialogProtocol = proto
                                    dialogRawConfig = if (proto == VpnProtocol.WIREGUARD) {
                                        TunnelConfigParser.sampleWireGuardTemplate()
                                    } else {
                                        TunnelConfigParser.sampleOpenVpnTemplate()
                                    }
                                    parseError = null
                                },
                                label = { Text(if (proto == VpnProtocol.WIREGUARD) "WireGuard" else "OpenVPN") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonOrchid.copy(alpha = 0.35f),
                                    selectedLabelColor = LavenderGlow
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = dialogName,
                        onValueChange = { dialogName = it },
                        label = { Text("Profile Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricAmethyst,
                            unfocusedBorderColor = BorderViolet,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (dialogProtocol == VpnProtocol.WIREGUARD) {
                        OutlinedButton(
                            onClick = {
                                dialogRawConfig = TunnelConfigParser.sampleWireGuardTemplate()
                                parseError = null
                            },
                            border = BorderStroke(1.dp, BorderViolet),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate Fresh Curve25519 Keypair")
                        }
                    }

                    OutlinedTextField(
                        value = dialogRawConfig,
                        onValueChange = {
                            dialogRawConfig = it
                            parseError = null
                        },
                        label = {
                            Text(
                                if (dialogProtocol == VpnProtocol.WIREGUARD) "WireGuard .conf"
                                else "OpenVPN .ovpn"
                            )
                        },
                        minLines = 6,
                        maxLines = 10,
                        textStyle = MaterialTheme.typography.labelSmall,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricAmethyst,
                            unfocusedBorderColor = BorderViolet,
                            focusedTextColor = LavenderGlow,
                            unfocusedTextColor = TextSecondary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    parseError?.let { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CrimsonAlert
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportCustomProfile(dialogName, dialogProtocol, dialogRawConfig) { ok ->
                            if (ok) {
                                showCreateDialog = false
                            } else {
                                parseError = "Check config syntax (Endpoint or remote host required)"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonOrchid)
                ) {
                    Text("Save Tunnel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun ProtocolCard(
    protocol: VpnProtocol,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Glass3DCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("protocol_card_${protocol.name.lowercase()}"),
        cornerRadius = 20.dp,
        isHighlighted = isSelected,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = protocol.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceViolet
                    ) {
                        Text(
                            text = "PORT ${protocol.defaultPort}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricAmethyst,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = protocol.cipherLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LavenderGlow
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${protocol.transport} • Default MTU ${protocol.defaultMtu}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Selected protocol",
                    tint = EmeraldSecure,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
