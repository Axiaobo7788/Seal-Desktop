package com.junkfood.seal.desktop.download.clip

import com.junkfood.seal.util.VideoClip

object ClipTimestampParser {
    fun parse(text: String): Int? {
        val parts = text.trim().split(':')
        if (parts.size !in 1..3 || parts.any { it.isEmpty() || it.any { c -> c !in '0'..'9' } }) return null
        val values = parts.map { it.toLongOrNull() ?: return null }
        if (parts.size > 1 && values.last() >= 60) return null
        if (parts.size == 3 && values[1] >= 60) return null
        var seconds = 0L
        values.forEach { value ->
            if (value > Int.MAX_VALUE || seconds > (Int.MAX_VALUE - value) / 60L) return null
            seconds = seconds * 60 + value
        }
        return seconds.toInt()
    }
}

fun formatClipTimestamp(seconds: Int): String {
    require(seconds >= 0)
    val s = (seconds % 60).toString().padStart(2, '0')
    return if (seconds >= 3600) {
        "${seconds / 3600}:${(seconds / 60 % 60).toString().padStart(2, '0')}:$s"
    } else "${seconds / 60}:$s"
}

object ClipRangePolicy {
    fun valid(start: Int, end: Int, duration: Int): Boolean =
        duration > 0 && start >= 0 && end > start && end <= duration

    fun available(duration: Double?, liveStatus: String?): Boolean =
        duration != null && duration.isFinite() && duration >= 1 && duration <= Int.MAX_VALUE &&
            liveStatus !in setOf("is_live", "is_upcoming", "post_live")
}

enum class VideoEditMode { None, Clip, Chapters }

object VideoEditModePolicy {
    fun restore(clips: List<VideoClip>, split: Boolean, clipAvailable: Boolean, chapters: Int): VideoEditMode = when {
        clipAvailable && clips.isNotEmpty() -> VideoEditMode.Clip
        split && chapters > 0 -> VideoEditMode.Chapters
        else -> VideoEditMode.None
    }

    fun toggle(current: VideoEditMode, requested: VideoEditMode): VideoEditMode =
        if (current == requested) VideoEditMode.None else requested
}

data class ClipEditorState(val duration: Int, val range: VideoClip, val startDraft: String, val endDraft: String) {
    val validDraft: VideoClip?
        get() {
            val start = ClipTimestampParser.parse(startDraft) ?: return null
            val end = ClipTimestampParser.parse(endDraft) ?: return null
            return if (ClipRangePolicy.valid(start, end, duration)) VideoClip(start, end) else null
        }

    // Partial timestamp edits never destroy the last usable slider range.
    fun edit(start: String = startDraft, end: String = endDraft): ClipEditorState {
        val draft = copy(startDraft = start, endDraft = end)
        return draft.copy(range = draft.validDraft ?: range)
    }

    fun move(start: Int, end: Int): ClipEditorState =
        if (!ClipRangePolicy.valid(start, end, duration)) this
        else copy(range = VideoClip(start, end), startDraft = formatClipTimestamp(start), endDraft = formatClipTimestamp(end))

    fun reset() = create(duration)

    companion object {
        fun create(duration: Int, saved: VideoClip? = null): ClipEditorState {
            require(duration > 0)
            val range = saved?.takeIf { ClipRangePolicy.valid(it.start, it.end, duration) } ?: VideoClip(0, duration)
            return ClipEditorState(duration, range, formatClipTimestamp(range.start), formatClipTimestamp(range.end))
        }
    }
}
