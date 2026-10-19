package com.junkfood.seal.desktop.download

import com.junkfood.seal.desktop.settings.DownloadTypePrevious
import com.junkfood.seal.util.DownloadPreferences

enum class DesktopDownloadType { Audio, Video, Playlist }

internal enum class DesktopFormatChoice { Preset, Custom }

internal fun initialDownloadType(
    initialization: Int,
    preferences: DownloadPreferences,
): DesktopDownloadType =
    if (initialization == DownloadTypePrevious) {
        when {
            preferences.downloadPlaylist -> DesktopDownloadType.Playlist
            preferences.extractAudio -> DesktopDownloadType.Audio
            else -> DesktopDownloadType.Video
        }
    } else {
        DesktopDownloadType.Video
    }

internal fun initialFormatChoice(
    preferCustomFormat: Boolean,
    type: DesktopDownloadType,
): DesktopFormatChoice =
    if (preferCustomFormat && type != DesktopDownloadType.Playlist) {
        DesktopFormatChoice.Custom
    } else {
        DesktopFormatChoice.Preset
    }

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

private fun DownloadPreferences.withDownloadType(type: DesktopDownloadType): DownloadPreferences {
    return copy(
        extractAudio = type == DesktopDownloadType.Audio,
        downloadPlaylist = type == DesktopDownloadType.Playlist,
        subdirectoryPlaylistTitle = if (type == DesktopDownloadType.Playlist) true else subdirectoryPlaylistTitle,
        embedMetadata = if (type == DesktopDownloadType.Audio) true else embedMetadata,
    )
}

/** Automatic downloads describe quality intent and must not reuse a media-specific Custom Format ID. */
internal fun preferencesForAutomaticDownload(
    base: DownloadPreferences,
    type: DesktopDownloadType,
): DownloadPreferences =
    base
        .copy(formatIdString = "")
        .withDownloadType(type)

/** Explicit Custom Format IDs are task intent and must survive execution and retry. */
internal fun preferencesForExplicitSelection(
    selection: DownloadPreferences,
    type: DesktopDownloadType,
): DownloadPreferences = selection.withDownloadType(type)

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
