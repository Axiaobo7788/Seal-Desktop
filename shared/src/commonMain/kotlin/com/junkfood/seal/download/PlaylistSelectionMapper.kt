package com.junkfood.seal.download

import com.junkfood.seal.util.PlaylistResult
import kotlin.math.roundToInt

/**
 * Produce lightweight view data for selected playlist items.
 */
object PlaylistSelectionMapper {
    data class ItemView(
        val url: String,
        val title: String,
        val duration: Int,
        val uploader: String,
        val thumbnailUrl: String,
    )

    fun map(playlistResult: PlaylistResult, indexList: List<Int>): List<Pair<Int, ItemView>> {
        val entries = playlistResult.entries ?: return emptyList()
        val indexEntryMap = indexList.associateWith { idx -> entries.getOrNull(idx - 1) }
        return indexEntryMap.mapNotNull { (index, entry) ->
            val e = entry ?: return@mapNotNull null
            val view =
                ItemView(
                    url = e.url ?: "",
                    title = e.title ?: "${playlistResult.title} - $index",
                    duration = e.duration?.roundToInt() ?: 0,
                    uploader = e.uploader ?: e.channel ?: playlistResult.channel ?: "",
                    thumbnailUrl = e.thumbnails?.lastOrNull()?.url ?: "",
                )
            index to view
        }
    }
}
