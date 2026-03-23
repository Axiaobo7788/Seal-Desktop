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
import androidx.compose.material3.Checkbox
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
import com.junkfood.seal.desktop.ui.AnimatedAlertDialog
import com.junkfood.seal.ui.download.queue.DownloadQueueAction
import com.junkfood.seal.ui.download.queue.DownloadQueueItemState
import com.junkfood.seal.ui.download.queue.DownloadQueueScreenShared
import com.junkfood.seal.ui.download.queue.DownloadQueueState
import com.junkfood.seal.ui.download.queue.DownloadQueueStrings
import com.junkfood.seal.util.DownloadPreferences
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.net.URLDecoder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.all
import com.junkfood.seal.shared.generated.resources.cancel
import com.junkfood.seal.shared.generated.resources.copy_link
import com.junkfood.seal.shared.generated.resources.download
import com.junkfood.seal.shared.generated.resources.download_hint
import com.junkfood.seal.shared.generated.resources.download_queue
import com.junkfood.seal.shared.generated.resources.delete
import com.junkfood.seal.shared.generated.resources.start_download
import com.junkfood.seal.shared.generated.resources.file
import com.junkfood.seal.shared.generated.resources.logs
import com.junkfood.seal.shared.generated.resources.open_file
import com.junkfood.seal.shared.generated.resources.open_file_location
import com.junkfood.seal.shared.generated.resources.open_url
import com.junkfood.seal.shared.generated.resources.print_details
import com.junkfood.seal.shared.generated.resources.restart
import com.junkfood.seal.shared.generated.resources.status_canceled
import com.junkfood.seal.shared.generated.resources.status_paused
import com.junkfood.seal.shared.generated.resources.status_completed
import com.junkfood.seal.shared.generated.resources.status_downloading
import com.junkfood.seal.shared.generated.resources.status_error
import com.junkfood.seal.shared.generated.resources.thumbnail
import com.junkfood.seal.shared.generated.resources.video_file_size
import com.junkfood.seal.shared.generated.resources.video_url
import com.junkfood.seal.shared.generated.resources.you_ll_find_your_downloads_here
import com.junkfood.seal.shared.generated.resources.desktop_view_grid
import com.junkfood.seal.shared.generated.resources.desktop_view_list
import com.junkfood.seal.shared.generated.resources.desktop_download_detail_args
import com.junkfood.seal.shared.generated.resources.desktop_download_detail_error
import com.junkfood.seal.shared.generated.resources.desktop_download_detail_exit_code
import com.junkfood.seal.shared.generated.resources.desktop_download_detail_status
import com.junkfood.seal.shared.generated.resources.desktop_action_failed_prefix
import com.junkfood.seal.shared.generated.resources.desktop_delete_local_file_also
import com.junkfood.seal.shared.generated.resources.desktop_delete_local_file_unavailable
import com.junkfood.seal.shared.generated.resources.desktop_open_local_file_unavailable
import com.junkfood.seal.shared.generated.resources.desktop_delete_queue_item_msg
import com.junkfood.seal.shared.generated.resources.desktop_error_item_not_found
import com.junkfood.seal.shared.generated.resources.desktop_error_link_empty
import com.junkfood.seal.shared.generated.resources.confirm

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
    var detailsDialogItem by remember { mutableStateOf<DownloadQueueItemState?>(null) }
    var deleteTargetItem by remember { mutableStateOf<DownloadQueueItemState?>(null) }
    var deleteDialogItem by remember { mutableStateOf<DownloadQueueItemState?>(null) }
    var deleteLocalFileTogether by remember { mutableStateOf(false) }
    var actionErrorMessage by remember { mutableStateOf<String?>(null) }
    var actionErrorDialogMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(preferences) { workingPreferences = preferences }
    LaunchedEffect(detailsItem) {
        if (detailsItem != null) {
            detailsDialogItem = detailsItem
        } else {
            delay(170)
            detailsDialogItem = null
        }
    }
    LaunchedEffect(deleteTargetItem) {
        if (deleteTargetItem != null) {
            deleteDialogItem = deleteTargetItem
        } else {
            delay(170)
            deleteDialogItem = null
        }
    }
    LaunchedEffect(actionErrorMessage) {
        if (actionErrorMessage != null) {
            actionErrorDialogMessage = actionErrorMessage
        } else {
            delay(170)
            actionErrorDialogMessage = null
        }
    }
    val logLines = controller.logLines
    val actionFailedPrefix = stringResource(Res.string.desktop_action_failed_prefix)
    val itemNotFoundMessage = stringResource(Res.string.desktop_error_item_not_found)
    val linkEmptyMessage = stringResource(Res.string.desktop_error_link_empty)
    val deleteLocalFileUnavailableMessage = stringResource(Res.string.desktop_delete_local_file_unavailable)
    val openLocalFileUnavailableMessage = stringResource(Res.string.desktop_open_local_file_unavailable)


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
            statusCanceled = stringResource(Res.string.status_paused),
            resumeLabel = stringResource(Res.string.restart),
            cancelLabel = stringResource(Res.string.cancel),
            deleteLabel = stringResource(Res.string.delete),
            openFileLabel = stringResource(Res.string.open_file),
            shareFileLabel = stringResource(Res.string.open_file_location),
            copyUrlLabel = stringResource(Res.string.copy_link),
            openUrlLabel = stringResource(Res.string.open_url),
            openThumbLabel = stringResource(Res.string.thumbnail),
            showDetailsLabel = stringResource(Res.string.print_details),
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
                                    DownloadQueueAction.Delete -> {
                                        if (item != null) {
                                            deleteTargetItem = item
                                            deleteLocalFileTogether = false
                                        } else {
                                            actionErrorMessage = itemNotFoundMessage
                                        }
                                    }
                                    DownloadQueueAction.Resume -> controller.resumeIfPossible(itemId)
                                    DownloadQueueAction.OpenFile -> {
                                        val localPathResolution = resolveLocalPathFromItem(item)
                                        val localPath = localPathResolution.selectedPath
                                        val result =
                                            localPath
                                                ?.let { safeOpenFile(it, openLocalFileUnavailableMessage) }
                                                ?: Result.failure(IllegalStateException(openLocalFileUnavailableMessage))
                                        result.exceptionOrNull()?.let { actionErrorMessage = it.message ?: it.toString() }
                                    }
                                    DownloadQueueAction.ShareFile -> {
                                        val localPathResolution = resolveLocalPathFromItem(item)
                                        val localPath = localPathResolution.selectedPath
                                        val result =
                                            localPath
                                                ?.let { safeRevealInFolder(it, openLocalFileUnavailableMessage) }
                                                ?: Result.failure(IllegalStateException(openLocalFileUnavailableMessage))
                                        result.exceptionOrNull()?.let { actionErrorMessage = it.message ?: it.toString() }
                                    }
                                    DownloadQueueAction.CopyVideoUrl -> {
                                        val url = item?.url?.trim().orEmpty()
                                        val result = if (url.isNotBlank()) safeCopyToClipboard(clipboard, url) else Result.failure(IllegalStateException(linkEmptyMessage))
                                        result.exceptionOrNull()?.let { actionErrorMessage = it.message ?: it.toString() }
                                    }
                                    DownloadQueueAction.OpenVideoUrl -> {
                                        val url = item?.url?.trim().orEmpty()
                                        val result = if (url.isNotBlank()) safeBrowse(url) else Result.failure(IllegalStateException(linkEmptyMessage))
                                        result.exceptionOrNull()?.let { actionErrorMessage = it.message ?: it.toString() }
                                    }
                                    DownloadQueueAction.OpenThumbnailUrl -> {
                                        if (!disablePreview) {
                                            val thumb = item?.thumbnailUrl?.trim().orEmpty()
                                            val result = if (thumb.isNotBlank()) safeBrowse(thumb) else Result.failure(IllegalStateException(linkEmptyMessage))
                                            result.exceptionOrNull()?.let { actionErrorMessage = it.message ?: it.toString() }
                                        }
                                    }
                                    DownloadQueueAction.CopyError -> {
                                        val errorText = item?.errorMessage?.trim().orEmpty()
                                        val result = if (errorText.isNotBlank()) safeCopyToClipboard(clipboard, errorText) else Result.failure(IllegalStateException(linkEmptyMessage))
                                        result.exceptionOrNull()?.let { actionErrorMessage = it.message ?: it.toString() }
                                    }
                                    DownloadQueueAction.ShowDetails -> {
                                        detailsItem = item
                                        detailsDialogItem = item
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
                        onPreferencesUpdated = { updated ->
                            formatPreferences = updated
                            workingPreferences = updated
                            onPreferencesChange(updated)
                        },
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

    detailsDialogItem?.let { item ->
        val statusText =
            when (item.status) {
                com.junkfood.seal.ui.download.queue.DownloadQueueStatus.Running -> stringResource(Res.string.status_downloading)
                com.junkfood.seal.ui.download.queue.DownloadQueueStatus.Completed -> stringResource(Res.string.status_completed)
                com.junkfood.seal.ui.download.queue.DownloadQueueStatus.Canceled -> stringResource(Res.string.status_paused)
                com.junkfood.seal.ui.download.queue.DownloadQueueStatus.Error -> stringResource(Res.string.status_error)
                else -> item.status.name
            }
        val logPreview = item.logLines?.takeLast(12)?.joinToString("\n").orEmpty()
        val cliArgs = item.cliArgs?.joinToString(separator = " ").orEmpty()
        val fileSizeText = item.fileSizeApproxBytes?.let { formatSizeApprox(it) }

        AnimatedAlertDialog(
            visible = detailsItem != null,
            onDismissRequest = { detailsItem = null },
            title = { Text(text = item.title.ifBlank { item.url }) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("${stringResource(Res.string.video_url)}: ${item.url}")
                    Text("${stringResource(Res.string.desktop_download_detail_status)}: $statusText")
                    item.filePath?.takeIf { it.isNotBlank() }?.let { Text("${stringResource(Res.string.file)}: $it") }
                    fileSizeText?.let { Text("${stringResource(Res.string.video_file_size)}: $it") }
                    item.exitCode?.let { Text("${stringResource(Res.string.desktop_download_detail_exit_code)}: $it") }
                    if (cliArgs.isNotBlank()) {
                        Text("${stringResource(Res.string.desktop_download_detail_args)}: $cliArgs", style = MaterialTheme.typography.bodySmall)
                    }
                    item.errorMessage?.takeIf { it.isNotBlank() }?.let {
                        Text("${stringResource(Res.string.desktop_download_detail_error)}: $it")
                    }
                    if (logPreview.isNotBlank()) {
                        Text("${stringResource(Res.string.logs)}:")
                        Text(logPreview, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { detailsItem = null }) { Text(stringResource(Res.string.cancel)) }
            },
        )
    }

    deleteDialogItem?.let { item ->
        val localPathResolution = resolveLocalPathFromItem(item)
        val localPath = localPathResolution.selectedPath
        val hasLocalFile = localPath?.let { runCatching { resolveExistingFile(it) }.isSuccess } == true

        AnimatedAlertDialog(
            visible = deleteTargetItem != null,
            onDismissRequest = {
                deleteTargetItem = null
                deleteLocalFileTogether = false
            },
            title = { Text(stringResource(Res.string.delete)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.desktop_delete_queue_item_msg))
                    if (hasLocalFile) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = deleteLocalFileTogether,
                                onCheckedChange = { deleteLocalFileTogether = it },
                            )
                            Text(stringResource(Res.string.desktop_delete_local_file_also))
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deleteTargetItem = null
                        deleteLocalFileTogether = false
                    },
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (deleteLocalFileTogether) {
                            val deleteResult = localPath?.let { deleteLocalFile(it, deleteLocalFileUnavailableMessage) }
                                ?: Result.failure(IllegalStateException(deleteLocalFileUnavailableMessage))
                            deleteResult.exceptionOrNull()?.let { actionErrorMessage = it.message ?: it.toString() }
                        }
                        controller.deleteQueueItem(item.id)
                        deleteTargetItem = null
                        deleteLocalFileTogether = false
                    },
                ) {
                    Text(stringResource(Res.string.delete))
                }
            },
        )
    }

    actionErrorDialogMessage?.let { message ->
        AnimatedAlertDialog(
            visible = actionErrorMessage != null,
            onDismissRequest = { actionErrorMessage = null },
            title = { Text(stringResource(Res.string.status_error)) },
            text = { Text(actionFailedPrefix.format(message)) },
            confirmButton = {
                TextButton(onClick = { actionErrorMessage = null }) {
                    Text(stringResource(Res.string.confirm))
                }
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

private fun safeBrowse(url: String): Result<Unit> =
    runCatching {
        val normalized = normalizeUrl(url)
        val uri = URI(normalized)
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(uri)
        } else {
            openWithSystem(normalized).getOrThrow()
        }
    }

private fun safeCopyToClipboard(clipboard: androidx.compose.ui.platform.ClipboardManager, text: String): Result<Unit> {
    val trimmed = text.trim()
    if (trimmed.isBlank()) return Result.failure(IllegalStateException("Link is empty"))
    return runCatching {
        clipboard.setText(AnnotatedString(trimmed))
    }.recoverCatching {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(trimmed), null)
    }
}

private fun safeOpenFile(path: String, fileUnavailableMessage: String): Result<Unit> =
    runCatching {
        val file = resolveExistingFile(path, fileUnavailableMessage)
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            Desktop.getDesktop().open(file)
        } else {
            openWithSystem(file.absolutePath).getOrThrow()
        }
    }

private fun safeRevealInFolder(path: String, fileUnavailableMessage: String): Result<Unit> =
    runCatching {
        val dir = resolveExistingDirectory(path)
            ?: throw IllegalStateException(fileUnavailableMessage)
        if (!dir.exists()) throw IllegalStateException(fileUnavailableMessage)

        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            Desktop.getDesktop().open(dir)
        } else {
            openWithSystem(dir.absolutePath).getOrThrow()
        }
    }

private fun deleteLocalFile(path: String, fileUnavailableMessage: String): Result<Unit> =
    runCatching {
        val file = resolveExistingFile(path, fileUnavailableMessage)
        if (!file.isFile) throw IllegalStateException("Local target is not a file")
        if (!file.delete()) throw IllegalStateException("Failed to delete local file")
    }

private fun resolveExistingFile(path: String, fileUnavailableMessage: String = "Local file is unavailable"): File {
    val candidates = buildPathCandidates(path)
    return candidates.firstOrNull { it.exists() && it.isFile }
        ?: throw IllegalStateException(fileUnavailableMessage)
}

private fun resolveExistingDirectory(path: String): File? {
    val candidates = buildPathCandidates(path)
    val direct = candidates.firstOrNull { it.exists() && it.isDirectory }
    if (direct != null) return direct

    return candidates
        .mapNotNull { it.parentFile }
        .firstOrNull { it.exists() && it.isDirectory }
}

private fun buildPathCandidates(rawPath: String): List<File> {
    val trimmed = rawPath.trim().trim('"', '\'')
    if (trimmed.isBlank()) return emptyList()

    val expandedHome =
        when {
            trimmed == "~" -> System.getProperty("user.home")
            trimmed.startsWith("~/") -> System.getProperty("user.home") + trimmed.removePrefix("~")
            else -> trimmed
        }

    val candidates = linkedSetOf<File>()
    candidates += File(expandedHome)

    if (expandedHome.startsWith("file:")) {
        runCatching { File(URI(expandedHome)) }.getOrNull()?.let { candidates += it }
    }

    if (expandedHome.contains('%')) {
        runCatching { URLDecoder.decode(expandedHome, Charsets.UTF_8.name()) }
            .getOrNull()
            ?.let { decoded ->
                candidates += File(decoded)
                if (decoded.startsWith("file:")) {
                    runCatching { File(URI(decoded)) }.getOrNull()?.let { candidates += it }
                }
            }
    }

    val relativeStrings = candidates.map { it.path }.filter { File(it).isAbsolute.not() }
    relativeStrings.forEach { relative ->
        candidates += File(System.getProperty("user.dir"), relative)
        candidates += File(System.getProperty("user.home"), relative)
    }

    return candidates.map { it.absoluteFile }
}

private fun normalizeUrl(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) throw IllegalStateException("Link is empty")
    return if (Regex("^[a-zA-Z][a-zA-Z\\d+.-]*:").containsMatchIn(trimmed)) trimmed else "https://$trimmed"
}

private fun openWithSystem(target: String): Result<Unit> =
    runCatching {
        val os = System.getProperty("os.name").lowercase()
        val command =
            when {
                os.contains("win") -> listOf("rundll32", "url.dll,FileProtocolHandler", target)
                os.contains("mac") -> listOf("open", target)
                else -> listOf("xdg-open", target)
            }
        ProcessBuilder(command).start()
    }

private data class LocalPathResolution(
    val selectedPath: String?,
)

private fun resolveLocalPathFromItem(item: DownloadQueueItemState?): LocalPathResolution {
    if (item == null) return LocalPathResolution(selectedPath = null)
    val rawCandidates = buildList<String> {
        item.filePath?.trim()?.takeIf { it.isNotBlank() }?.let(::add)
        extractDestinationPathFromLogs(item.logLines)?.let(::add)
    }.distinct()

    val verified = rawCandidates.firstOrNull { raw ->
        runCatching { resolveExistingFile(raw) }.isSuccess
    }

    return LocalPathResolution(
        selectedPath = verified ?: rawCandidates.firstOrNull(),
    )
}

private fun extractDestinationPathFromLogs(lines: List<String>?): String? {
    if (lines.isNullOrEmpty()) return null
    val patterns = listOf(
        Regex("(?:\\[download\\]\\s*)?Destination:\\s*(.+)$"),
        Regex("\\[Merger\\].*?into\\s+\\\"(.+)\\\"$"),
        Regex("\\[ExtractAudio\\].*?Destination:\\s*(.+)$"),
        Regex("\\[download\\]\\s+(.+?)\\s+has already been downloaded"),
    )

    return lines.asReversed().firstNotNullOfOrNull { rawLine ->
        val line = rawLine.replace(Regex("\\u001B\\[[;\\d]*m"), "").trim()
        patterns.firstNotNullOfOrNull { regex ->
            regex.find(line)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()
                ?.trim('"', '\'')
                ?.takeIf { it.isNotBlank() }
        }
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

