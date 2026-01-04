package com.junkfood.seal.download

import com.junkfood.seal.util.PlaylistEntry
import com.junkfood.seal.util.PlaylistResult
import com.junkfood.seal.util.Thumbnail
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaylistSelectionMapperTest {

    @Test
    fun `maps playlist entries with fallbacks`() {
        val entries = listOf(
            PlaylistEntry(id = "1", url = "u1", title = "t1", duration = 10.0, uploader = "up1"),
            PlaylistEntry(id = "2", url = null, title = null, duration = null, thumbnails = listOf(Thumbnail(url = "thumb2"))),
        )
        val result =
            PlaylistSelectionMapper.map(
                PlaylistResult(title = "pl", channel = "chn", entries = entries),
                indexList = listOf(1, 2),
            )

        assertEquals(2, result.size)
        val first = result[0]
        assertEquals(1, first.first)
        assertEquals("u1", first.second.url)
        assertEquals("t1", first.second.title)
        assertEquals(10, first.second.duration)
        assertEquals("up1", first.second.uploader)

        val second = result[1]
        assertEquals(2, second.first)
        assertEquals("", second.second.url)
        assertEquals("pl - 2", second.second.title)
        assertEquals(0, second.second.duration)
        assertEquals("chn", second.second.uploader)
        assertEquals("thumb2", second.second.thumbnailUrl)
    }
}
