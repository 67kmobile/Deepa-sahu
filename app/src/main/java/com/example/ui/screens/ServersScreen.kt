package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.data.ServerRegion
import com.example.data.VpnServer
import com.example.ui.VpnUiState
import com.example.ui.components.Glass3DCard
import com.example.ui.components.PingBadge
import com.example.ui.theme.AmberReconnect
import com.example.ui.theme.BorderViolet
import com.example.ui.theme.ElectricAmethyst
import com.example.ui.theme.EmeraldSecure
import com.example.ui.theme.LavenderGlow
import com.example.ui.theme.NeonOrchid
import com.example.ui.theme.SurfaceViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ServersScreen(
    uiState: VpnUiState,
    onSelectServer: (VpnServer) -> Unit,
    onPingAll: () -> Unit,
    onPingServer: (VpnServer) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf(ServerRegion.ALL) }
    var favoritesOnly by remember { mutableStateOf(false) }

    val filteredServers = remember(uiState.servers, searchQuery, selectedRegion, favoritesOnly) {
        uiState.servers.filter { server ->
            val matchesQuery = searchQuery.isBlank() ||
                server.countryName.contains(searchQuery, ignoreCase = true) ||
                server.cityName.contains(searchQuery, ignoreCase = true) ||
                server.countryCode.contains(searchQuery, ignoreCase = true)
            val matchesRegion = selectedRegion == ServerRegion.ALL || server.region == selectedRegion
            val matchesFav = !favoritesOnly || server.isFavorite
            matchesQuery && matchesRegion && matchesFav
        }
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
                        text = "Global Gateways",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "${uiState.servers.size} encrypted locations available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                Button(
                    onClick = onPingAll,
                    enabled = !uiState.isPingingAll,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonOrchid.copy(alpha = 0.28f),
                        contentColor = LavenderGlow
                    ),
                    border = BorderStroke(1.dp, ElectricAmethyst.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("ping_all_button")
                ) {
                    if (uiState.isPingingAll) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = LavenderGlow
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Ping all servers",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (uiState.isPingingAll) "Pinging" else "Ping All")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search country, city, or ISO code", color = TextMuted) },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = ElectricAmethyst)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceViolet.copy(alpha = 0.8f),
                    unfocusedContainerColor = SurfaceViolet.copy(alpha = 0.55f),
                    focusedBorderColor = ElectricAmethyst,
                    unfocusedBorderColor = BorderViolet,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("server_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = favoritesOnly,
                    onClick = { favoritesOnly = !favoritesOnly },
                    label = { Text("Favorites") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonOrchid.copy(alpha = 0.30f),
                        selectedLabelColor = LavenderGlow,
                        selectedLeadingIconColor = AmberReconnect
                    )
                )

                ServerRegion.entries.forEach { region ->
                    FilterChip(
                        selected = selectedRegion == region,
                        onClick = { selectedRegion = region },
                        label = { Text(region.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonOrchid.copy(alpha = 0.30f),
                            selectedLabelColor = LavenderGlow
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredServers.isEmpty()) {
                Glass3DCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No matching gateways",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try clearing filters or importing a custom WireGuard/OpenVPN profile.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredServers, key = { it.id }) { server ->
                        val isSelected = server.id == uiState.selectedServer.id
                        ServerRowCard(
                            server = server,
                            isSelected = isSelected,
                            onSelect = { onSelectServer(server) },
                            onPing = { onPingServer(server) },
                            onToggleFavorite = { onToggleFavorite(server.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerRowCard(
    server: VpnServer,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPing: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Glass3DCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("server_item_${server.id}"),
        cornerRadius = 20.dp,
        isHighlighted = isSelected,
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = server.flagEmoji,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = server.countryName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Selected server",
                            tint = EmeraldSecure,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = "${server.cityName} • ${server.host}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { (server.loadPercent / 100f).coerceIn(0.05f, 1f) },
                        modifier = Modifier
                            .width(72.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (server.loadPercent < 60) EmeraldSecure else AmberReconnect,
                        trackColor = BorderViolet.copy(alpha = 0.45f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${server.loadPercent}% load",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            PingBadge(
                pingMs = server.pingMs,
                isPinging = server.isPinging,
                modifier = Modifier.clickable(onClick = onPing)
            )

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.testTag("favorite_btn_${server.id}")
            ) {
                Icon(
                    imageVector = if (server.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = if (server.isFavorite) "Remove favorite" else "Add favorite",
                    tint = if (server.isFavorite) AmberReconnect else TextMuted
                )
            }
        }
    }
}
