package com.junkfood.seal.desktop.download

import com.junkfood.seal.util.DownloadPreferences

enum class DesktopDownloadType { Audio, Video, Playlist }

internal data class CustomFormatSelectionPolicy(
    val audioOnly: Boolean,
    val allowMultiAudio: Boolean,
)

internal fun customFormatSelectionPolicy(
    type: DesktopDownloadType,
    basePreferences: DownloadPreferences,
): CustomFormatSelectionPolicy {
    val audioOnly = type == DesktopDownloadType.Audio
    return CustomFormatSelectionPolicy(
        audioOnly = audioOnly,
        allowMultiAudio = !audioOnly && basePreferences.mergeAudioStream,
    )
}

internal fun preferencesForType(base: DownloadPreferences, type: DesktopDownloadType): DownloadPreferences {
    return base.copy(
        extractAudio = type == DesktopDownloadType.Audio,
        downloadPlaylist = type == DesktopDownloadType.Playlist,
        subdirectoryPlaylistTitle = if (type == DesktopDownloadType.Playlist) true else base.subdirectoryPlaylistTitle,
        embedMetadata = if (type == DesktopDownloadType.Audio) true else base.embedMetadata,
    )
}

internal fun DownloadPreferences.withRetryRuntimePreferences(
    current: DownloadPreferences,
): DownloadPreferences =
    copy(
        cookies = current.cookies,
        cookiesBrowser = current.cookiesBrowser,
        aria2c = current.aria2c,
        concurrentFragments = current.concurrentFragments,
        debug = current.debug,
        proxy = current.proxy,
        proxyUrl = current.proxyUrl,
        userAgentString = current.userAgentString,
        rateLimit = current.rateLimit,
        maxDownloadRate = current.maxDownloadRate,
        useDownloadArchive = current.useDownloadArchive,
        forceIpv4 = current.forceIpv4,
        // Retrying a private task must never make its URL/history persistent.
        privateMode = privateMode || current.privateMode,
    )
