package com.junkfood.seal.desktop.download

import com.junkfood.seal.util.findURLsFromString

internal data class DesktopDownloadInputValidation(
    val normalizedInput: String,
    val urlCount: Int,
) {
    val isBlank: Boolean
        get() = normalizedInput.isBlank()

    val hasMultipleUrls: Boolean
        get() = urlCount > 1

    val canProceed: Boolean
        get() = !isBlank && !hasMultipleUrls
}

internal fun validateDesktopDownloadInput(input: String): DesktopDownloadInputValidation {
    val urls = findURLsFromString(input).distinct()
    val normalized = if (urls.size == 1) urls.single() else input.trim()
    return DesktopDownloadInputValidation(normalizedInput = normalized, urlCount = urls.size)
}

internal enum class DownloadInputDestination {
    Configure,
    Start,
}

internal fun downloadInputDestination(
    configureBeforeDownload: Boolean,
    customCommandEnabled: Boolean,
    customCommandTemplateAvailable: Boolean,
    requiresFormatSelection: Boolean = false,
): DownloadInputDestination =
    if (
        configureBeforeDownload ||
            (customCommandEnabled && !customCommandTemplateAvailable) ||
            (!customCommandEnabled && requiresFormatSelection)
    ) {
        DownloadInputDestination.Configure
    } else {
        DownloadInputDestination.Start
    }
