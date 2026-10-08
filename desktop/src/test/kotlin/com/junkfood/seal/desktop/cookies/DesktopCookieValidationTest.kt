package com.junkfood.seal.desktop.cookies

import com.junkfood.seal.desktop.ytdlp.YtDlpMetadataException
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DesktopCookieValidationTest {
    @Test
    fun `preset host mapping keeps domain validation local and predictable`() {
        assertEquals(listOf("bilibili.com"), CookieValidationPreset.patternsForHost("www.bilibili.com"))
        assertEquals(listOf("youtube.com", "youtu.be"), CookieValidationPreset.patternsForHost("youtu.be"))
        assertEquals(listOf("media.example.com"), CookieValidationPreset.patternsForHost("media.example.com"))
    }

    @Test
    fun `invalid cache and missing target cookies fail before metadata fetch`() = withCookiesFile { file ->
        var fetchCount = 0
        val validator = DesktopCookieMediaValidator { _, _, _, _ -> fetchCount += 1 }

        val missing = validator.validate("https://example.com/video", file.resolveSibling("missing.txt"), null, null)
        val noMatch = validator.validate("https://youtube.com/watch?v=test", file, null, null)

        assertEquals(DesktopCookieMediaValidationResult.Reason.InvalidCache, assertIs<DesktopCookieMediaValidationResult.Failure>(missing).reason)
        assertEquals(DesktopCookieMediaValidationResult.Reason.NoMatchingCookies, assertIs<DesktopCookieMediaValidationResult.Failure>(noMatch).reason)
        assertEquals(0, fetchCount)
    }

    @Test
    fun `successful media validation uses cached context and normalized url`() = withCookiesFile { file ->
        var capturedContext: DesktopCookieContext? = null
        var capturedUrl = ""
        val validator =
            DesktopCookieMediaValidator { url, _, context, _ ->
                capturedUrl = url
                capturedContext = context
            }

        val result = validator.validate("bilibili.com/video/BV1", file, "UA", "http://proxy:8080")

        assertIs<DesktopCookieMediaValidationResult.Success>(result)
        assertEquals("https://bilibili.com/video/BV1", capturedUrl)
        val context = assertIs<DesktopCookieContext.CachedFile>(capturedContext)
        assertEquals(file, context.path)
        assertEquals("UA", context.userAgent)
    }

    @Test
    fun `typed metadata failures keep authentication network media and extractor separate`() = withCookiesFile { file ->
        fun validate(stderr: String) =
            DesktopCookieMediaValidator { _, _, _, _ -> throw YtDlpMetadataException(1, "", stderr) }
                .validate("https://bilibili.com/video/BV1", file, null, null)

        assertReason(DesktopCookieMediaValidationResult.Reason.AuthenticationRequired, validate("ERROR: Sign in to confirm you are not a bot"))
        assertReason(DesktopCookieMediaValidationResult.Reason.NetworkError, validate("ERROR: connection timed out"))
        assertReason(DesktopCookieMediaValidationResult.Reason.MediaUnavailable, validate("ERROR: Video unavailable"))
        assertReason(DesktopCookieMediaValidationResult.Reason.ExtractorFailed, validate("ERROR: unsupported extractor state"))
    }

    private fun assertReason(
        expected: DesktopCookieMediaValidationResult.Reason,
        result: DesktopCookieMediaValidationResult,
    ) {
        val failure = assertIs<DesktopCookieMediaValidationResult.Failure>(result)
        assertEquals(expected, failure.reason)
        assertTrue(failure.diagnostic.isNotBlank())
    }

    private fun withCookiesFile(block: (java.nio.file.Path) -> Unit) {
        val directory = Files.createTempDirectory("seal-cookie-media-validation")
        try {
            val file = directory.resolve("cookies.txt")
            file.writeText(".bilibili.com\tTRUE\t/\tTRUE\t0\tsess\tsecret\n")
            block(file)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
