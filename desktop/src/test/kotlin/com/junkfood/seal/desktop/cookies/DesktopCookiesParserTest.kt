package com.junkfood.seal.desktop.cookies

import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopCookiesParserTest {
    @Test
    fun `counts normal and HttpOnly cookies while ignoring non-data lines`() {
        val directory = Files.createTempDirectory("seal-cookie-parser-test")
        try {
            val file = directory.resolve("cookies.txt")
            file.writeText(
                """
                # Netscape HTTP Cookie File

                .example.com	TRUE	/	FALSE	0	session	one
                #HttpOnly_.example.com	TRUE	/	TRUE	0	auth	two
                example.org	FALSE	/	FALSE	0	name	three
                malformed
                """.trimIndent(),
            )

            assertEquals(DesktopCookiesStats(cookieCount = 3, siteCount = 2), DesktopCookiesParser.parseStats(file))
            assertTrue(DesktopCookiesParser.isValidCookiesFile(file))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `comments blanks and malformed lines are not valid cookies`() {
        val directory = Files.createTempDirectory("seal-cookie-parser-invalid-test")
        try {
            val file = directory.resolve("cookies.txt")
            file.writeText("# comment\n\nmissing-fields\n")

            assertEquals(DesktopCookiesStats(cookieCount = 0, siteCount = 0), DesktopCookiesParser.parseStats(file))
            assertFalse(DesktopCookiesParser.isValidCookiesFile(file))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `empty cookie values preserve the seventh Netscape field`() {
        val directory = Files.createTempDirectory("seal-cookie-parser-empty-value-test")
        try {
            val file = directory.resolve("cookies.txt")
            file.writeText("empty.example\tFALSE\t/\tFALSE\t0\tempty\t\n")

            assertEquals(DesktopCookiesStats(cookieCount = 1, siteCount = 1), DesktopCookiesParser.parseStats(file))
            assertTrue(DesktopCookiesParser.isValidCookiesFile(file))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
