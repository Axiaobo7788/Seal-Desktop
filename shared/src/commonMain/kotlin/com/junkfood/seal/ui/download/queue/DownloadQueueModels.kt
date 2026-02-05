package com.junkfood.seal.ui.download.queue

enum class DownloadQueueFilter { All, Downloading, Canceled, Finished }

enum class DownloadQueueViewMode { Grid, List }

enum class DownloadQueueStatus {
    Idle,
    FetchingInfo,
    Ready,
    Running,
    Completed,
    Canceled,
    Error,
}

enum class DownloadQueueMediaType { Video, Audio, Unknown }

data class DownloadQueueItemState(
    val id: String,
    val title: String,
    val author: String = "",
    val url: String = "",
    val mediaType: DownloadQueueMediaType = DownloadQueueMediaType.Unknown,
    val durationSeconds: Int? = null,
    val fileSizeApproxBytes: Double? = null,
    val extractorKey: String = "",
    val videoFormats: List<com.junkfood.seal.util.Format>? = null,
    val audioOnlyFormats: List<com.junkfood.seal.util.Format>? = null,
    val progress: Float? = null, 
    val progressText: String = "",
    val status: DownloadQueueStatus = DownloadQueueStatus.Idle,
    val thumbnailUrl: String? = null,
    val filePath: String? = null,
    val errorMessage: String? = null,
    val exitCode: Int? = null,
    val cliArgs: List<String>? = null,
    val logLines: List<String>? = null,
)

data class DownloadQueueState(
    val items: List<DownloadQueueItemState> = emptyList(),
    val filter: DownloadQueueFilter = DownloadQueueFilter.All,
    val viewMode: DownloadQueueViewMode = DownloadQueueViewMode.Grid,
    val isLoading: Boolean = false,
    val selectedItemId: String? = null,
)

sealed interface DownloadQueueAction {
    data object Cancel : DownloadQueueAction
    data object Resume : DownloadQueueAction
    data object Delete : DownloadQueueAction
    data object OpenFile : DownloadQueueAction
    data object ShareFile : DownloadQueueAction
    data object CopyVideoUrl : DownloadQueueAction
    data object OpenVideoUrl : DownloadQueueAction
    data object OpenThumbnailUrl : DownloadQueueAction
    data object CopyError : DownloadQueueAction
    data object ShowDetails : DownloadQueueAction
}

data class DownloadQueueStrings(
    val queueTitle: String,
    val addLabel: String,
    val filterAll: String,
    val filterDownloading: String,
    val filterCanceled: String,
    val filterFinished: String,
    val emptyTitle: String,
    val emptyBody: String,
    val gridLabel: String,
    val listLabel: String,
    val statusIdle: String = "Idle",
    val statusFetchingInfo: String = "Fetching info",
    val statusReady: String = "Ready",
    val statusRunning: String = "Downloading",
    val statusCompleted: String = "Completed",
    val statusCanceled: String = "Canceled",
    val statusError: String = "Error",
    val videoCountLabel: (Int) -> String = { count -> "Video: $count" },
    val audioCountLabel: (Int) -> String = { count -> "Audio: $count" },
    val openActionsLabel: String = "Actions",
    val resumeLabel: String = "Resume",
    val cancelLabel: String = "Cancel",
    val deleteLabel: String = "Delete",
    val openFileLabel: String = "Open file",
    val shareFileLabel: String = "Share file",
    val copyUrlLabel: String = "Copy link",
    val openUrlLabel: String = "Open link",
    val openThumbLabel: String = "Thumbnail",
    val copyErrorLabel: String = "Copy error",
    val showDetailsLabel: String = "Details",
)
