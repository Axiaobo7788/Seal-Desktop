@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)

package com.junkfood.seal.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Settings
import com.junkfood.seal.desktop.settings.DesktopSettingsScreen
import com.junkfood.seal.desktop.settings.DesktopSettingsState
import com.junkfood.seal.desktop.settings.DesktopAppSettingsState
import com.junkfood.seal.desktop.settings.rememberDesktopAppSettingsState
import com.junkfood.seal.desktop.settings.rememberDesktopSettingsState
import com.junkfood.seal.desktop.download.DesktopDownloadController
import com.junkfood.seal.desktop.download.DesktopDownloadScreen
import com.junkfood.seal.desktop.download.history.DesktopDownloadHistoryPage
import com.junkfood.seal.desktop.download.history.DesktopHistoryExportType
import com.junkfood.seal.desktop.theme.DesktopSealTheme
import com.junkfood.seal.desktop.theme.DesktopThemeState
import com.junkfood.seal.desktop.theme.rememberDesktopThemeState
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.app_name
import com.junkfood.seal.shared.generated.resources.custom_command
import com.junkfood.seal.shared.generated.resources.desktop_menu
import com.junkfood.seal.shared.generated.resources.desktop_navigation
import com.junkfood.seal.shared.generated.resources.desktop_open_navigation
import com.junkfood.seal.shared.generated.resources.download_queue
import com.junkfood.seal.shared.generated.resources.downloads_history
import com.junkfood.seal.shared.generated.resources.settings
import com.junkfood.seal.shared.generated.resources.sponsor
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = stringResource(Res.string.app_name),
        state = rememberWindowState(width = 1100.dp, height = 720.dp),
    ) {
        val themeState = rememberDesktopThemeState()
        DesktopSealTheme(themeState = themeState) {
            Surface { DesktopApp(themeState = themeState) }
        }
    }
}

@Composable
private fun DesktopApp(themeState: DesktopThemeState) {
    var current by remember { mutableStateOf(Destination.DownloadQueue) }
    val settingsState = rememberDesktopSettingsState()
    val appSettingsState = rememberDesktopAppSettingsState()
    val downloadController = remember { DesktopDownloadController() }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val navType = when {
            maxWidth < 720.dp -> NavLayout.ModalDrawer
            maxWidth < 1200.dp -> NavLayout.NavigationRail
            else -> NavLayout.PermanentDrawer
        }

        when (navType) {
            NavLayout.ModalDrawer -> {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    scrimColor = Color.Black.copy(alpha = 0.2f),
                    drawerContent = {
                        Surface(
                            modifier = Modifier.width(320.dp).fillMaxHeight(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 2.dp,
                        ) {
                            DrawerContent(current = current, onSelect = {
                                current = it
                                scope.launch { drawerState.close() }
                            })
                        }
                    },
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        ContentArea(
                            current,
                            modifier = Modifier.fillMaxWidth(),
                            onMenuClick = { scope.launch { drawerState.open() } },
                            isCompact = true,
                            settingsState = settingsState,
                            appSettingsState = appSettingsState,
                            themeState = themeState,
                            downloadController = downloadController,
                        )
                    }
                }
            }

            NavLayout.NavigationRail -> {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    scrimColor = Color.Black.copy(alpha = 0.2f),
                    drawerContent = {
                        Surface(
                            modifier = Modifier.width(320.dp).fillMaxHeight(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 2.dp,
                        ) {
                            DrawerContent(current = current, onSelect = {
                                current = it
                                scope.launch { drawerState.close() }
                            })
                        }
                    },
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        NavigationRailMenu(
                            current = current,
                            onSelect = { current = it },
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                        )
                        ContentArea(
                            current,
                            modifier = Modifier.weight(1f),
                            isCompact = false,
                            settingsState = settingsState,
                            appSettingsState = appSettingsState,
                            themeState = themeState,
                            downloadController = downloadController,
                        )
                    }
                }
            }

            NavLayout.PermanentDrawer -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    PermanentNav(current = current, onSelect = { current = it })
                    ContentArea(
                        current,
                        modifier = Modifier.weight(1f),
                        isCompact = false,
                        settingsState = settingsState,
                        appSettingsState = appSettingsState,
                        themeState = themeState,
                        downloadController = downloadController,
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerContent(current: Destination, onSelect: (Destination) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(vertical = 16.dp, horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Destination.entries.forEach { dest ->
            val selected = current == dest
            NavigationDrawerItem(
                label = { Text(stringResource(dest.labelRes)) },
                selected = selected,
                onClick = { onSelect(dest) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                icon = { Icon(dest.icon, contentDescription = stringResource(dest.labelRes)) },
            )
        }
    }
}

@Composable
private fun PermanentNav(current: Destination, onSelect: (Destination) -> Unit) {
    Surface(
        modifier = Modifier.width(240.dp).fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
    ) {
        DrawerContent(current = current, onSelect = onSelect)
    }
}

@Composable
private fun NavigationRailMenu(current: Destination, onSelect: (Destination) -> Unit, onOpenDrawer: (() -> Unit)? = null) {
    NavigationRail(modifier = Modifier.fillMaxHeight()) {
        onOpenDrawer?.let {
            NavigationRailItem(
                selected = false,
                onClick = it,
                icon = { Icon(Icons.Outlined.Menu, contentDescription = stringResource(Res.string.desktop_open_navigation)) },
                label = { Text(stringResource(Res.string.desktop_navigation)) },
            )
        }
        Destination.entries.forEach { dest ->
            NavigationRailItem(
                selected = current == dest,
                onClick = { onSelect(dest) },
                icon = { Icon(dest.icon, contentDescription = stringResource(dest.labelRes)) },
                label = { Text(stringResource(dest.labelRes)) },
            )
        }
    }
}

@Composable
private fun ContentArea(
    current: Destination,
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {},
    isCompact: Boolean = false,
    settingsState: DesktopSettingsState,
    appSettingsState: DesktopAppSettingsState,
    themeState: DesktopThemeState,
    downloadController: DesktopDownloadController,
) {
    val contentModifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
    val clipboard = LocalClipboardManager.current
    when (current) {
        Destination.DownloadQueue ->
            DesktopDownloadScreen(
                contentModifier,
                onMenuClick = onMenuClick,
                isCompact = isCompact,
                preferences = settingsState.preferences,
                onPreferencesChange = settingsState::set,
                controller = downloadController,
            )
        Destination.DownloadHistory ->
            DesktopDownloadHistoryPage(
                entries = downloadController.historyEntries,
                onDelete = { downloadController.deleteHistoryEntry(it) },
                onExportToFile = { type: DesktopHistoryExportType, path, onComplete ->
                    downloadController.exportHistoryToFile(path, type, onComplete)
                },
                onExportToClipboard = { type: DesktopHistoryExportType, onComplete ->
                    runCatching {
                        val text = downloadController.exportHistoryText(type)
                        clipboard.setText(AnnotatedString(text))
                    }.also(onComplete)
                },
                onImportFromFile = { path, onComplete ->
                    downloadController.importHistoryFromFile(path, onComplete = onComplete)
                },
                onImportFromClipboard = { text, onComplete ->
                    runCatching { downloadController.importHistoryText(text) }.also(onComplete)
                },
                onMenuClick = onMenuClick,
                isCompact = isCompact,
            )
        Destination.Settings ->
            DesktopSettingsScreen(
                modifier = contentModifier,
                isCompact = isCompact,
                onMenuClick = onMenuClick,
                settingsState = settingsState,
                appSettingsState = appSettingsState,
                themeState = themeState,
            )
        Destination.CustomCommand -> PlaceholderScreen(stringResource(Res.string.custom_command), contentModifier, onMenuClick = onMenuClick, isCompact = isCompact)
        Destination.Sponsor -> PlaceholderScreen(stringResource(Res.string.sponsor), contentModifier, onMenuClick = onMenuClick, isCompact = isCompact)
    }
}

private enum class Destination(val labelRes: StringResource, val icon: ImageVector) {
    DownloadQueue(Res.string.download_queue, Icons.Outlined.FileDownload),
    DownloadHistory(Res.string.downloads_history, Icons.Outlined.History),
    CustomCommand(Res.string.custom_command, Icons.Outlined.Add),
    Settings(Res.string.settings, Icons.Outlined.Settings),
    Sponsor(Res.string.sponsor, Icons.Outlined.Add),
}

private enum class NavLayout { ModalDrawer, NavigationRail, PermanentDrawer }

@Composable
private fun PlaceholderScreen(text: String, modifier: Modifier = Modifier, onMenuClick: () -> Unit = {}, isCompact: Boolean = false) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (isCompact) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Menu,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(4.dp)
                        .clickable(onClick = onMenuClick),
                )
                Text(text, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
        } else {
            Text(text, style = MaterialTheme.typography.headlineSmall)
        }
        Text("This page is not available on Desktop yet.", style = MaterialTheme.typography.bodyMedium)
        Divider()
        Text("Tip: use the Android app for now.", style = MaterialTheme.typography.bodySmall)
    }
}
