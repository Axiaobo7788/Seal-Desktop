package com.junkfood.seal.ui.page.downloadv2

import com.junkfood.seal.download.Task
import com.junkfood.seal.download.Task.DownloadState
import com.junkfood.seal.ui.download.queue.DownloadQueueItemState
import com.junkfood.seal.ui.download.queue.DownloadQueueMediaType
import com.junkfood.seal.ui.download.queue.DownloadQueueStatus

internal fun Task.toQueueItemState(state: Task.State): DownloadQueueItemState {
    val downloadState = state.downloadState
    val view = state.viewState
    val mediaType = when {
        view.videoFormats?.isNotEmpty() == true -> DownloadQueueMediaType.Video
        view.audioOnlyFormats?.isNotEmpty() == true -> DownloadQueueMediaType.Audio
        else -> DownloadQueueMediaType.Unknown
    }
    var status = DownloadQueueStatus.Idle
    var progress: Float? = null
    var progressText = ""
    var errorMsg: String? = null
    var filePath: String? = null

    when (downloadState) {
        is DownloadState.Running -> {
            status = DownloadQueueStatus.Running
            progress = downloadState.progress
            progressText = downloadState.progressText
        }
        is DownloadState.FetchingInfo -> status = DownloadQueueStatus.FetchingInfo
        DownloadState.ReadyWithInfo -> status = DownloadQueueStatus.Ready
        DownloadState.Idle -> status = DownloadQueueStatus.Idle
        is DownloadState.Completed -> {
            status = DownloadQueueStatus.Completed
            progress = 1f
            filePath = downloadState.filePath
        }
        is DownloadState.Canceled -> {
            status = DownloadQueueStatus.Canceled
            progress = downloadState.progress
        }
        is DownloadState.Error -> {
            status = DownloadQueueStatus.Error
            errorMsg = downloadState.throwable.message
        }
    }

    return DownloadQueueItemState(
        id = id,
        title = view.title.ifBlank { url },
        author = view.uploader,
        url = url,
        mediaType = mediaType,
        durationSeconds = view.duration,
        fileSizeApproxBytes = view.fileSizeApprox,
        progress = progress,
        progressText = progressText,
        status = status,
        thumbnailUrl = view.thumbnailUrl,
        filePath = filePath,
        errorMessage = errorMsg,
    )
}
