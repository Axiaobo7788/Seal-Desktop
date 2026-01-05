package com.junkfood.seal.ui.page.downloadv2

sealed interface UiAction {
    data object Cancel : UiAction
    data object Resume : UiAction
    data object Delete : UiAction
    data class OpenFile(val path: String?) : UiAction
    data class ShareFile(val path: String?) : UiAction
    data class CopyErrorReport(val throwable: Throwable) : UiAction
    data object CopyVideoURL : UiAction
    data class OpenVideoURL(val url: String) : UiAction
    data class OpenThumbnailURL(val url: String?) : UiAction
}
