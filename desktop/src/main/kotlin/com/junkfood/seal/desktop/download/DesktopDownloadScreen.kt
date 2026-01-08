@file:OptIn(ExperimentalLayoutApi::class)

package com.junkfood.seal.desktop.download

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DesktopDownloadScreen(
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {},
    isCompact: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    val executor = remember { DownloadPlanExecutor() }
    val metadataFetcher = remember { YtDlpMetadataFetcher() }

    var filter by remember { mutableStateOf(DownloadQueueFilter.All) }
    var viewMode by remember { mutableStateOf(DownloadQueueViewMode.Grid) }
    val queueItems = remember { mutableStateListOf<DownloadQueueItemState>() }

    var showInputSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
        showInputSheet = false

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

    Column(modifier = modifier.fillMaxHeight()) {
        runningItemId?.let { id ->
            queueItems.find { it.id == id }?.let { item ->
                RunningNowBanner(item)
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
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
                onAddClick = { showInputSheet = true },
                onMenuClick = onMenuClick,
                isCompact = isCompact,
                showAddButton = false,
                showMenuButton = isCompact,
            )

            FloatingActionButton(
                onClick = { showInputSheet = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            ) {
                Icon(Icons.Outlined.FileDownload, contentDescription = "开始下载")
            }
        }

        logLines.lastOrNull()?.let { LatestLogRow(it) }
    }

    if (showInputSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInputSheet = false },
            sheetState = sheetState,
        ) {
            DownloadInputSheet(
                url = inputUrl,
                onUrlChange = { inputUrl = it },
                onPasteIntoUrl = { pasted -> inputUrl = pasted },
                onDismissRequest = { showInputSheet = false },
                onConfirm = {
                    startDownload(inputUrl)
                    inputUrl = ""
                },
            )
        }
    }
}

@Composable
private fun DownloadInputSheet(
    url: String,
    onUrlChange: (String) -> Unit,
    onPasteIntoUrl: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("新建下载任务", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            label = { Text("视频链接") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = {
                val clip = clipboard.getText()?.text
                if (!clip.isNullOrBlank()) onPasteIntoUrl(clip)
            }) { Text("粘贴 URL") }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onDismissRequest) { Text("取消") }
                Button(onClick = onConfirm, enabled = url.isNotBlank()) { Text("继续") }
            }
        }
    }
}

@Composable
private fun RunningNowBanner(item: DownloadQueueItemState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.FileDownload, contentDescription = null)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.title.ifBlank { item.url }, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val statusText = item.progressText.ifBlank { "正在下载" }
                Text(statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun LatestLogRow(line: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Text(
            text = line,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun defaultDesktopPreferences(): DownloadPreferences =
    DownloadPreferences.EMPTY.copy(
        formatSorting = true,
        videoFormat = 2, // QUALITY
        videoResolution = 3, // 1080p
        embedMetadata = true,
    )
