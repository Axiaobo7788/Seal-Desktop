package com.junkfood.seal.desktop.ytdlp

import com.junkfood.seal.desktop.cookies.DesktopCookieContext
import com.junkfood.seal.download.buildDownloadPlan
import com.junkfood.seal.util.DownloadPreferences
import com.junkfood.seal.util.VideoClip
import com.junkfood.seal.util.VideoInfo
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MetadataRuntimeOptionsTest {
    @Test
    fun `metadata inherits extraction runtime but never output or postprocessing options`() {
        val preferences =
            DownloadPreferences.EMPTY.copy(
                forceIpv4 = true,
                formatSorting = true,
                sortingFields = "res:720",
                outputTemplate = "custom",
                useDownloadArchive = true,
                createThumbnail = true,
                splitByChapter = true,
                videoClips = listOf(VideoClip(42, 497)),
                rateLimit = true,
                maxDownloadRate = "100",
                aria2c = true,
            )
        val command =
            buildMetadataCommand(
                Path.of("yt-dlp"),
                null,
                "https://example.org",
                cookieContext = DesktopCookieContext.CachedFile(Path.of("cache.txt"), "Custom UA"),
                runtimeOptions = MetadataRuntimeOptions.from(preferences, "http://127.0.0.1:8080"),
            )

        assertTrue(command.containsAll(listOf("-4", "--proxy", "http://127.0.0.1:8080", "-S", "res:720", "--cookies", "--add-header", "User-Agent:Custom UA")))
        listOf("-o", "--download-archive", "--write-thumbnail", "--split-chapters", "--download-sections", "-r", "--downloader").forEach {
            assertFalse(it in command, it)
        }
        assertEquals(listOf("--", "https://example.org"), command.takeLast(2))
    }

    @Test
    fun `metadata sorting agrees with download while stale concrete formats never scope extraction`() {
        listOf(
            DownloadPreferences.EMPTY,
            DownloadPreferences.EMPTY.copy(extractAudio = true),
            DownloadPreferences.EMPTY.copy(formatSorting = true, sortingFields = "res:720"),
            DownloadPreferences.EMPTY.copy(formatIdString = "137+140", mergeAudioStream = true),
            DownloadPreferences.EMPTY.copy(extractAudio = true, convertAudio = true),
        ).forEach { preferences ->
            val metadata =
                buildMetadataCommand(
                    Path.of("yt-dlp"),
                    null,
                    "https://example.org",
                    runtimeOptions = MetadataRuntimeOptions.from(preferences, null),
                )
            val download = buildDownloadPlan(VideoInfo(), preferences).asCliArgs()
            fun List<String>.value(flag: String): String? = indexOf(flag).takeIf { it >= 0 }?.let { get(it + 1) }
            assertEquals(download.value("-S"), metadata.value("-S"), "-S")
            assertFalse("-f" in metadata)
            assertFalse("--audio-multistreams" in metadata)
        }
    }
}
