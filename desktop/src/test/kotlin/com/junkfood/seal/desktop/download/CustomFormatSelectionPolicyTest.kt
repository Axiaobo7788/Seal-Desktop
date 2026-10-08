package com.junkfood.seal.desktop.download

import com.junkfood.seal.util.DownloadPreferences
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CustomFormatSelectionPolicyTest {
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
}
