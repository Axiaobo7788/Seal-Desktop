package com.junkfood.seal.desktop.i18n

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AndroidStringsTest {
    @Test
    fun `Chinese scripts map to existing Android region qualifiers`() {
        assertEquals(
            "values-zh-rCN/strings.xml",
            AndroidStrings.candidatePaths(Locale.forLanguageTag("zh-Hans")).first(),
        )
        assertEquals(
            "values-zh-rTW/strings.xml",
            AndroidStrings.candidatePaths(Locale.forLanguageTag("zh-Hant")).first(),
        )
        assertEquals(
            "values-zh-rCN/strings.xml",
            AndroidStrings.candidatePaths(Locale.forLanguageTag("zh-SG")).first(),
        )
        assertEquals(
            "values-zh-rTW/strings.xml",
            AndroidStrings.candidatePaths(Locale.forLanguageTag("zh-HK")).first(),
        )
    }

    @Test
    fun `canonical Hebrew and Indonesian codes include legacy resource qualifiers`() {
        assertTrue("values-iw/strings.xml" in AndroidStrings.candidatePaths(Locale.forLanguageTag("he")))
        assertTrue("values-in/strings.xml" in AndroidStrings.candidatePaths(Locale.forLanguageTag("id")))
    }

    @Test
    fun `missing localized value falls back to default resources`() {
        val value = AndroidStrings.get("desktop_dependency_status_healthy", Locale.forLanguageTag("fr"))

        assertEquals("healthy", value)
    }

    @Test
    fun `formatted Android placeholders use the requested locale`() {
        val value =
            AndroidStrings.format(
                "desktop_dependency_missing_summary",
                "yt-dlp, ffmpeg",
                locale = Locale.ENGLISH,
            )

        assertEquals("Missing: yt-dlp, ffmpeg", value)
    }
}
