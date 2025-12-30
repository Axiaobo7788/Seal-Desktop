package com.junkfood.seal.download

import com.junkfood.seal.util.DownloadPreferences
import com.junkfood.seal.util.VideoClip
import com.junkfood.seal.util.VideoInfo
import com.junkfood.seal.util.isNumberInRange
import com.junkfood.seal.util.toAudioFormatSorter
import com.junkfood.seal.util.toFormatSorter

private const val BASENAME = "%(title).200B"
private const val EXTENSION = ".%(ext)s"
private const val CLIP_TIMESTAMP = "%(section_start)d-%(section_end)d"
private const val OUTPUT_TEMPLATE_CLIPS = "$BASENAME [$CLIP_TIMESTAMP]$EXTENSION"
private const val OUTPUT_TEMPLATE_CHAPTERS =
    "chapter:$BASENAME/%(section_number)d - %(section_title).200B$EXTENSION"
private const val OUTPUT_TEMPLATE_SPLIT = "$BASENAME/${DownloadPlanBuilder.DEFAULT_OUTPUT_TEMPLATE}"
private const val PLAYLIST_TITLE_SUBDIRECTORY_PREFIX = "%(playlist)s/"

private const val CONVERT_ASS = 1
private const val CONVERT_LRC = 2
private const val CONVERT_SRT = 3
private const val CONVERT_VTT = 4

private const val CONVERT_MP3 = 0
private const val CONVERT_M4A = 1

/**
 * Map preferences + video info + user selections into a platform-neutral DownloadPlan.
 * Paths/cookies/archive files remain adapter responsibilities.
 */
fun buildDownloadPlan(
    videoInfo: VideoInfo,
    preferences: DownloadPreferences,
    playlistUrl: String = "",
    playlistItem: Int = 0,
): DownloadPlan {
    val builder = DownloadPlanBuilder()

    // Housekeeping & connectivity
    builder.flag("--no-mtime")
    if (preferences.cookies) builder.markNeedsCookies()
    if (preferences.restrictFilenames) builder.flag("--restrict-filenames")
    if (preferences.proxy) builder.option("--proxy", preferences.proxyUrl)
    if (preferences.forceIpv4) builder.flag("-4")
    if (preferences.debug) builder.flag("-v")
    if (preferences.rateLimit && preferences.maxDownloadRate.isNumberInRange(1, 1_000_000)) {
        builder.option("-r", "${preferences.maxDownloadRate}K")
    }

    // Playlist scoping
    var outputPrefix = ""
    if (playlistItem != 0 && preferences.downloadPlaylist) {
        builder.option("--playlist-items", playlistItem.toString())
        if (preferences.subdirectoryPlaylistTitle && !videoInfo.playlist.isNullOrEmpty()) {
            outputPrefix = PLAYLIST_TITLE_SUBDIRECTORY_PREFIX
        }
    } else {
        builder.flag("--no-playlist")
    }

    // Concurrency / downloader
    if (preferences.aria2c) {
        builder.option("--downloader", "libaria2c.so")
    } else if (preferences.concurrentFragments > 1) {
        builder.option("--concurrent-fragments", preferences.concurrentFragments.toString())
    }

    // Mode selection
    val isAudioLike = preferences.extractAudio || videoInfo.vcodec == "none"
    if (isAudioLike) {
        addAudioOptions(builder, preferences, playlistUrl)
        builder.downloadPathHint = "audio"
    } else {
        addVideoOptions(builder, preferences)
        builder.downloadPathHint = "video"
    }

    // Misc feature toggles
    if (preferences.sponsorBlock) {
        builder.option("--sponsorblock-remove", preferences.sponsorBlockCategory)
    }
    if (preferences.createThumbnail) {
        builder.flag("--write-thumbnail")
        builder.option("--convert-thumbnails", "png")
    }

    addClipSections(builder, preferences.videoClips)
    addReplaceTitle(builder, preferences.newTitle)

    if (preferences.splitByChapter) {
        builder.option("-o", OUTPUT_TEMPLATE_CHAPTERS)
        builder.flag("--split-chapters")
    }

    builder.outputTemplate = buildOutputTemplate(preferences, outputPrefix)

    if (preferences.useDownloadArchive) builder.markNeedsArchive()

    return builder.build()
}

private fun addClipSections(builder: DownloadPlanBuilder, clips: List<VideoClip>) {
    clips.forEach { clip ->
        builder.option(
            "--download-sections",
            "*%d-%d".format(locale = java.util.Locale.US, clip.start, clip.end),
        )
    }
}

private fun addReplaceTitle(builder: DownloadPlanBuilder, newTitle: String) {
    if (newTitle.isNotEmpty()) {
        builder.option("--replace-in-metadata", "title", ".+", newTitle)
    }
}

private fun addVideoOptions(builder: DownloadPlanBuilder, preferences: DownloadPreferences) {
    builder.flag("--add-metadata")
    builder.flag("--no-embed-info-json")

    if (preferences.formatIdString.isNotEmpty()) {
        builder.option("-f", preferences.formatIdString)
        if (preferences.mergeAudioStream) {
            builder.flag("--audio-multistreams")
        }
    } else {
        val sorter = preferences.toFormatSorter()
        if (preferences.formatSorting && preferences.sortingFields.isNotEmpty()) {
            builder.option("-S", preferences.sortingFields)
        } else if (sorter.isNotEmpty()) {
            builder.option("-S", sorter)
        }
    }

    if (preferences.downloadSubtitle) {
        builder.addSubtitleOptions(
            autoSubtitle = preferences.autoSubtitle,
            autoTranslated = preferences.autoTranslatedSubtitles,
            subtitleLanguage = preferences.subtitleLanguage,
            embedSubtitle = preferences.embedSubtitle,
            keepSubtitle = preferences.keepSubtitle,
            convertSubtitle = preferences.convertSubtitle,
        )
    }

    if (preferences.mergeToMkv) {
        builder.option("--remux-video", "mkv")
        builder.option("--merge-output-format", "mkv")
    }
    if (preferences.embedThumbnail) {
        builder.flag("--embed-thumbnail")
    }
    if (preferences.videoClips.isEmpty()) {
        builder.flag("--embed-chapters")
    }
}

private fun addAudioOptions(
    builder: DownloadPlanBuilder,
    preferences: DownloadPreferences,
    playlistUrl: String,
) {
    builder.flag("-x")
    if (preferences.downloadSubtitle) {
        builder.flag("--write-subs")
        builder.addSubtitleOptions(
            autoSubtitle = preferences.autoSubtitle,
            autoTranslated = preferences.autoTranslatedSubtitles,
            subtitleLanguage = preferences.subtitleLanguage,
            embedSubtitle = false,
            keepSubtitle = false,
            convertSubtitle = preferences.convertSubtitle,
        )
    }

    if (preferences.formatIdString.isNotEmpty()) {
        builder.option("-f", preferences.formatIdString)
        if (preferences.mergeAudioStream) {
            builder.flag("--audio-multistreams")
        }
    } else if (preferences.convertAudio) {
        when (preferences.audioConvertFormat) {
            CONVERT_MP3 -> builder.option("--audio-format", "mp3")
            CONVERT_M4A -> builder.option("--audio-format", "m4a")
        }
    } else {
        val sorter = preferences.toAudioFormatSorter()
        if (preferences.formatSorting && preferences.sortingFields.isNotEmpty()) {
            builder.option("-S", preferences.sortingFields)
        } else if (sorter.isNotEmpty()) {
            builder.option("-S", sorter)
        }
    }

    if (preferences.embedMetadata) {
        builder.flag("--embed-metadata")
        builder.flag("--embed-thumbnail")
        builder.option("--convert-thumbnails", "jpg")
    }

    builder.option("--parse-metadata", "%(release_year,upload_date)s:%(meta_date)s")
    if (playlistUrl.isNotEmpty()) {
        builder.option("--parse-metadata", "%(album,playlist,title)s:%(meta_album)s")
        builder.option("--parse-metadata", "%(track_number,playlist_index)d:%(meta_track)s")
    } else {
        builder.option("--parse-metadata", "%(album,title)s:%(meta_album)s")
    }
}

private fun DownloadPlanBuilder.addSubtitleOptions(
    autoSubtitle: Boolean,
    autoTranslated: Boolean,
    subtitleLanguage: String,
    embedSubtitle: Boolean,
    keepSubtitle: Boolean,
    convertSubtitle: Int,
) {
    if (autoSubtitle) {
        option("--write-auto-subs")
        if (!autoTranslated) {
            option("--extractor-args", "youtube:skip=translated_subs")
        }
    }
    subtitleLanguage.takeIf { it.isNotEmpty() }?.let { option("--sub-langs", it) }

    if (embedSubtitle) {
        option("--embed-subs")
        if (keepSubtitle) {
            option("--write-subs")
        }
    } else {
        option("--write-subs")
    }

    when (convertSubtitle) {
        CONVERT_ASS -> option("--convert-subs", "ass")
        CONVERT_SRT -> option("--convert-subs", "srt")
        CONVERT_VTT -> option("--convert-subs", "vtt")
        CONVERT_LRC -> option("--convert-subs", "lrc")
    }
}

private fun buildOutputTemplate(preferences: DownloadPreferences, prefix: String): String {
    val output =
        when {
            preferences.splitByChapter -> OUTPUT_TEMPLATE_SPLIT
            preferences.videoClips.isNotEmpty() -> OUTPUT_TEMPLATE_CLIPS
            else -> preferences.outputTemplate.ifEmpty { DownloadPlanBuilder.DEFAULT_OUTPUT_TEMPLATE }
        }
    return prefix + output
}
