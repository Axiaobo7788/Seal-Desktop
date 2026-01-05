package com.junkfood.seal.download

import android.os.Build
import com.junkfood.seal.App
import com.junkfood.seal.R
import com.junkfood.seal.util.DownloadPreferences
import com.junkfood.seal.util.FileUtil.getArchiveFile
import com.junkfood.seal.util.FileUtil.getConfigFile
import com.junkfood.seal.util.FileUtil.getCookiesFile
import com.junkfood.seal.util.FileUtil.getExternalTempDir
import com.junkfood.seal.util.FileUtil.getSdcardTempDir
import com.junkfood.seal.util.FileUtil.writeContentToFile
import com.junkfood.seal.util.VideoInfo
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest

/**
 * Android adapter that maps shared DownloadPlan/CustomCommandPlan to YoutubeDLRequest.
 * Platform-specific paths/cookies/archive handling stay here, keeping DownloadUtil lean.
 */
object YoutubeDlRequestAdapter {

    private const val CROP_ARTWORK_COMMAND =
        """--ppa \"ffmpeg: -c:v mjpeg -vf crop=\"'if(gt(ih,iw),iw,ih)':'if(gt(iw,ih),ih,iw)'\"\""""

    /** Build a YoutubeDLRequest from DownloadPlan and return request + download directory. */
    fun buildRequestFromPlan(
        plan: DownloadPlan,
        videoInfo: VideoInfo,
        preferences: DownloadPreferences,
        playlistUrl: String,
    ): Result<Pair<YoutubeDLRequest, String>> =
        runCatching {
            val url =
                playlistUrl.ifEmpty {
                    videoInfo.originalUrl
                        ?: videoInfo.webpageUrl
                        ?: throw Throwable(App.context.getString(R.string.fetch_info_error_msg))
                }

            val request = YoutubeDLRequest(url)
            val downloadPath = configureDownloadPaths(request, preferences, videoInfo)
            applyPlanToRequest(request, plan, preferences, videoInfo)
            request to downloadPath
        }

    private fun configureDownloadPaths(
        request: YoutubeDLRequest,
        preferences: DownloadPreferences,
        videoInfo: VideoInfo,
    ): String {
        val pathBuilder = StringBuilder()
        val isAudioLike = preferences.extractAudio || videoInfo.vcodec == "none"

        if (isAudioLike) {
            if (preferences.privateDirectory) pathBuilder.append(App.privateDownloadDir)
            else pathBuilder.append(App.audioDownloadDir)
        } else {
            if (preferences.privateDirectory) pathBuilder.append(App.privateDownloadDir)
            else pathBuilder.append(App.videoDownloadDir)
        }

        if (preferences.subdirectoryExtractor) {
            pathBuilder.append("/${videoInfo.extractorKey}")
        }

        if (preferences.sdcard) {
            request.addOption("-P", App.context.getSdcardTempDir(videoInfo.id).absolutePath)
        } else {
            request.addOption("-P", pathBuilder.toString())
        }

        if (Build.VERSION.SDK_INT > 23 && !preferences.sdcard) {
            request.addOption("-P", "temp:" + getExternalTempDir())
        }

        return pathBuilder.toString()
    }

    private fun applyPlanToRequest(
        request: YoutubeDLRequest,
        plan: DownloadPlan,
        preferences: DownloadPreferences,
        videoInfo: VideoInfo,
    ) {
        plan.options.forEach { option ->
            when (option) {
                is YtDlpOption.Flag -> request.addOption(option.name)
                is YtDlpOption.KeyValue -> request.addOption(option.name, option.value)
                is YtDlpOption.Multi -> request.addCommands(listOf(option.name) + option.values)
            }
        }

        if (preferences.cookies && plan.needsCookiesFile) {
            request.enableCookies(preferences.userAgentString)
        } else if (preferences.userAgentString.isNotEmpty()) {
            request.addOption("--add-header", "User-Agent:${preferences.userAgentString}")
        }

        if (preferences.useDownloadArchive && plan.needsArchiveFile) {
            val archiveFile = App.context.getArchiveFile()
            val archiveFileContent = archiveFile.readText()
            if (archiveFileContent.contains("${videoInfo.extractor} ${videoInfo.id}")) {
                throw YoutubeDLException(App.context.getString(R.string.download_archive_error))
            }
            request.useDownloadArchive()
        }

        request.addOption("-o", plan.outputTemplate)
        applyCropArtworkIfNeeded(request, preferences, videoInfo)
    }

    fun applyCustomCommandPlan(
        request: YoutubeDLRequest,
        plan: CustomCommandPlan,
        preferences: DownloadPreferences,
    ) {
        plan.options.forEach { opt ->
            when (opt) {
                is YtDlpOption.Flag -> request.addOption(opt.name)
                is YtDlpOption.KeyValue -> request.addOption(opt.name, opt.value)
                is YtDlpOption.Multi -> request.addCommands(listOf(opt.name) + opt.values)
            }
        }

        if (plan.needsCookiesFile && preferences.cookies) {
            request.enableCookies(preferences.userAgentString)
        }
        if (plan.needsArchiveFile && preferences.useDownloadArchive) {
            request.useDownloadArchive()
        }
    }

    private fun applyCropArtworkIfNeeded(
        request: YoutubeDLRequest,
        preferences: DownloadPreferences,
        videoInfo: VideoInfo,
    ) {
        val isAudioLike = preferences.extractAudio || videoInfo.vcodec == "none"
        if (isAudioLike && preferences.embedMetadata && preferences.cropArtwork) {
            val configFile = App.context.getConfigFile(videoInfo.id)
            writeContentToFile(CROP_ARTWORK_COMMAND, configFile)
            request.addOption("--config", configFile.absolutePath)
        }
    }
}

private fun YoutubeDLRequest.enableCookies(userAgentString: String): YoutubeDLRequest =
    this.addOption("--cookies", App.context.getCookiesFile().absolutePath).apply {
        if (userAgentString.isNotEmpty()) {
            addOption("--add-header", "User-Agent:$userAgentString")
        }
    }

private fun YoutubeDLRequest.useDownloadArchive(): YoutubeDLRequest =
    this.addOption("--download-archive", App.context.getArchiveFile().absolutePath)

