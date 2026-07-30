package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.model.ScriptModel
import com.example.permission.PermissionUtils
import com.example.service.AutoClickerService

enum class AppNavDestination {
    DASHBOARD,
    SCRIPTS,
    SCRIPT_DETAIL,
    SETTINGS,
    GUIDE
}

@Composable
fun VisualAutoClickerApp(viewModel: MainViewModel) {
    val context = LocalContext.current

    var currentDestination by remember { mutableStateOf(AppNavDestination.DASHBOARD) }
    var editingScript by remember { mutableStateOf<ScriptModel?>(null) }

    val hasOverlayPermission by viewModel.hasOverlayPermission.collectAsState()
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled.collectAsState()
    val scripts by viewModel.scripts.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val activeScript by viewModel.activeScript.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkPermissions()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentDestination != AppNavDestination.SCRIPT_DETAIL && currentDestination != AppNavDestination.GUIDE) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    NavigationBarItem(
                        selected = currentDestination == AppNavDestination.DASHBOARD,
                        onClick = { currentDestination = AppNavDestination.DASHBOARD },
                        icon = { Icon(imageVector = Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Home") },
                        modifier = Modifier.testTag("nav_dashboard"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.secondary,
                            unselectedTextColor = MaterialTheme.colorScheme.secondary
                        )
                    )

                    NavigationBarItem(
                        selected = currentDestination == AppNavDestination.SCRIPTS,
                        onClick = { currentDestination = AppNavDestination.SCRIPTS },
                        icon = { Icon(imageVector = Icons.Default.List, contentDescription = "Scripts") },
                        label = { Text("Profiles") },
                        modifier = Modifier.testTag("nav_scripts"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.secondary,
                            unselectedTextColor = MaterialTheme.colorScheme.secondary
                        )
                    )

                    NavigationBarItem(
                        selected = currentDestination == AppNavDestination.SETTINGS,
                        onClick = { currentDestination = AppNavDestination.SETTINGS },
                        icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        modifier = Modifier.testTag("nav_settings"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.secondary,
                            unselectedTextColor = MaterialTheme.colorScheme.secondary
                        )
                    )

                    NavigationBarItem(
                        selected = currentDestination == AppNavDestination.GUIDE,
                        onClick = { currentDestination = AppNavDestination.GUIDE },
                        icon = { Icon(imageVector = Icons.Default.Help, contentDescription = "Guide") },
                        label = { Text("Guide") },
                        modifier = Modifier.testTag("nav_guide"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.secondary,
                            unselectedTextColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { targetDestination ->
                when (targetDestination) {
                    AppNavDestination.DASHBOARD -> DashboardScreen(
                        hasOverlayPermission = hasOverlayPermission,
                        isAccessibilityEnabled = isAccessibilityEnabled,
                        activeScript = activeScript,
                        onRequestOverlayPermission = {
                            PermissionUtils.requestOverlayPermission(context)
                            viewModel.checkPermissions()
                        },
                        onOpenAccessibilitySettings = {
                            PermissionUtils.openAccessibilitySettings(context)
                            viewModel.checkPermissions()
                        },
                        onStartService = { script ->
                            startAutoClickerService(context, script)
                        },
                        onStopService = {
                            stopAutoClickerService(context)
                        },
                        onOpenGuide = { currentDestination = AppNavDestination.GUIDE }
                    )

                    AppNavDestination.SCRIPTS -> ScriptsScreen(
                        scripts = scripts,
                        onCreateNewScript = {
                            editingScript = null
                            currentDestination = AppNavDestination.SCRIPT_DETAIL
                        },
                        onEditScript = { script ->
                            editingScript = script
                            currentDestination = AppNavDestination.SCRIPT_DETAIL
                        },
                        onRunScript = { script ->
                            startAutoClickerService(context, script)
                        },
                        onDeleteScript = { id -> viewModel.deleteScript(id) },
                        onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                        onDuplicateScript = { script -> viewModel.duplicateScript(script) },
                        onRenameScript = { id, newName -> viewModel.renameScript(id, newName) },
                        onImportScriptJson = { jsonStr -> viewModel.importScriptFromJson(jsonStr) }
                    )

                    AppNavDestination.SCRIPT_DETAIL -> ScriptDetailScreen(
                        initialScript = editingScript,
                        onSaveScript = { script ->
                            viewModel.saveScript(script)
                        },
                        onBack = { currentDestination = AppNavDestination.SCRIPTS }
                    )

                    AppNavDestination.SETTINGS -> SettingsScreen(
                        settings = settings,
                        onUpdateSettings = { newSettings -> viewModel.updateSettings(newSettings) }
                    )

                    AppNavDestination.GUIDE -> GuideScreen(
                        onBack = { currentDestination = AppNavDestination.DASHBOARD }
                    )
                }
            }
        }
    }
}

private fun startAutoClickerService(context: Context, script: ScriptModel) {
    val intent = Intent(context, AutoClickerService::class.java).apply {
        action = AutoClickerService.ACTION_START_OVERLAY
    }
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

private fun stopAutoClickerService(context: Context) {
    val intent = Intent(context, AutoClickerService::class.java).apply {
        action = AutoClickerService.ACTION_STOP_OVERLAY
    }
    context.startService(intent)
}
