package com.junkfood.seal.desktop.cookies

import com.junkfood.seal.desktop.customcommand.buildDesktopCustomCommandArgs
import com.junkfood.seal.desktop.ytdlp.DownloadPlanExecutor
import com.junkfood.seal.desktop.ytdlp.buildDownloadExecutionArgs
import com.junkfood.seal.desktop.ytdlp.buildMetadataCommand
import com.junkfood.seal.download.CustomCommandPlan
import com.junkfood.seal.download.DownloadPlan
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopCookieCommandContractTest {
    private val cookiesFile = Path.of("build", "cookie-contract", "cookies.txt").toAbsolutePath().normalize()
    private val cached = DesktopCookieContext.CachedFile(cookiesFile, userAgent = "UA")

    @Test
    fun `metadata download and custom command use the same cached context`() {
        val expected = listOf("--cookies", cookiesFile.toString(), "--add-header", "User-Agent:UA")
        val metadata =
            buildMetadataCommand(
                ytDlpPath = Path.of("yt-dlp"),
                ffmpegPath = null,
                url = "https://example.com/video",
                cookieContext = cached,
            )
        val download =
            buildDownloadExecutionArgs(
                plan = DownloadPlan(emptyList(), "%(title)s", needsCookiesFile = true),
                config = DownloadPlanExecutor.ExecutionConfig(cookieContext = cached, url = "https://example.com/video"),
            )
        val custom =
            buildDesktopCustomCommandArgs(
                plan = CustomCommandPlan(emptyList(), emptyList(), needsCookiesFile = true, needsArchiveFile = false),
                urls = listOf("https://example.com/video"),
                configFile = Path.of("command.conf"),
                cookieContext = cached,
                archiveFile = Path.of("archive.txt"),
            )

        listOf(metadata, download, custom).forEach { args -> assertTrue(args.containsSlice(expected), args.toString()) }
        assertFalse(custom.contains("--cookies-from-browser"))
    }

    @Test
    fun `disabled context adds neither cookies nor browser extraction`() {
        val context = DesktopCookieContext.Disabled()
        val metadata = buildMetadataCommand(Path.of("yt-dlp"), null, "https://example.com", cookieContext = context)
        val download =
            buildDownloadExecutionArgs(
                DownloadPlan(emptyList(), "out"),
                DownloadPlanExecutor.ExecutionConfig(cookieContext = context, url = "https://example.com"),
            )

        listOf(metadata, download).forEach { args ->
            assertFalse(args.contains("--cookies"))
            assertFalse(args.contains("--cookies-from-browser"))
        }
    }

    @Test
    fun `unmaterialized browser context fails closed in every execution adapter`() {
        val source =
            DesktopCookieContext.BrowserSource(
                browser = SupportedBrowser.Chrome,
                validationUrl = "https://example.com",
                targetFile = cookiesFile,
            )

        assertFailsWith<DesktopCookieContextException> {
            buildMetadataCommand(Path.of("yt-dlp"), null, "https://example.com", cookieContext = source)
        }
        assertFailsWith<DesktopCookieContextException> {
            buildDownloadExecutionArgs(
                DownloadPlan(emptyList(), "out", needsCookiesFile = true),
                DownloadPlanExecutor.ExecutionConfig(cookieContext = source, url = "https://example.com"),
            )
        }
        assertFailsWith<DesktopCookieContextException> {
            buildDesktopCustomCommandArgs(
                CustomCommandPlan(emptyList(), emptyList(), needsCookiesFile = true, needsArchiveFile = false),
                listOf("https://example.com"),
                Path.of("command.conf"),
                source,
                Path.of("archive.txt"),
            )
        }
    }

    private fun List<String>.containsSlice(expected: List<String>): Boolean =
        windowed(expected.size).any { it == expected }
}
