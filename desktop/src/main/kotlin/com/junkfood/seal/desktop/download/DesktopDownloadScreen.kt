@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)

package com.junkfood.seal.desktop.download

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.junkfood.seal.desktop.ui.page.downloadv2.configure.CustomFormatSelectionSheet
import com.junkfood.seal.desktop.ui.page.downloadv2.configure.DownloadInputSheet
import com.junkfood.seal.desktop.ui.page.downloadv2.configure.DownloadOptionsSheet
import com.junkfood.seal.ui.download.queue.DownloadQueueAction
import com.junkfood.seal.ui.download.queue.DownloadQueueItemState
import com.junkfood.seal.ui.download.queue.DownloadQueueScreenShared
import com.junkfood.seal.ui.download.queue.DownloadQueueState
import com.junkfood.seal.ui.download.queue.DownloadQueueStrings
import com.junkfood.seal.util.DownloadPreferences
import java.awt.Desktop
import java.io.File
import java.net.URI
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.all
import com.junkfood.seal.shared.generated.resources.cancel
import com.junkfood.seal.shared.generated.resources.download
import com.junkfood.seal.shared.generated.resources.download_hint
import com.junkfood.seal.shared.generated.resources.download_queue
import com.junkfood.seal.shared.generated.resources.start_download
import com.junkfood.seal.shared.generated.resources.status_canceled
import com.junkfood.seal.shared.generated.resources.status_completed
import com.junkfood.seal.shared.generated.resources.status_downloading
import com.junkfood.seal.shared.generated.resources.status_error
import com.junkfood.seal.shared.generated.resources.you_ll_find_your_downloads_here
import com.junkfood.seal.shared.generated.resources.desktop_view_grid
import com.junkfood.seal.shared.generated.resources.desktop_view_list

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DesktopDownloadScreen(
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {},
    isCompact: Boolean = true,
    disablePreview: Boolean = false,
    preferences: DownloadPreferences,
    onPreferencesChange: (DownloadPreferences) -> Unit,
    controller: DesktopDownloadController,
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var showSheet by remember { mutableStateOf(false) }
    var sheetPage by remember { mutableStateOf(DownloadSheetPage.Input) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var inputUrl by remember { mutableStateOf("") }
    var pendingUrl by remember { mutableStateOf<String?>(null) }
    var mainPage by remember { mutableStateOf(DownloadMainPage.Queue) }
    var formatUrl by remember { mutableStateOf("") }
    var formatType by remember { mutableStateOf(DesktopDownloadType.Video) }
    var formatPreferences by remember { mutableStateOf(preferences) }
    var downloadType by remember { mutableStateOf(DesktopDownloadType.Video) }
    var workingPreferences by remember { mutableStateOf(preferences) }
    var detailsItem by remember { mutableStateOf<DownloadQueueItemState?>(null) }
    LaunchedEffect(preferences) { workingPreferences = preferences }
    val logLines = controller.logLines


    val queueStrings =
        DownloadQueueStrings(
            queueTitle = stringResource(Res.string.download_queue),
            addLabel = stringResource(Res.string.download),
            filterAll = stringResource(Res.string.all),
            filterDownloading = stringResource(Res.string.status_downloading),
            filterCanceled = stringResource(Res.string.status_canceled),
            filterFinished = stringResource(Res.string.status_completed),
            emptyTitle = stringResource(Res.string.you_ll_find_your_downloads_here),
            emptyBody = stringResource(Res.string.download_hint),
            gridLabel = stringResource(Res.string.desktop_view_grid),
            listLabel = stringResource(Res.string.desktop_view_list),
            statusCanceled = "已暂停",
            resumeLabel = "继续",
            cancelLabel = "暂停",
            shareFileLabel = "打开所在文件夹",
            showDetailsLabel = "详情",
        )

    Column(modifier = modifier.fillMaxHeight()) {
        AnimatedContent(
            targetState = mainPage,
            transitionSpec = {
                val forward = initialState == DownloadMainPage.Queue && targetState == DownloadMainPage.Format
                val slideIn = slideInHorizontally(animationSpec = tween(240)) { fullWidth -> if (forward) fullWidth else -fullWidth }
                val slideOut = slideOutHorizontally(animationSpec = tween(240)) { fullWidth -> if (forward) -fullWidth else fullWidth }
                (slideIn + fadeIn(animationSpec = tween(240))).togetherWith(slideOut + fadeOut(animationSpec = tween(200)))
            },
            label = "downloadMainPage",
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { page ->
            when (page) {
                DownloadMainPage.Queue -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val itemsForUi =
                            remember(controller.queueItems, disablePreview) {
                                if (!disablePreview) controller.queueItems
                                else controller.queueItems.map { it.copy(thumbnailUrl = null) }
                            }

                        DownloadQueueScreenShared(
                            state = DownloadQueueState(items = itemsForUi, filter = controller.filter, viewMode = controller.viewMode),
                            strings = queueStrings,
                            onFilterChange = { controller.filter = it },
                            onViewModeChange = { controller.viewMode = it },
                            onItemAction = { itemId, action ->
                                val item = controller.queueItems.firstOrNull { it.id == itemId }
                                when (action) {
                                    DownloadQueueAction.Cancel -> controller.cancelIfRunning(itemId)
                                    DownloadQueueAction.Delete -> controller.deleteQueueItem(itemId)
                                    DownloadQueueAction.Resume -> controller.resumeIfPossible(itemId)
                                    DownloadQueueAction.OpenFile -> {
                                        item?.filePath?.let { safeOpenFile(it) }
                                    }
                                    DownloadQueueAction.ShareFile -> {
                                        // Desktop: no share sheet for now; best-effort open folder.
                                        item?.filePath?.let { safeRevealInFolder(it) }
                                    }
                                    DownloadQueueAction.CopyVideoUrl -> {
                                        item?.url?.takeIf { it.isNotBlank() }?.let { clipboard.setText(AnnotatedString(it)) }
                                    }
                                    DownloadQueueAction.OpenVideoUrl -> {
                                        item?.url?.takeIf { it.isNotBlank() }?.let { safeBrowse(it) }
                                    }
                                    DownloadQueueAction.OpenThumbnailUrl -> {
                                        if (!disablePreview) {
                                            item?.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { safeBrowse(it) }
                                        }
                                    }
                                    DownloadQueueAction.CopyError -> {
                                        item?.errorMessage?.takeIf { it.isNotBlank() }?.let { clipboard.setText(AnnotatedString(it)) }
                                    }
                                    DownloadQueueAction.ShowDetails -> {
                                        detailsItem = item
                                    }
                                }
                            },
                            onAddClick = {
                                sheetPage = DownloadSheetPage.Input
                                showSheet = true
                            },
                            onMenuClick = onMenuClick,
                            isCompact = isCompact,
                            showAddButton = false,
                            showMenuButton = isCompact,
                        )

                        FloatingActionButton(
                            onClick = {
                                sheetPage = DownloadSheetPage.Input
                                showSheet = true
                            },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                        ) {
                            Icon(Icons.Outlined.FileDownload, contentDescription = stringResource(Res.string.start_download))
                        }
                    }
                }

                DownloadMainPage.Format -> {
                    CustomFormatSelectionSheet(
                        url = formatUrl,
                        controller = controller,
                        downloadType = formatType,
                        basePreferences = formatPreferences,
                        onBack = {
                            mainPage = DownloadMainPage.Queue
                            sheetPage = DownloadSheetPage.Options
                            showSheet = true
                        },
                        onDownloadComplete = {
                            mainPage = DownloadMainPage.Queue
                            showSheet = false
                            pendingUrl = null
                            inputUrl = ""
                            sheetPage = DownloadSheetPage.Input
                        },
                    )
                }
            }
        }

        if (mainPage == DownloadMainPage.Queue) {
            logLines.lastOrNull()?.let { LatestLogRow(it) }
        }
    }

    if (showSheet && mainPage == DownloadMainPage.Queue) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    showSheet = false
                    sheetPage = DownloadSheetPage.Input
                    pendingUrl = null
                }
            },
            sheetState = sheetState,
        ) {
            AnimatedContent(
                targetState = sheetPage,
                transitionSpec = {
                    val forward = initialState == DownloadSheetPage.Input && targetState == DownloadSheetPage.Options
                    val slideIn = slideInHorizontally(animationSpec = tween(220)) { fullWidth -> if (forward) fullWidth / 2 else -fullWidth / 2 }
                    val slideOut = slideOutHorizontally(animationSpec = tween(220)) { fullWidth -> if (forward) -fullWidth / 2 else fullWidth / 2 }
                    (slideIn + fadeIn(animationSpec = tween(220))).togetherWith(slideOut + fadeOut(animationSpec = tween(180)))
                },
                label = "downloadSheet",
            ) { page ->
                when (page) {
                    DownloadSheetPage.Input ->
                        DownloadInputSheet(
                            url = inputUrl,
                            onUrlChange = { inputUrl = it },
                            onPasteIntoUrl = { pasted -> inputUrl = pasted },
                            onDismissRequest = {
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    showSheet = false
                                    pendingUrl = null
                                    sheetPage = DownloadSheetPage.Input
                                }
                            },
                            onProceed = {
                                pendingUrl = inputUrl
                                sheetPage = DownloadSheetPage.Options
                            },
                        )

                    DownloadSheetPage.Options ->
                        DownloadOptionsSheet(
                            urlPreview = pendingUrl.orEmpty(),
                            downloadType = downloadType,
                            onTypeChange = { newType ->
                                downloadType = newType
                                val updated = preferencesForType(workingPreferences, newType)
                                workingPreferences = updated
                                onPreferencesChange(updated)
                            },
                            preferences = workingPreferences,
                            onPreferencesChange = {
                                workingPreferences = it
                                onPreferencesChange(it)
                            },
                            onDismissRequest = {
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    showSheet = false
                                    pendingUrl = null
                                    sheetPage = DownloadSheetPage.Input
                                }
                            },
                            onDownload = {
                                pendingUrl?.let { url ->
                                    controller.startDownload(url, downloadType, workingPreferences)
                                }
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    pendingUrl = null
                                    showSheet = false
                                    sheetPage = DownloadSheetPage.Input
                                    inputUrl = ""
                                }
                            },
                            onCustomFormatClick = {
                                formatUrl = pendingUrl.orEmpty()
                                formatType = downloadType
                                formatPreferences = workingPreferences
                                mainPage = DownloadMainPage.Format
                                scope.launch { sheetState.hide() }.invokeOnCompletion { showSheet = false }
                            },
                        )
                }
            }
        }
    }

    detailsItem?.let { item ->
        val statusText =
            when (item.status) {
                com.junkfood.seal.ui.download.queue.DownloadQueueStatus.Running -> stringResource(Res.string.status_downloading)
                com.junkfood.seal.ui.download.queue.DownloadQueueStatus.Completed -> stringResource(Res.string.status_completed)
                com.junkfood.seal.ui.download.queue.DownloadQueueStatus.Canceled -> stringResource(Res.string.status_canceled)
                com.junkfood.seal.ui.download.queue.DownloadQueueStatus.Error -> stringResource(Res.string.status_error)
                else -> item.status.name
            }
        val logPreview = item.logLines?.takeLast(12)?.joinToString("\n").orEmpty()
        val cliArgs = item.cliArgs?.joinToString(separator = " ").orEmpty()
        val fileSizeText = item.fileSizeApproxBytes?.let { formatSizeApprox(it) }

        AlertDialog(
            onDismissRequest = { detailsItem = null },
            title = { Text(text = item.title.ifBlank { item.url }) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("URL: ${item.url}")
                    Text("Status: $statusText")
                    item.filePath?.takeIf { it.isNotBlank() }?.let { Text("File: $it") }
                    fileSizeText?.let { Text("Size: $it") }
                    item.exitCode?.let { Text("Exit code: $it") }
                    if (cliArgs.isNotBlank()) {
                        Text("Args:")
                        Text(cliArgs, style = MaterialTheme.typography.bodySmall)
                    }
                    item.errorMessage?.takeIf { it.isNotBlank() }?.let { Text("Error: $it") }
                    if (logPreview.isNotBlank()) {
                        Text("Logs:")
                        Text(logPreview, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { detailsItem = null }) { Text(stringResource(Res.string.cancel)) }
            },
        )
    }
}

private enum class DownloadSheetPage {
    Input,
    Options,
}

private enum class DownloadMainPage {
    Queue,
    Format,
}

private fun safeBrowse(url: String) {
    runCatching {
        if (!Desktop.isDesktopSupported()) return
        Desktop.getDesktop().browse(URI(url))
    }
}

private fun safeOpenFile(path: String) {
    runCatching {
        if (!Desktop.isDesktopSupported()) return
        Desktop.getDesktop().open(File(path))
    }
}

private fun safeRevealInFolder(path: String) {
    runCatching {
        val file = File(path)
        val dir = if (file.isDirectory) file else file.parentFile
        if (dir != null) safeOpenFile(dir.absolutePath)
    }
}

private fun formatSizeApprox(bytes: Double): String {
    if (bytes <= 0.0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return "%.1f %s".format(value, units[index])
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
                val fallbackStatusText = stringResource(Res.string.status_downloading)
                val statusText = if (item.progressText.isBlank()) fallbackStatusText else item.progressText
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

