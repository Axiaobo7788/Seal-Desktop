package com.junkfood.seal.desktop.ui.page.downloadv2.configure

import com.junkfood.seal.util.Format
import java.util.Locale

internal object FormatSearchMatcher {
    fun matches(format: Format, query: String): Boolean {
        val tokens = query.trim().lowercase(Locale.ROOT).split(Regex("\\s+")).filter(String::isNotEmpty)
        if (tokens.isEmpty()) return true
        val text = buildList {
            addAll(listOfNotNull(format.formatId, format.resolution, format.ext, format.vcodec, format.acodec, format.format, format.formatNote))
            listOfNotNull(format.width, format.height, format.fps, format.abr).forEach { add(number(it)) }
            format.height?.let { height ->
                add("${number(height)}p")
                format.fps?.let { add("${number(height)}p${number(it)}") }
                format.width?.let { add("${number(it)}x${number(height)}") }
            }
            format.abr?.let { add("${number(it)}kbps") }
            if (format.vcodec?.startsWith("av01", ignoreCase = true) == true) add("av1")
            if (format.vcodec?.startsWith("vp09", ignoreCase = true) == true) add("vp9")
        }.joinToString(" ").lowercase(Locale.ROOT)
        return tokens.all(text::contains)
    }

    // Keep original category indexes: a filtered row must not select a different format.
    fun visible(formats: List<Format>, query: String, limit: Int): List<IndexedValue<Format>> =
        formats.withIndex().filter { matches(it.value, query) }.let {
            if (query.isBlank()) it.take(limit) else it
        }

    private fun number(value: Double): String =
        if (value.isFinite() && value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
}
