package com.junkfood.seal.desktop.cookies

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DesktopCookieExtractorTest {
    @Test
    fun `successful extraction materializes a sanitized cached context`() = withExtractorDirectory { directory ->
        val target = directory.resolve("cookies.txt")
        val runner = WritingRunner(targetDirectory = directory, execution = successExecution())
        val extractor = DesktopCookieExtractor(binaryProvider = { directory.resolve("yt-dlp") }, runner = runner)

        val result = extractor.extract(browserSource(target))

        val success = assertIs<DesktopCookieExtractionResult.Success>(result)
        assertEquals(DesktopCookiesStats(1, 1), success.stats)
        assertEquals(target, success.context.path)
        assertTrue(target.exists())
        assertFalse(success.stdoutSummary.contains("secret-value"))
        assertTrue(success.stdoutSummary.contains("[cookie data omitted]"))
        assertTrue(runner.command.containsAll(listOf("--cookies-from-browser", "chrome", "--simulate", "https://example.com")))
    }

    @Test
    fun `nonzero exit preserves sanitized diagnostics and old cache`() = withExtractorDirectory { directory ->
        val target = directory.resolve("cookies.txt")
        target.writeText(validCookie("old.example"))
        val runner = WritingRunner(directory, successExecution().copy(exitCode = 2, stderr = "Cookie: secret\nERROR: denied"))

        val result = DesktopCookieExtractor(binaryProvider = { directory.resolve("yt-dlp") }, runner = runner).extract(browserSource(target))

        val failure = assertIs<DesktopCookieExtractionResult.Failure>(result)
        assertEquals(DesktopCookieExtractionResult.Reason.ProcessFailed, failure.reason)
        assertTrue(failure.stderrSummary.contains("[redacted]"))
        assertFalse(failure.stderrSummary.contains("secret"))
        assertEquals("old.example", DesktopCookiesParser.parseCookieDomain(target.toFile().readLines().single()))
    }

    @Test
    fun `timeout and cancellation are distinguished`() = withExtractorDirectory { directory ->
        val timedOut =
            DesktopCookieExtractor(
                binaryProvider = { directory.resolve("yt-dlp") },
                runner = WritingRunner(directory, successExecution().copy(finished = false, exitCode = null)),
                timeoutMillis = 10,
            ).extract(browserSource(directory.resolve("timeout.txt")))
        assertEquals(DesktopCookieExtractionResult.Reason.TimedOut, assertIs<DesktopCookieExtractionResult.Failure>(timedOut).reason)

        val canceled =
            DesktopCookieExtractor(
                binaryProvider = { directory.resolve("yt-dlp") },
                runner = WritingRunner(directory, successExecution().copy(canceled = true, exitCode = null)),
            ).extract(browserSource(directory.resolve("canceled.txt")))
        assertEquals(DesktopCookieExtractionResult.Reason.Canceled, assertIs<DesktopCookieExtractionResult.Failure>(canceled).reason)
    }

    @Test
    fun `zero exit without a valid output is a readable failure`() = withExtractorDirectory { directory ->
        val runner = WritingRunner(directory, successExecution(), writeOutput = false)

        val result =
            DesktopCookieExtractor(binaryProvider = { directory.resolve("yt-dlp") }, runner = runner)
                .extract(browserSource(directory.resolve("cookies.txt")))

        assertEquals(DesktopCookieExtractionResult.Reason.InvalidOutput, assertIs<DesktopCookieExtractionResult.Failure>(result).reason)
    }

    @Test
    fun `cancel delegates to process runner`() {
        val directory = Files.createTempDirectory("seal-cookie-cancel-test")
        try {
            val runner = WritingRunner(directory, successExecution())
            DesktopCookieExtractor(binaryProvider = { directory.resolve("yt-dlp") }, runner = runner).cancel()
            assertTrue(runner.cancelCalled)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    private class WritingRunner(
        private val targetDirectory: Path,
        private val execution: DesktopCookieProcessExecution,
        private val writeOutput: Boolean = true,
    ) : DesktopCookieProcessRunner {
        var command: List<String> = emptyList()
        var cancelCalled = false

        override fun execute(command: List<String>, timeoutMillis: Long): DesktopCookieProcessExecution {
            this.command = command
            if (writeOutput && execution.finished && execution.exitCode == 0) {
                val output = Path.of(command[command.indexOf("--cookies") + 1])
                output.parent?.let(Files::createDirectories)
                output.writeText(validCookie("example.com"))
            }
            return execution
        }

        override fun cancel() {
            cancelCalled = true
        }
    }

    private fun browserSource(target: Path) =
        DesktopCookieContext.BrowserSource(
            browser = SupportedBrowser.Chrome,
            validationUrl = "example.com",
            targetFile = target,
            userAgent = "UA",
        )

    private fun successExecution() =
        DesktopCookieProcessExecution(
            finished = true,
            canceled = false,
            exitCode = 0,
            stdout = "#HttpOnly_.example.com\tTRUE\t/\tTRUE\t0\tname\tsecret-value\n",
            stderr = "",
        )

    private fun withExtractorDirectory(block: (Path) -> Unit) {
        val directory = Files.createTempDirectory("seal-cookie-extractor-test")
        try {
            block(directory)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
