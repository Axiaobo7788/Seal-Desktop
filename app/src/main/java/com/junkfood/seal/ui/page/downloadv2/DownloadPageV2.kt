package com.junkfood.seal.ui.page.downloadv2

import android.content.Intent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.junkfood.seal.R
import com.junkfood.seal.download.DownloaderV2
import com.junkfood.seal.download.Task
import com.junkfood.seal.ui.common.HapticFeedback.slightHapticFeedback
import com.junkfood.seal.ui.common.LocalWindowWidthState
import com.junkfood.seal.ui.download.queue.DownloadQueueAction
import com.junkfood.seal.ui.download.queue.DownloadQueueFilter
import com.junkfood.seal.ui.download.queue.DownloadQueueScreenShared
import com.junkfood.seal.ui.download.queue.DownloadQueueState
import com.junkfood.seal.ui.download.queue.DownloadQueueStrings
import com.junkfood.seal.ui.download.queue.DownloadQueueViewMode
import com.junkfood.seal.ui.page.downloadv2.configure.Config
import com.junkfood.seal.ui.page.downloadv2.configure.DownloadDialog
import com.junkfood.seal.ui.page.downloadv2.configure.DownloadDialogViewModel
import com.junkfood.seal.ui.page.downloadv2.configure.DownloadDialogViewModel.Action
import com.junkfood.seal.ui.page.downloadv2.configure.FormatPage
import com.junkfood.seal.ui.page.downloadv2.configure.PlaylistSelectionPage
import com.junkfood.seal.ui.page.downloadv2.configure.PreferencesMock
import com.junkfood.seal.util.DownloadPreferences
import com.junkfood.seal.util.FileUtil
import com.junkfood.seal.util.createFromPreferences
import com.junkfood.seal.util.getErrorReport
import com.junkfood.seal.util.makeToast
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private const val TAG = "DownloadPageV2"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadPageV2(
    modifier: Modifier = Modifier,
    onMenuOpen: (() -> Unit) = {},
    dialogViewModel: DownloadDialogViewModel,
    downloader: DownloaderV2 = koinInject(),
) {
    val view = LocalView.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    val taskMap = downloader.getTaskStateMap()
    var filter by remember { mutableStateOf(DownloadQueueFilter.All) }
    var viewMode by remember { mutableStateOf(DownloadQueueViewMode.Grid) }

    val queueItems = remember(taskMap.size, filter, viewMode) {
        taskMap.toList().map { (task, state) -> task.toQueueItemState(state) }
    }

    val strings =
        DownloadQueueStrings(
            queueTitle = stringResource(R.string.download_queue),
            addLabel = stringResource(R.string.download),
            filterAll = stringResource(R.string.all),
            filterDownloading = stringResource(R.string.status_downloading),
            filterCanceled = stringResource(R.string.status_canceled),
            filterFinished = stringResource(R.string.status_completed),
            emptyTitle = stringResource(R.string.you_ll_find_your_downloads_here),
            emptyBody = stringResource(R.string.download_hint),
            gridLabel = "Grid",
            listLabel = "List",
        )

    DownloadQueueScreenShared(
        modifier = modifier,
        state = DownloadQueueState(items = queueItems, filter = filter, viewMode = viewMode),
        strings = strings,
        onFilterChange = { filter = it },
        onViewModeChange = { viewMode = it },
        onItemAction = { itemId, action ->
            val taskEntry = taskMap.entries.firstOrNull { it.key.id == itemId }
            val (task, taskState) = taskEntry ?: return@DownloadQueueScreenShared
            view.slightHapticFeedback()
            when (action) {
                DownloadQueueAction.Cancel -> downloader.cancel(task)
                DownloadQueueAction.Delete -> downloader.remove(task)
                DownloadQueueAction.Resume -> downloader.restart(task)
                DownloadQueueAction.OpenFile -> {
                    val path =
                        (taskState.downloadState as? Task.DownloadState.Completed)?.filePath
                    if (path != null) {
                        FileUtil.openFile(path = path) {
                            context.makeToast(R.string.file_unavailable)
                        }
                    } else {
                        context.makeToast(R.string.file_unavailable)
                    }
                }
                DownloadQueueAction.ShareFile -> {
                    val path =
                        (taskState.downloadState as? Task.DownloadState.Completed)?.filePath
                    val shareTitle = context.getString(R.string.share)
                    FileUtil.createIntentForSharingFile(path)?.let {
                        context.startActivity(Intent.createChooser(it, shareTitle))
                    }
                }
                DownloadQueueAction.CopyVideoUrl -> {
                    clipboardManager.setText(AnnotatedString(task.url))
                    context.makeToast(R.string.link_copied)
                }
                DownloadQueueAction.OpenVideoUrl -> uriHandler.openUri(task.url)
                DownloadQueueAction.OpenThumbnailUrl ->
                    taskState.viewState.thumbnailUrl?.let { uriHandler.openUri(it) }
                DownloadQueueAction.CopyError -> {
                    val throwable =
                        (taskState.downloadState as? Task.DownloadState.Error)?.throwable
                    throwable?.let {
                        clipboardManager.setText(AnnotatedString(getErrorReport(it, task.url)))
                        context.makeToast(R.string.error_copied)
                    }
                }
                DownloadQueueAction.ShowDetails -> {}
            }
        },
        onAddClick = {
            view.slightHapticFeedback()
            dialogViewModel.postAction(Action.ShowSheet())
        },
        onMenuClick = onMenuOpen,
        isCompact = LocalWindowWidthState.current == WindowWidthSizeClass.Compact,
    )

    var preferences by remember { mutableStateOf(DownloadPreferences.createFromPreferences()) }
    val sheetValue by dialogViewModel.sheetValueFlow.collectAsStateWithLifecycle()
    val state by dialogViewModel.sheetStateFlow.collectAsStateWithLifecycle()
    val selectionState = dialogViewModel.selectionStateFlow.collectAsStateWithLifecycle().value

    var showDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    LaunchedEffect(sheetValue) {
        if (sheetValue == DownloadDialogViewModel.SheetValue.Expanded) {
            showDialog = true
        } else {
            scope.launch { sheetState.hide() }.invokeOnCompletion { showDialog = false }
        }
    }

    if (showDialog) {
        DownloadDialog(
            state = state,
            sheetState = sheetState,
            config = Config(),
            preferences = preferences,
            onPreferencesUpdate = { preferences = it },
            onActionPost = { dialogViewModel.postAction(it) },
        )
    }

    when (selectionState) {
        is DownloadDialogViewModel.SelectionState.FormatSelection ->
            FormatPage(
                state = selectionState,
                onDismissRequest = { dialogViewModel.postAction(Action.Reset) },
            )
        is DownloadDialogViewModel.SelectionState.PlaylistSelection ->
            PlaylistSelectionPage(
                state = selectionState,
                onDismissRequest = { dialogViewModel.postAction(Action.Reset) },
            )
        DownloadDialogViewModel.SelectionState.Idle -> {}
    }
}
