package com.junkfood.seal.download

import com.junkfood.seal.util.DownloadPreferences
import com.junkfood.seal.util.Format
import com.junkfood.seal.util.VideoClip
import com.junkfood.seal.util.VideoInfo

/**
 * Pure helper to merge user selections (formats/subtitles/clips/newTitle) into preferences and video info.
 */
object SelectionMerge {
    data class Result(
        val videoInfo: VideoInfo,
        val preferences: DownloadPreferences,
        val audioFormats: List<Format>,
        val videoFormats: List<Format>,
    )

    fun merge(
        basePreferences: DownloadPreferences,
        videoInfo: VideoInfo,
        formatList: List<Format>,
        videoClips: List<VideoClip>,
        splitByChapter: Boolean,
        newTitle: String,
        selectedSubtitles: List<String>,
        selectedAutoCaptions: List<String>,
    ): Result {
        val fileSize = formatList.fold(0.0) { acc, format -> acc + (format.fileSize ?: format.fileSizeApprox ?: 0.0) }
        val updatedInfo =
            videoInfo
                .run { if (fileSize != 0.0) copy(fileSize = fileSize) else this }
                .run { if (newTitle.isNotEmpty()) copy(title = newTitle) else this }

        val audioOnlyFormats = formatList.filter { it.isAudioOnly() }
        val videoFormats = formatList.filter { it.containsVideo() }
        val audioOnly = audioOnlyFormats.isNotEmpty() && videoFormats.isEmpty()
        val mergeAudioStream = audioOnlyFormats.size > 1
        val formatId = formatList.joinToString(separator = "+") { it.formatId.orEmpty() }

        val subtitleLanguage = (selectedSubtitles + selectedAutoCaptions).joinToString(separator = ",")

        val mergedPreferences =
            basePreferences
                .copy(
                    formatIdString = formatId,
                    videoClips = videoClips,
                    splitByChapter = splitByChapter,
                    newTitle = newTitle,
                    mergeAudioStream = mergeAudioStream,
                    extractAudio = basePreferences.extractAudio || audioOnly,
                )
                .run {
                    if (subtitleLanguage.isNotEmpty()) {
                        copy(
                            downloadSubtitle = true,
                            autoSubtitle = selectedAutoCaptions.isNotEmpty(),
                            subtitleLanguage = subtitleLanguage,
                        )
                    } else this
                }

        return Result(
            videoInfo = updatedInfo,
            preferences = mergedPreferences,
            audioFormats = audioOnlyFormats,
            videoFormats = videoFormats,
        )
    }
}
