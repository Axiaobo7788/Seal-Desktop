package com.junkfood.seal.desktop.download.clip

import kotlin.test.*

class ClipTimestampParserTest {
    @Test fun `seconds and precise timestamps`() {
        mapOf("42" to 42, "0:42" to 42, "00:42" to 42, "01:02" to 62, "8:17" to 497, "1:02:03" to 3723, "00:00" to 0, " 42 " to 42)
            .forEach { (text, expected) -> assertEquals(expected, ClipTimestampParser.parse(text), text) }
    }

    @Test fun `invalid partial and overflowing timestamps`() {
        listOf("", "-1", "1:99", "1:60:00", "abc", "1:", ":42", "1:2:3:4", "1.5", "99999999999999999999", "2147483648", "999999999:00")
            .forEach { assertNull(ClipTimestampParser.parse(it), it) }
        assertEquals(Int.MAX_VALUE, ClipTimestampParser.parse(Int.MAX_VALUE.toString()))
    }

    @Test fun `formatting round trips without locale dependence`() {
        listOf(0, 42, 62, 497, 3600, 3723, Int.MAX_VALUE).forEach {
            assertEquals(it, ClipTimestampParser.parse(formatClipTimestamp(it)))
        }
        assertEquals("1:02:03", formatClipTimestamp(3723))
        assertFailsWith<IllegalArgumentException> { formatClipTimestamp(-1) }
    }
}
