package com.junkfood.seal.util

/** User-facing video format preference. Mirrors the int codes stored in DownloadPreferences. */
enum class VideoFormatPreference(val code: Int) {
    COMPATIBILITY(1),
    QUALITY(2),
}

/** Audio container preference. */
enum class AudioFormatPreference(val code: Int) {
    OPUS(1),
    M4A(2),
}

/** Audio quality preference. */
enum class AudioQualityPreference(val code: Int) {
    HIGH(1),
    MEDIUM(2),
    LOW(3),
    ULTRA_LOW(4),
}

private fun Int.toVideoFormatPreference(): VideoFormatPreference? =
    VideoFormatPreference.entries.firstOrNull { it.code == this }

private fun Int.toAudioFormatPreference(): AudioFormatPreference? =
    AudioFormatPreference.entries.firstOrNull { it.code == this }

private fun Int.toAudioQualityPreference(): AudioQualityPreference? =
    AudioQualityPreference.entries.firstOrNull { it.code == this }

fun DownloadPreferences.toAudioFormatSorter(): String =
    run {
        if (!useCustomAudioPreset) return ""

        val format =
            when (audioFormat.toAudioFormatPreference()) {
                AudioFormatPreference.M4A -> "acodec:aac"
                AudioFormatPreference.OPUS -> "acodec:opus"
                null -> ""
            }

        val quality =
            when (audioQuality.toAudioQualityPreference()) {
                AudioQualityPreference.HIGH -> "abr~192"
                AudioQualityPreference.MEDIUM -> "abr~128"
                AudioQualityPreference.LOW -> "abr~64"
                AudioQualityPreference.ULTRA_LOW, null -> ""
            }

        connectWithDelimiter(format, quality, delimiter = ",")
    }

fun DownloadPreferences.toVideoFormatSorter(): String =
    run {
        val format =
            when (videoFormat.toVideoFormatPreference()) {
                VideoFormatPreference.COMPATIBILITY -> "proto,vcodec:h264,ext"
                VideoFormatPreference.QUALITY ->
                    if (supportAv1HardwareDecoding) {
                        "vcodec:av01"
                    } else {
                        "vcodec:vp9.2"
                    }
                null -> ""
            }

        val res =
            when (videoResolution) {
                1 -> "res:2160"
                2 -> "res:1440"
                3 -> "res:1080"
                4 -> "res:720"
                5 -> "res:480"
                6 -> "res:360"
                7 -> "+res"
                else -> ""
            }

        val sorter =
            if (videoFormat.toVideoFormatPreference() == VideoFormatPreference.COMPATIBILITY) {
                connectWithDelimiter(format, res, delimiter = ",")
            } else {
                connectWithDelimiter(res, format, delimiter = ",")
            }

        sorter
    }

/** Combine video/audio sorting preferences into a single sorter string for yt-dlp. */
fun DownloadPreferences.toFormatSorter(): String =
    connectWithDelimiter(toVideoFormatSorter(), toAudioFormatSorter(), delimiter = ",")
