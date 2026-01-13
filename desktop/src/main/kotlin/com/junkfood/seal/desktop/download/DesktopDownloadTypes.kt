package com.junkfood.seal.desktop.download

import com.junkfood.seal.util.DownloadPreferences

enum class DesktopDownloadType { Audio, Video, Playlist }

internal fun preferencesForType(base: DownloadPreferences, type: DesktopDownloadType): DownloadPreferences {
    return base.copy(
        extractAudio = type == DesktopDownloadType.Audio,
        downloadPlaylist = type == DesktopDownloadType.Playlist,
        subdirectoryPlaylistTitle = if (type == DesktopDownloadType.Playlist) true else base.subdirectoryPlaylistTitle,
        embedMetadata = if (type == DesktopDownloadType.Audio) true else base.embedMetadata,
    )
}
