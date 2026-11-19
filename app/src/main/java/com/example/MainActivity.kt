package com.example

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.VpnConnectionState
import com.example.ui.MainViewModel
import com.example.ui.components.Purple3DBackdrop
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.ProtocolsScreen
import com.example.ui.screens.ServersScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.DeepPlum
import com.example.ui.theme.ElectricAmethyst
import com.example.ui.theme.LavenderGlow
import com.example.ui.theme.NeonOrchid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletTunnelTheme
import kotlinx.coroutines.flow.collectLatest

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_SERVERS = "servers"
const val ROUTE_PROTOCOLS = "protocols"
const val ROUTE_LOGS = "logs"
const val ROUTE_SETTINGS = "settings"

private data class NavDestinationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val navItems = listOf(
    NavDestinationItem(ROUTE_DASHBOARD, "Tunnel", Icons.Rounded.Security, Icons.Outlined.Security),
    NavDestinationItem(ROUTE_SERVERS, "Servers", Icons.Rounded.Public, Icons.Outlined.Public),
    NavDestinationItem(ROUTE_PROTOCOLS, "Protocols", Icons.Rounded.VpnKey, Icons.Outlined.VpnKey),
    NavDestinationItem(ROUTE_LOGS, "Logs", Icons.Rounded.Terminal, Icons.Outlined.Terminal),
    NavDestinationItem(ROUTE_SETTINGS, "Security", Icons.Rounded.Settings, Icons.Outlined.Settings)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            VioletTunnelTheme {
                VioletTunnelRoot()
            }
        }
    }
}

@Composable
fun VioletTunnelRoot(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_DASHBOARD
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.toastEvents.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Proceed with VPN start regardless of notification choice
        viewModel.connectVpn(context)
    }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.connectVpn(context)
            }
        }
    }

    fun triggerConnectOrDisconnect() {
        val state = uiState.connectionState
        if (state is VpnConnectionState.Connected ||
            state is VpnConnectionState.Connecting ||
            state is VpnConnectionState.Reconnecting
        ) {
            viewModel.disconnectVpn(context)
        } else {
            val prepareIntent = viewModel.prepareVpnIntent(context)
            if (prepareIntent != null) {
                vpnPermissionLauncher.launch(prepareIntent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.connectVpn(context)
            }
        }
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val isConnected = uiState.connectionState is VpnConnectionState.Connected

    Purple3DBackdrop(isConnected = isConnected) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            containerColor = DeepPlum.copy(alpha = 0.94f),
                            contentColor = LavenderGlow,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            navItems.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    },
                                    label = { Text(item.label) },
                                    modifier = Modifier.testTag("nav_${item.route}"),
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = LavenderGlow,
                                        selectedTextColor = LavenderGlow,
                                        indicatorColor = NeonOrchid.copy(alpha = 0.28f),
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = DeepPlum.copy(alpha = 0.90f),
                            contentColor = LavenderGlow,
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            navItems.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTo(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    },
                                    label = { Text(item.label) },
                                    modifier = Modifier.testTag("rail_${item.route}"),
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = LavenderGlow,
                                        selectedTextColor = LavenderGlow,
                                        indicatorColor = NeonOrchid.copy(alpha = 0.28f),
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_DASHBOARD
                        ) {
                            composable(ROUTE_DASHBOARD) {
                                DashboardScreen(
                                    uiState = uiState,
                                    onToggleConnect = { triggerConnectOrDisconnect() },
                                    onOpenServers = { navigateTo(ROUTE_SERVERS) },
                                    onOpenProtocols = { navigateTo(ROUTE_PROTOCOLS) },
                                    onRefreshPing = { viewModel.pingServer(it) },
                                    onSelectProtocol = { viewModel.selectProtocol(context, it) },
                                    onToggleDnsProtection = { viewModel.setDnsLeakProtection(it) },
                                    onToggleAutoReconnect = { viewModel.setAutoReconnect(it) }
                                )
                            }
                            composable(ROUTE_SERVERS) {
                                ServersScreen(
                                    uiState = uiState,
                                    onSelectServer = { server ->
                                        viewModel.selectServer(context, server, connectImmediately = false)
                                        navigateTo(ROUTE_DASHBOARD)
                                    },
                                    onPingAll = { viewModel.pingAllServers() },
                                    onPingServer = { viewModel.pingServer(it) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) }
                                )
                            }
                            composable(ROUTE_PROTOCOLS) {
                                ProtocolsScreen(
                                    uiState = uiState,
                                    onSelectProtocol = { viewModel.selectProtocol(context, it) },
                                    onImportCustomProfile = { name, proto, raw, cb ->
                                        viewModel.importCustomProfile(name, proto, raw, cb)
                                    },
                                    onSelectCustomServer = { server ->
                                        viewModel.selectServer(context, server, connectImmediately = false)
                                        navigateTo(ROUTE_DASHBOARD)
                                    },
                                    onDeleteCustomServer = { viewModel.deleteCustomProfile(it) }
                                )
                            }
                            composable(ROUTE_LOGS) {
                                LogsScreen(
                                    uiState = uiState,
                                    onRunDnsLeakTest = { viewModel.runDnsLeakTest() },
                                    onClearLogs = { viewModel.clearLogs() }
                                )
                            }
                            composable(ROUTE_SETTINGS) {
                                SettingsScreen(
                                    uiState = uiState,
                                    onToggleAutoReconnect = { viewModel.setAutoReconnect(it) },
                                    onToggleDnsLeakProtection = { viewModel.setDnsLeakProtection(it) },
                                    onToggleIpv6Block = { viewModel.setIpv6LeakBlock(it) },
                                    onSelectDns = { p, s -> viewModel.setDnsServers(p, s) },
                                    onSetMtu = { viewModel.setMtuOverride(it) },
                                    onToggleBypassApp = { viewModel.toggleBypassedPackage(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
