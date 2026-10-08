package com.junkfood.seal.download

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubtitleLanguageMatcherTest {
    @Test
    fun `exact patterns only match the complete language code`() {
        val matcher = SubtitleLanguageMatcher.compile("en")

        assertTrue(matcher.matches("en"))
        assertFalse(matcher.matches("en-US"))
        assertFalse(matcher.matches("english"))
    }

    @Test
    fun `regex patterns support language variants and original captions`() {
        val matcher = SubtitleLanguageMatcher.compile("en.*,.*-orig")

        assertTrue(matcher.matches("en"))
        assertTrue(matcher.matches("en-US"))
        assertTrue(matcher.matches("zh-Hans-orig"))
        assertFalse(matcher.matches("zh-Hans"))
    }

    @Test
    fun `multiple patterns trim whitespace and ignore empty entries`() {
        val selected =
            listOf("en", "ja", "fr", "de")
                .filterSubtitleLanguages(" en , , ja|fr ")

        assertEquals(linkedSetOf("en", "ja", "fr"), selected)
    }

    @Test
    fun `malformed patterns fail safe without suppressing valid patterns`() {
        val matcher = SubtitleLanguageMatcher.compile("[unterminated,en.*")

        assertTrue(matcher.matches("en-GB"))
        assertFalse(matcher.matches("ja"))
        assertFalse(SubtitleLanguageMatcher.compile("[").matches("en"))
    }

    @Test
    fun `filter preserves source order and removes duplicates`() {
        val selected =
            SubtitleLanguageMatcher
                .compile(".*")
                .filter(listOf("fr", "en", "fr", "ja"))

        assertEquals(linkedSetOf("fr", "en", "ja"), selected)
    }
}
