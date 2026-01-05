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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Settings
import com.junkfood.seal.desktop.ytdlp.DesktopYtDlpPaths
import com.junkfood.seal.desktop.ytdlp.DownloadPlanExecutor
import com.junkfood.seal.desktop.ytdlp.YtDlpMetadataFetcher
import com.junkfood.seal.download.buildDownloadPlan
import com.junkfood.seal.ui.download.queue.DownloadQueueAction
import com.junkfood.seal.ui.download.queue.DownloadQueueFilter
import com.junkfood.seal.ui.download.queue.DownloadQueueItemState
import com.junkfood.seal.ui.download.queue.DownloadQueueScreenShared
import com.junkfood.seal.ui.download.queue.DownloadQueueState
import com.junkfood.seal.ui.download.queue.DownloadQueueStrings
import com.junkfood.seal.ui.download.queue.DownloadQueueViewMode
import com.junkfood.seal.ui.download.queue.DownloadQueueStatus
import com.junkfood.seal.ui.download.queue.DownloadQueueMediaType
import com.junkfood.seal.util.DownloadPreferences
import com.junkfood.seal.util.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                    drawerContent = {
                        Surface(
                            modifier = Modifier.fillMaxHeight(),
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
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRailMenu(current = current, onSelect = { current = it })
                    ContentArea(current, modifier = Modifier.weight(1f), isCompact = false)
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
private fun NavigationRailMenu(current: Destination, onSelect: (Destination) -> Unit) {
    NavigationRail(modifier = Modifier.fillMaxHeight()) {
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
        Destination.Download -> DownloadScreen(contentModifier, onMenuClick = onMenuClick, isCompact = isCompact)
        Destination.Settings -> PlaceholderScreen("设置（待接入更多选项）", contentModifier, onMenuClick = onMenuClick, isCompact = isCompact)
        Destination.Templates -> PlaceholderScreen("命令模板（待接入）", contentModifier, onMenuClick = onMenuClick, isCompact = isCompact)
    }
}

private enum class Destination(val label: String, val icon: ImageVector) {
    Download("下载", Icons.Outlined.Menu),
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

@Composable
private fun DownloadScreen(modifier: Modifier = Modifier, onMenuClick: () -> Unit = {}, isCompact: Boolean = true) {
    val scope = rememberCoroutineScope()
    val executor = remember { DownloadPlanExecutor() }
    val metadataFetcher = remember { YtDlpMetadataFetcher() }

    var filter by remember { mutableStateOf(DownloadQueueFilter.All) }
    var viewMode by remember { mutableStateOf(DownloadQueueViewMode.Grid) }
    val queueItems = remember { mutableStateListOf<DownloadQueueItemState>() }

    var showInputDialog by remember { mutableStateOf(false) }
    var inputUrl by remember { mutableStateOf("") }
    var runningProcess by remember { mutableStateOf<DownloadPlanExecutor.RunningProcess?>(null) }
    var runningItemId by remember { mutableStateOf<String?>(null) }
    val logLines = remember { mutableStateListOf<String>() }

    fun appendLog(line: String) {
        logLines.add(line)
        if (logLines.size > 200) {
            logLines.removeFirst()
        }
    }

    fun updateItem(itemId: String, transform: (DownloadQueueItemState) -> DownloadQueueItemState) {
        val index = queueItems.indexOfFirst { it.id == itemId }
        if (index >= 0) {
            queueItems[index] = transform(queueItems[index])
        }
    }

    fun startDownload(url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return
        val itemId = System.currentTimeMillis().toString()
        queueItems.add(
            DownloadQueueItemState(
                id = itemId,
                title = trimmed,
                url = trimmed,
                mediaType = DownloadQueueMediaType.Video,
                status = DownloadQueueStatus.FetchingInfo,
            ),
        )
        showInputDialog = false

        scope.launch {
            appendLog("start: $trimmed")

            val videoInfo =
                try {
                    withContext(Dispatchers.IO) { metadataFetcher.fetch(trimmed) }
                } catch (e: Exception) {
                    appendLog("metadata failed: ${e.message}")
                    VideoInfo(originalUrl = trimmed, webpageUrl = trimmed, title = trimmed)
                }

            updateItem(itemId) {
                it.copy(
                    title = videoInfo.title.ifBlank { trimmed },
                    author = videoInfo.uploader.orEmpty(),
                    status = DownloadQueueStatus.Ready,
                )
            }

            val preferences = defaultDesktopPreferences()
            val plan = buildDownloadPlan(videoInfo, preferences, playlistUrl = trimmed, playlistItem = 0)

            try {
                runningItemId = itemId
                updateItem(itemId) { it.copy(status = DownloadQueueStatus.Running, progressText = "") }

                val proc =
                    withContext(Dispatchers.IO) {
                        executor.start(
                            plan,
                            executor.defaultConfigFor(plan, url = trimmed, paths = DesktopYtDlpPaths),
                            onStdout = { appendLog(it) },
                            onStderr = { appendLog("[err] $it") },
                        )
                    }

                runningProcess = proc
                val result = withContext(Dispatchers.IO) { proc.waitForResult() }

                updateItem(itemId) {
                    it.copy(
                        status = if (result.exitCode == 0) DownloadQueueStatus.Completed else DownloadQueueStatus.Error,
                        progressText = "退出码 ${result.exitCode}",
                    )
                }
            } catch (e: Exception) {
                appendLog("download failed: ${e.message}")
                updateItem(itemId) { it.copy(status = DownloadQueueStatus.Error, errorMessage = e.message, progressText = e.message.orEmpty()) }
            } finally {
                runningProcess = null
                runningItemId = null
            }
        }
    }

    val queueStrings = DownloadQueueStrings(
        queueTitle = "下载队列",
        addLabel = "添加",
        filterAll = "全部",
        filterDownloading = "下载中",
        filterCanceled = "已取消",
        filterFinished = "已完成",
        emptyTitle = "你会在这里找到下载文件",
        emptyBody = "轻按下载按钮或分享视频链接到本应用来启动下载",
        gridLabel = "网格",
        listLabel = "列表",
    )

    Box(modifier = modifier.fillMaxHeight()) {
        DownloadQueueScreenShared(
            state = DownloadQueueState(items = queueItems, filter = filter, viewMode = viewMode),
            strings = queueStrings,
            onFilterChange = { filter = it },
            onViewModeChange = { viewMode = it },
            onItemAction = { itemId, action ->
                when (action) {
                    DownloadQueueAction.Cancel -> {
                        if (itemId == runningItemId) {
                            runningProcess?.cancel()
                            updateItem(itemId) { it.copy(status = DownloadQueueStatus.Canceled) }
                        }
                    }
                    DownloadQueueAction.Delete -> queueItems.removeAll { it.id == itemId }
                    DownloadQueueAction.Resume -> { /* not supported */ }
                    DownloadQueueAction.OpenFile,
                    DownloadQueueAction.ShareFile,
                    DownloadQueueAction.CopyVideoUrl,
                    DownloadQueueAction.OpenVideoUrl,
                    DownloadQueueAction.OpenThumbnailUrl,
                    DownloadQueueAction.CopyError,
                    DownloadQueueAction.ShowDetails -> {
                        // Not implemented on desktop for now
                    }
                }
            },
            onAddClick = { showInputDialog = true },
            onMenuClick = onMenuClick,
            isCompact = isCompact,
            showAddButton = false,
        )

        FloatingActionButton(
            onClick = { showInputDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
        ) {
            Icon(Icons.Outlined.FileDownload, contentDescription = "开始下载")
        }
    }

    if (showInputDialog) {
        DownloadInputDialog(
            url = inputUrl,
            onUrlChange = { inputUrl = it },
            onDismissRequest = { showInputDialog = false },
            onConfirm = {
                startDownload(inputUrl)
                inputUrl = ""
            },
        )
    }
}

@Composable
private fun DownloadInputDialog(
    url: String,
    onUrlChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("新建下载") },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                label = { Text("视频链接") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = url.isNotBlank()) { Text("开始下载") }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("取消") }
        },
    )
}

private fun defaultDesktopPreferences(): DownloadPreferences =
    DownloadPreferences.EMPTY.copy(
        formatSorting = true,
        videoFormat = 2, // QUALITY
        videoResolution = 3, // 1080p
        embedMetadata = true,
    )
