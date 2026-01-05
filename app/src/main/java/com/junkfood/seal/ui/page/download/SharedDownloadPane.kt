package com.junkfood.seal.ui.page.download

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.junkfood.seal.Downloader
import com.junkfood.seal.Downloader.ErrorState
import com.junkfood.seal.ui.download.DownloadEvent
import com.junkfood.seal.ui.download.DownloadScreenShared
import com.junkfood.seal.ui.download.DownloadUiState
import com.junkfood.seal.util.DownloadUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * Android 端桥接共享下载 UI：沿用现有 Downloader/HomePageViewModel 逻辑，仅复用 UI。
 */
@Composable
fun DownloadSharedPaneAndroid(
    modifier: Modifier = Modifier,
    homePageViewModel: HomePageViewModel = koinViewModel(),
) {
    val scope = rememberCoroutineScope()
    val viewState by homePageViewModel.viewStateFlow.collectAsStateWithLifecycle()
    val downloaderState by Downloader.downloaderState.collectAsStateWithLifecycle()
    val taskState by Downloader.taskState.collectAsStateWithLifecycle()
    val errorState by Downloader.errorState.collectAsStateWithLifecycle()

    val logs = remember { mutableStateListOf<String>() }

    fun appendLog(line: String) {
        logs.add(line)
        if (logs.size > 200) logs.removeFirst()
    }

    LaunchedEffect(downloaderState) {
        appendLog("状态：$downloaderState")
    }
    LaunchedEffect(taskState.progressText, taskState.progress) {
        if (taskState.progressText.isNotBlank()) appendLog(taskState.progressText)
    }
    LaunchedEffect(errorState) {
        if (errorState !is ErrorState.None) appendLog("错误：${errorState.report}")
    }

    val status =
        when (val state = downloaderState) {
            Downloader.State.Idle ->
                if (errorState is ErrorState.None) "等待链接" else "错误：${errorState.title}"
            Downloader.State.FetchingInfo -> "获取信息..."
            Downloader.State.DownloadingVideo -> "下载中 ${taskState.progress.toInt()}%"
            is Downloader.State.DownloadingPlaylist -> "下载列表 ${state.currentItem}/${state.itemCount}"
            Downloader.State.Updating -> "更新中..."
        }

    val uiState =
        DownloadUiState(
            url = viewState.url,
            status = status,
            isRunning = downloaderState !is Downloader.State.Idle,
            logLines = logs.toList(),
        )

    DownloadScreenShared(
        state = uiState,
        onEvent = { event ->
            when (event) {
                is DownloadEvent.UrlChanged -> homePageViewModel.updateUrl(event.url)
                DownloadEvent.FetchInfo -> {
                    val target = viewState.url.trim()
                    if (target.isEmpty()) return@DownloadScreenShared
                    scope.launch(Dispatchers.IO) {
                        Downloader.updateState(Downloader.State.FetchingInfo)
                        try {
                            DownloadUtil.fetchVideoInfoFromUrl(url = target)
                                .onSuccess { appendLog("获取成功：${it.title}".take(80)) }
                                .onFailure {
                                    appendLog("获取失败：${it.message}")
                                    Downloader.manageDownloadError(
                                        th = it,
                                        url = target,
                                        isFetchingInfo = true,
                                        isTaskAborted = true,
                                    )
                                }
                        } finally {
                            Downloader.updateState(Downloader.State.Idle)
                        }
                    }
                }
                DownloadEvent.StartDownload -> homePageViewModel.startDownloadVideo()
                DownloadEvent.Cancel -> Downloader.cancelDownload()
            }
        },
        modifier = modifier,
    )
}
