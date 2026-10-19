package com.junkfood.seal.desktop.download

import com.junkfood.seal.desktop.settings.DownloadTypeNone
import com.junkfood.seal.desktop.settings.DownloadTypePrevious
import com.junkfood.seal.util.DownloadPreferences
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CustomFormatSelectionPolicyTest {
    @Test
    fun `rapid task IDs are unique UUIDs`() {
        val ids = List(1_000) { newDesktopDownloadTaskId() }

        assertEquals(ids.size, ids.toSet().size)
        ids.forEach(UUID::fromString)
    }

    @Test
    fun `download type initialization distinguishes default video from previous selection`() {
        assertEquals(DesktopDownloadType.Video, initialDownloadType(DownloadTypeNone, DownloadPreferences.EMPTY))
        assertEquals(
            DesktopDownloadType.Audio,
            initialDownloadType(DownloadTypePrevious, DownloadPreferences.EMPTY.copy(extractAudio = true)),
        )
        assertEquals(
            DesktopDownloadType.Playlist,
            initialDownloadType(
                DownloadTypePrevious,
                DownloadPreferences.EMPTY.copy(extractAudio = true, downloadPlaylist = true),
            ),
        )
    }

    @Test
    fun `format selection preference defaults to custom except for whole playlists`() {
        assertEquals(DesktopFormatChoice.Preset, initialFormatChoice(false, DesktopDownloadType.Video))
        assertEquals(DesktopFormatChoice.Custom, initialFormatChoice(true, DesktopDownloadType.Video))
        assertEquals(DesktopFormatChoice.Custom, initialFormatChoice(true, DesktopDownloadType.Audio))
        assertEquals(DesktopFormatChoice.Preset, initialFormatChoice(true, DesktopDownloadType.Playlist))
    }

    @Test
    fun `audio custom format only exposes audio choices`() {
        val policy =
            customFormatSelectionPolicy(
                type = DesktopDownloadType.Audio,
                basePreferences = DownloadPreferences.EMPTY.copy(mergeAudioStream = true),
            )

        assertTrue(policy.audioOnly)
        assertFalse(policy.allowMultiAudio)
    }

    @Test
    fun `video and playlist custom format preserve configured multi audio behavior`() {
        val preferences = DownloadPreferences.EMPTY.copy(mergeAudioStream = true)

        DesktopDownloadType.entries
            .filterNot { it == DesktopDownloadType.Audio }
            .forEach { type ->
                val policy = customFormatSelectionPolicy(type, preferences)
                assertFalse(policy.audioOnly)
                assertTrue(policy.allowMultiAudio)
            }
    }

    @Test
    fun `video custom format does not enable multi audio when preference is disabled`() {
        val policy =
            customFormatSelectionPolicy(
                type = DesktopDownloadType.Video,
                basePreferences = DownloadPreferences.EMPTY.copy(mergeAudioStream = false),
            )

        assertFalse(policy.audioOnly)
        assertFalse(policy.allowMultiAudio)
    }

    @Test
    fun `automatic download never reuses a persisted media-specific format id`() {
        val stale =
            DownloadPreferences.EMPTY.copy(
                extractAudio = true,
                downloadPlaylist = true,
                formatIdString = "30077+30280",
            )
        val preferences = preferencesForAutomaticDownload(stale, DesktopDownloadType.Playlist)

        assertEquals("", preferences.formatIdString)
        assertTrue(preferences.downloadPlaylist)
        assertFalse(preferences.extractAudio)

        val video = preferencesForAutomaticDownload(stale, DesktopDownloadType.Video)
        assertFalse(video.extractAudio)
        assertFalse(video.downloadPlaylist)

        val audio = preferencesForAutomaticDownload(stale, DesktopDownloadType.Audio)
        assertTrue(audio.extractAudio)
        assertFalse(audio.downloadPlaylist)
    }

    @Test
    fun `explicit custom format remains task intent`() {
        val preferences =
            preferencesForExplicitSelection(
                DownloadPreferences.EMPTY.copy(formatIdString = "30077+30280"),
                DesktopDownloadType.Playlist,
            )

        assertEquals("30077+30280", preferences.formatIdString)
        assertTrue(preferences.downloadPlaylist)
    }
}
