package com.junkfood.seal.desktop

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Settings
import com.junkfood.seal.desktop.settings.DesktopSettingsScreen
import com.junkfood.seal.desktop.download.DesktopDownloadScreen
import kotlinx.coroutines.launch

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Seal Desktop",
        state = rememberWindowState(width = 1100.dp, height = 720.dp),
    ) {
        MaterialTheme {
            Surface { DesktopApp() }
        }
    }
}

@Composable
private fun DesktopApp() {
    var current by remember { mutableStateOf(Destination.Download) }

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
                        ContentArea(current, modifier = Modifier.weight(1f), isCompact = false)
                    }
                }
            }

            NavLayout.PermanentDrawer -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    PermanentNav(current = current, onSelect = { current = it })
                    ContentArea(current, modifier = Modifier.weight(1f), isCompact = false)
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
            text = "Seal",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Destination.entries.forEach { dest ->
            val selected = current == dest
            NavigationDrawerItem(
                label = { Text(dest.label) },
                selected = selected,
                onClick = { onSelect(dest) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                icon = { Icon(dest.icon, contentDescription = dest.label) },
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
                icon = { Icon(Icons.Outlined.Menu, contentDescription = "展开导航") },
                label = { Text("导航") },
            )
        }
        Destination.entries.forEach { dest ->
            NavigationRailItem(
                selected = current == dest,
                onClick = { onSelect(dest) },
                icon = { Icon(dest.icon, contentDescription = dest.label) },
                label = { Text(dest.label) },
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
) {
    val contentModifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
    when (current) {
        Destination.Download -> DesktopDownloadScreen(contentModifier, onMenuClick = onMenuClick, isCompact = isCompact)
        Destination.Settings -> DesktopSettingsScreen(modifier = contentModifier, isCompact = isCompact, onMenuClick = onMenuClick)
        Destination.Templates -> PlaceholderScreen("命令模板（待接入）", contentModifier, onMenuClick = onMenuClick, isCompact = isCompact)
    }
}

private enum class Destination(val label: String, val icon: ImageVector) {
    Download("下载", Icons.Outlined.FileDownload),
    Settings("设置", Icons.Outlined.Settings),
    Templates("模板", Icons.Outlined.Add),
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
                Button(onClick = onMenuClick) { Text("菜单") }
                Text(text, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
        } else {
            Text(text, style = MaterialTheme.typography.headlineSmall)
        }
        Text("与 Android 保持 1:1 的入口占位，后续逐步接入功能。", style = MaterialTheme.typography.bodyMedium)
        Divider()
        Text("当前可切换回“下载”页面继续使用下载功能。", style = MaterialTheme.typography.bodySmall)
    }
}
