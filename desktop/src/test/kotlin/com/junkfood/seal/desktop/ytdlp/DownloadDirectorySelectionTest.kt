package com.junkfood.seal.desktop.ytdlp

import com.junkfood.seal.download.DownloadPlan
import com.junkfood.seal.util.DownloadPreferences
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class DownloadDirectorySelectionTest {
    @Test
    fun `audio plans use the configured audio directory`() {
        val audioDir = createTempDirectory("seal-audio-dir")
        val plan = DownloadPlan(options = emptyList(), outputTemplate = "%(title)s", downloadPathHint = "audio")

        val config =
            DownloadPlanExecutor().defaultConfigFor(
                plan = plan,
                url = "https://example.invalid/audio",
                preferences = DownloadPreferences.EMPTY.copy(audioDirectory = audioDir.toString()),
            )

        assertEquals(audioDir.normalizedString(), config.workingDirectory?.normalizedString())
    }

    @Test
    fun `video plans use the configured video directory`() {
        val videoDir = createTempDirectory("seal-video-dir")
        val plan = DownloadPlan(options = emptyList(), outputTemplate = "%(title)s", downloadPathHint = "video")

        val config =
            DownloadPlanExecutor().defaultConfigFor(
                plan = plan,
                url = "https://example.invalid/video",
                preferences = DownloadPreferences.EMPTY.copy(videoDirectory = videoDir.toString()),
            )

        assertEquals(videoDir.normalizedString(), config.workingDirectory?.normalizedString())
    }

    @Test
    fun `custom command uses the configured command directory`() {
        val commandDir = createTempDirectory("seal-command-dir")

        val resolved = DesktopYtDlpPaths.configuredDownloadDirectory(commandDir.toString())

        assertEquals(commandDir.normalizedString(), resolved.normalizedString())
    }
}

private fun java.nio.file.Path.normalizedString(): String =
    toAbsolutePath().normalize().toString()
