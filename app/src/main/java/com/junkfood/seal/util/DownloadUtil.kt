package com.junkfood.seal.util

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.OPEN_READONLY
import android.media.MediaCodecList
import android.os.Build
import android.util.Log
import android.webkit.CookieManager
import androidx.annotation.CheckResult
import com.junkfood.seal.App
import com.junkfood.seal.App.Companion.audioDownloadDir
import com.junkfood.seal.App.Companion.context
import com.junkfood.seal.App.Companion.videoDownloadDir
import com.junkfood.seal.Downloader
import com.junkfood.seal.Downloader.onProcessEnded
import com.junkfood.seal.Downloader.onProcessStarted
import com.junkfood.seal.Downloader.onTaskEnded
import com.junkfood.seal.Downloader.onTaskError
import com.junkfood.seal.Downloader.onTaskStarted
import com.junkfood.seal.Downloader.toNotificationId
import com.junkfood.seal.R
import com.junkfood.seal.database.objects.CommandTemplate
import com.junkfood.seal.database.objects.DownloadedVideoInfo
import com.junkfood.seal.ui.page.settings.network.Cookie
import com.junkfood.seal.download.DownloadPlan
import com.junkfood.seal.download.YtDlpOption
import com.junkfood.seal.download.buildDownloadPlan
import com.junkfood.seal.util.FileUtil.getArchiveFile
import com.junkfood.seal.util.FileUtil.getConfigFile
import com.junkfood.seal.util.FileUtil.getCookiesFile
import com.junkfood.seal.util.FileUtil.getExternalTempDir
import com.junkfood.seal.util.FileUtil.getFileName
import com.junkfood.seal.util.FileUtil.getSdcardTempDir
import com.junkfood.seal.util.FileUtil.moveFilesToSdcard
import com.junkfood.seal.util.PreferenceUtil.COOKIE_HEADER
import com.junkfood.seal.util.PreferenceUtil.getBoolean
import com.junkfood.seal.util.PreferenceUtil.getInt
import com.junkfood.seal.util.PreferenceUtil.getString
import com.junkfood.seal.util.PreferenceUtil.updateBoolean
import com.junkfood.seal.util.PreferenceUtil.updateInt
import com.junkfood.seal.util.PreferenceUtil.updateString
import com.junkfood.seal.util.connectWithDelimiter
import com.junkfood.seal.util.isNumberInRange
import com.junkfood.seal.util.toHttpsUrl
import com.junkfood.seal.util.toAudioFormatSorter
import com.junkfood.seal.util.toFormatSorter
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object DownloadUtil {

    object CookieScheme {
        const val NAME = "name"
        const val VALUE = "value"
        const val SECURE = "is_secure"
        const val EXPIRY = "expires_utc"
        const val HOST = "host_key"
        const val PATH = "path"
    }

    private val jsonFormat = Json { ignoreUnknownKeys = true }

    private const val TAG = "DownloadUtil"

    const val BASENAME = "%(title).200B"

    const val EXTENSION = ".%(ext)s"

    private const val ID = "[%(id)s]"

    private const val CLIP_TIMESTAMP = "%(section_start)d-%(section_end)d"

    const val OUTPUT_TEMPLATE_DEFAULT = BASENAME + EXTENSION

    const val OUTPUT_TEMPLATE_ID = "$BASENAME $ID$EXTENSION"

    private const val OUTPUT_TEMPLATE_CLIPS = "$BASENAME [$CLIP_TIMESTAMP]$EXTENSION"

    private const val OUTPUT_TEMPLATE_CHAPTERS =
        "chapter:$BASENAME/%(section_number)d - %(section_title).200B$EXTENSION"

    private const val OUTPUT_TEMPLATE_SPLIT = "$BASENAME/$OUTPUT_TEMPLATE_DEFAULT"

    private const val PLAYLIST_TITLE_SUBDIRECTORY_PREFIX = "%(playlist)s/"

    private const val CROP_ARTWORK_COMMAND =
        """--ppa "ffmpeg: -c:v mjpeg -vf crop=\"'if(gt(ih,iw),iw,ih)':'if(gt(iw,ih),ih,iw)'\"""""

    @CheckResult
    fun getPlaylistOrVideoInfo(
        playlistURL: String,
        downloadPreferences: DownloadPreferences = DownloadPreferences.createFromPreferences(),
    ): Result<YoutubeDLInfo> =
        YoutubeDL.runCatching {
            ToastUtil.makeToastSuspend(context.getString(R.string.fetching_playlist_info))
            val request = YoutubeDLRequest(playlistURL)
            with(request) {
                //            addOption("--compat-options", "no-youtube-unavailable-videos")
                addOption("--flat-playlist")
                addOption("--dump-single-json")
                addOption("-o", BASENAME)
                addOption("-R", "1")
                addOption("--socket-timeout", "5")
                downloadPreferences.run {
                    if (extractAudio) {
                        addOption("-x")
                    }
                    applyFormatSorter(this, toFormatSorter())
                    if (proxy) {
                        enableProxy(proxyUrl)
                    }
                    if (forceIpv4) {
                        addOption("-4")
                    }
                    if (cookies) {
                        enableCookies(userAgentString)
                    }
                    if (restrictFilenames) {
                        addOption("--restrict-filenames")
                    }
                }
            }
            execute(request, playlistURL).out.run {
                val playlistInfo = jsonFormat.decodeFromString<PlaylistResult>(this)
                if (playlistInfo.type != "playlist") {
                    jsonFormat.decodeFromString<VideoInfo>(this)
                } else playlistInfo
            }
        }

    @CheckResult
    private fun getVideoInfo(
        request: YoutubeDLRequest,
        taskKey: String? = null,
    ): Result<VideoInfo> =
        request.runCatching {
            val response: YoutubeDLResponse =
                YoutubeDL.getInstance().execute(request, taskKey, null)
            jsonFormat.decodeFromString(response.out)
        }

    @CheckResult
    fun fetchVideoInfoFromUrl(
        url: String,
        playlistIndex: Int? = null,
        taskKey: String? = null,
        preferences: DownloadPreferences = DownloadPreferences.createFromPreferences(),
    ): Result<VideoInfo> {
        with(preferences) {
            val request =
                YoutubeDLRequest(url).apply {
                    addOption("-o", BASENAME)
                    if (restrictFilenames) {
                        addOption("--restrict-filenames")
                    }
                    if (extractAudio) {
                        addOption("-x")
                    }
                    applyFormatSorter(this@with, toFormatSorter())
                    if (cookies) {
                        enableCookies(userAgentString)
                    }
                    if (proxy) {
                        enableProxy(proxyUrl)
                    }
                    if (forceIpv4) {
                        addOption("-4")
                    }
                    /*            if (debug) {
                        addOption("-v")
                    }*/
                    if (autoSubtitle) {
                        addOption("--write-auto-subs")
                        if (!autoTranslatedSubtitles) {
                            addOption("--extractor-args", "youtube:skip=translated_subs")
                        }
                    }
                    if (playlistIndex != null) {
                        addOption("--playlist-items", playlistIndex)
                        addOption("--dump-json")
                    } else {
                        addOption("--dump-single-json")
                    }
                    addOption("-R", "1")
                    addOption("--no-playlist")
                    addOption("--socket-timeout", "5")
                }
            return getVideoInfo(request, taskKey)
        }
    }

    

    private fun YoutubeDLRequest.enableCookies(userAgentString: String): YoutubeDLRequest =
        this.addOption("--cookies", context.getCookiesFile().absolutePath).apply {
            if (userAgentString.isNotEmpty()) {
                addOption("--add-header", "User-Agent:$userAgentString")
            }
        }

    private fun YoutubeDLRequest.enableProxy(proxyUrl: String): YoutubeDLRequest =
        this.addOption("--proxy", proxyUrl)

    private fun YoutubeDLRequest.useDownloadArchive(): YoutubeDLRequest =
        this.addOption("--download-archive", context.getArchiveFile().absolutePath)

    @CheckResult
    fun getCookieListFromDatabase(): Result<List<Cookie>> = runCatching {
        CookieManager.getInstance().run {
            if (!hasCookies()) throw Exception("There is no cookies in the database!")
            flush()
        }
        SQLiteDatabase.openDatabase(
                context.dataDir.resolve("app_webview/Default/Cookies").absolutePath,
                null,
                OPEN_READONLY,
            )
            .run {
                val projection =
                    arrayOf(
                        CookieScheme.HOST,
                        CookieScheme.EXPIRY,
                        CookieScheme.PATH,
                        CookieScheme.NAME,
                        CookieScheme.VALUE,
                        CookieScheme.SECURE,
                    )
                val cookieList = mutableListOf<Cookie>()
                query("cookies", projection, null, null, null, null, null).run {
                    while (moveToNext()) {
                        val expiry = getLong(getColumnIndexOrThrow(CookieScheme.EXPIRY))
                        val name = getString(getColumnIndexOrThrow(CookieScheme.NAME))
                        val value = getString(getColumnIndexOrThrow(CookieScheme.VALUE))
                        val path = getString(getColumnIndexOrThrow(CookieScheme.PATH))
                        val secure = getLong(getColumnIndexOrThrow(CookieScheme.SECURE)) == 1L
                        val hostKey = getString(getColumnIndexOrThrow(CookieScheme.HOST))

                        val host = if (hostKey[0] != '.') ".$hostKey" else hostKey
                        cookieList.add(
                            Cookie(
                                domain = host,
                                name = name,
                                value = value,
                                path = path,
                                secure = secure,
                                expiry = expiry,
                            )
                        )
                    }
                    close()
                }
                close()
                cookieList
            }
    }

    fun List<Cookie>.toCookiesFileContent(): String =
        this.fold(StringBuilder(COOKIE_HEADER)) { acc, cookie ->
                acc.append(cookie.toNetscapeCookieString()).append("\n")
            }
            .toString()

    fun getCookiesContentFromDatabase(): Result<String> =
        getCookieListFromDatabase().mapCatching { it.toCookiesFileContent() }

    private fun YoutubeDLRequest.enableAria2c(): YoutubeDLRequest =
        this.addOption("--downloader", "libaria2c.so")

    private fun YoutubeDLRequest.addOptionsForVideoDownloads(
        downloadPreferences: DownloadPreferences
    ): YoutubeDLRequest =
        this.apply {
            downloadPreferences.run {
                addOption("--add-metadata")
                addOption("--no-embed-info-json")
                if (formatIdString.isNotEmpty()) {
                    addOption("-f", formatIdString)
                    if (mergeAudioStream) {
                        addOption("--audio-multistreams")
                    }
                } else {
                    applyFormatSorter(this, toFormatSorter())
                }
                if (downloadSubtitle) {
                    if (autoSubtitle) {
                        addOption("--write-auto-subs")
                        if (!autoTranslatedSubtitles) {
                            addOption("--extractor-args", "youtube:skip=translated_subs")
                        }
                    }
                    subtitleLanguage
                        .takeIf { it.isNotEmpty() }
                        ?.let { addOption("--sub-langs", it) }
                    if (embedSubtitle) {
                        addOption("--embed-subs")
                        if (keepSubtitle) {
                            addOption("--write-subs")
                        }
                    } else {
                        addOption("--write-subs")
                    }
                    when (convertSubtitle) {
                        CONVERT_ASS -> addOption("--convert-subs", "ass")
                        CONVERT_SRT -> addOption("--convert-subs", "srt")
                        CONVERT_VTT -> addOption("--convert-subs", "vtt")
                        CONVERT_LRC -> addOption("--convert-subs", "lrc")
                        else -> {}
                    }
                }
                if (mergeToMkv) {
                    addOption("--remux-video", "mkv")
                    addOption("--merge-output-format", "mkv")
                }
                if (embedThumbnail) {
                    addOption("--embed-thumbnail")
                }
                if (videoClips.isEmpty()) addOption("--embed-chapters")
            }
        }

    private fun YoutubeDLRequest.addOptionsForAudioDownloads(
        id: String,
        preferences: DownloadPreferences,
        playlistUrl: String,
    ): YoutubeDLRequest =
        this.apply {
            with(preferences) {
                addOption("-x")
                if (downloadSubtitle) {
                    addOption("--write-subs")

                    if (autoSubtitle) {
                        addOption("--write-auto-subs")
                        if (!autoTranslatedSubtitles) {
                            addOption("--extractor-args", "youtube:skip=translated_subs")
                        }
                    }
                    subtitleLanguage
                        .takeIf { it.isNotEmpty() }
                        ?.let { addOption("--sub-langs", it) }
                    when (convertSubtitle) {
                        CONVERT_ASS -> addOption("--convert-subs", "ass")
                        CONVERT_SRT -> addOption("--convert-subs", "srt")
                        CONVERT_VTT -> addOption("--convert-subs", "vtt")
                        CONVERT_LRC -> addOption("--convert-subs", "lrc")
                        else -> {}
                    }
                }
                if (formatIdString.isNotEmpty()) {
                    addOption("-f", formatIdString)
                    if (mergeAudioStream) {
                        addOption("--audio-multistreams")
                    }
                } else if (convertAudio) {
                    when (audioConvertFormat) {
                        CONVERT_MP3 -> {
                            addOption("--audio-format", "mp3")
                        }

                        CONVERT_M4A -> {
                            addOption("--audio-format", "m4a")
                        }
                    }
                } else {
                    applyFormatSorter(preferences, toAudioFormatSorter())
                }

                if (embedMetadata) {
                    addOption("--embed-metadata")
                    addOption("--embed-thumbnail")
                    addOption("--convert-thumbnails", "jpg")

                    if (cropArtwork) {
                        val configFile = context.getConfigFile(id)
                        FileUtil.writeContentToFile(CROP_ARTWORK_COMMAND, configFile)
                        addOption("--config", configFile.absolutePath)
                    }
                }
                addOption("--parse-metadata", "%(release_year,upload_date)s:%(meta_date)s")

                if (playlistUrl.isNotEmpty()) {
                    addOption("--parse-metadata", "%(album,playlist,title)s:%(meta_album)s")
                    addOption("--parse-metadata", "%(track_number,playlist_index)d:%(meta_track)s")
                } else {
                    addOption("--parse-metadata", "%(album,title)s:%(meta_album)s")
                }
            }
        }

    private fun YoutubeDLRequest.applyFormatSorter(
        preferences: DownloadPreferences,
        sorter: String,
    ) =
        preferences.run {
            if (formatSorting && sortingFields.isNotEmpty()) addOption("-S", sortingFields)
            else if (sorter.isNotEmpty()) addOption("-S", sorter) else {}
        }

    private fun insertInfoIntoDownloadHistory(
        videoInfo: VideoInfo,
        filePaths: List<String>,
    ): List<String> =
        filePaths.onEach {
            DatabaseUtil.insertInfo(videoInfo.toDownloadedVideoInfo(videoPath = it))
        }

    private fun VideoInfo.toDownloadedVideoInfo(
        id: Int = 0,
        videoPath: String,
    ): DownloadedVideoInfo =
        this.run {
            DownloadedVideoInfo(
                id = id,
                videoTitle = title,
                videoAuthor = uploader ?: channel ?: uploaderId.toString(),
                videoUrl = webpageUrl ?: originalUrl.toString(),
                thumbnailUrl = thumbnail.toHttpsUrl(),
                videoPath = videoPath,
                extractor = extractorKey,
            )
        }

    private fun insertSplitChapterIntoHistory(videoInfo: VideoInfo, filePaths: List<String>) =
        filePaths.onEach {
            DatabaseUtil.insertInfo(
                videoInfo.toDownloadedVideoInfo(videoPath = it).copy(videoTitle = it.getFileName())
            )
        }

    @CheckResult
    fun downloadVideo(
        videoInfo: VideoInfo? = null,
        playlistUrl: String = "",
        playlistItem: Int = 0,
        taskId: String,
        downloadPreferences: DownloadPreferences,
        progressCallback: ((Float, Long, String) -> Unit)?,
    ): Result<List<String>> {
        if (videoInfo == null)
            return Result.failure(Throwable(context.getString(R.string.fetch_info_error_msg)))

        val plan =
            buildDownloadPlan(
                videoInfo = videoInfo,
                preferences = downloadPreferences,
                playlistUrl = playlistUrl,
                playlistItem = playlistItem,
            )

        val (request, downloadPath) =
            buildRequestFromPlan(
                    plan = plan,
                    videoInfo = videoInfo,
                    preferences = downloadPreferences,
                    playlistUrl = playlistUrl,
                )
                .getOrElse { return Result.failure(it) }

        for (s in request.buildCommand()) Log.d(TAG, s)

        request
            .runCatching {
                YoutubeDL.getInstance()
                    .execute(request = this, processId = taskId, callback = progressCallback)
            }
            .onFailure { th ->
                return if (
                    downloadPreferences.sponsorBlock &&
                        th.message?.contains("Unable to communicate with SponsorBlock API") == true
                ) {
                    th.printStackTrace()
                    onFinishDownloading(
                        preferences = downloadPreferences,
                        videoInfo = videoInfo,
                        downloadPath = downloadPath,
                        sdcardUri = downloadPreferences.sdcardUri,
                    )
                } else Result.failure(th)
            }

        return onFinishDownloading(
            preferences = downloadPreferences,
            videoInfo = videoInfo,
            downloadPath = downloadPath,
            sdcardUri = downloadPreferences.sdcardUri,
        )
    }

    private fun buildRequestFromPlan(
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
                        ?: throw Throwable(context.getString(R.string.fetch_info_error_msg))
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
            else pathBuilder.append(audioDownloadDir)
        } else {
            if (preferences.privateDirectory) pathBuilder.append(App.privateDownloadDir)
            else pathBuilder.append(videoDownloadDir)
        }

        if (preferences.subdirectoryExtractor) {
            pathBuilder.append("/${videoInfo.extractorKey}")
        }

        if (preferences.sdcard) {
            request.addOption("-P", context.getSdcardTempDir(videoInfo.id).absolutePath)
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
            val archiveFile = context.getArchiveFile()
            val archiveFileContent = archiveFile.readText()
            if (archiveFileContent.contains("${videoInfo.extractor} ${videoInfo.id}")) {
                throw YoutubeDLException(context.getString(R.string.download_archive_error))
            }
            request.useDownloadArchive()
        }

        request.addOption("-o", plan.outputTemplate)
    }

    private fun onFinishDownloading(
        preferences: DownloadPreferences,
        videoInfo: VideoInfo,
        downloadPath: String,
        sdcardUri: String,
    ): Result<List<String>> =
        preferences.run {
            val fileName =
                preferences.newTitle.ifEmpty {
                    videoInfo.filename
                        ?: videoInfo.requestedDownloads?.firstOrNull()?.filename
                        ?: videoInfo.title
                }

            Log.d(TAG, "onFinishDownloading: $fileName")
            if (sdcard) {
                moveFilesToSdcard(
                        sdcardUri = sdcardUri,
                        tempPath = context.getSdcardTempDir(videoInfo.id),
                    )
                    .onSuccess {
                        if (privateMode) {
                            return Result.success(emptyList())
                        } else if (splitByChapter) {
                            insertSplitChapterIntoHistory(videoInfo, it)
                        } else {
                            insertInfoIntoDownloadHistory(videoInfo, it)
                        }
                    }
            } else {
                FileUtil.scanFileToMediaLibraryPostDownload(
                        title = fileName,
                        downloadDir = downloadPath,
                    )
                    .run {
                        if (privateMode) Result.success(emptyList())
                        else
                            Result.success(
                                if (splitByChapter) {
                                    insertSplitChapterIntoHistory(videoInfo, this)
                                } else {
                                    insertInfoIntoDownloadHistory(videoInfo, this)
                                }
                            )
                    }
            }
        }

    @CheckResult
    fun executeCustomCommandTask(
        urlString: String,
        taskId: String,
        template: CommandTemplate,
        preferences: DownloadPreferences,
        progressCallback: ((Float, Long, String) -> Unit),
    ): Result<YoutubeDLResponse> {
        val urlList = urlString.split(Regex("[\n ]")).filter { it.isNotBlank() }

        val request =
            with(preferences) {
                YoutubeDLRequest(urlList).apply {
                    commandDirectory.takeIf { it.isNotEmpty() }?.let { addOption("-P", it) }
                    addOption("--newline")
                    if (aria2c) {
                        enableAria2c()
                    }
                    if (useDownloadArchive) {
                        useDownloadArchive()
                    }
                    if (restrictFilenames) {
                        addOption("--restrict-filenames")
                    }
                    addOption(
                        "--config-locations",
                        FileUtil.writeContentToFile(template.template, context.getConfigFile())
                            .absolutePath,
                    )
                    if (cookies) {
                        enableCookies(userAgentString)
                    }
                }
            }

        return runCatching {
            YoutubeDL.getInstance()
                .execute(request = request, processId = taskId, callback = progressCallback)
        }
    }

    suspend fun executeCommandInBackground(
        url: String,
        template: CommandTemplate = PreferenceUtil.getTemplate(),
        downloadPreferences: DownloadPreferences = DownloadPreferences.createFromPreferences(),
    ) {
        downloadPreferences.run {
            val taskId = Downloader.makeKey(url = url, templateName = template.name)
            val notificationId = taskId.toNotificationId()
            val urlList = url.split(Regex("[\n ]")).filter { it.isNotBlank() }

            ToastUtil.makeToastSuspend(context.getString(R.string.start_execute))
            val request =
                YoutubeDLRequest(urlList).apply {
                    commandDirectory.takeIf { it.isNotEmpty() }?.let { addOption("-P", it) }
                    addOption("--newline")
                    if (aria2c) {
                        enableAria2c()
                    }
                    if (useDownloadArchive) {
                        useDownloadArchive()
                    }
                    if (restrictFilenames) {
                        addOption("--restrict-filenames")
                    }
                    addOption(
                        "--config-locations",
                        FileUtil.writeContentToFile(template.template, context.getConfigFile())
                            .absolutePath,
                    )
                    if (cookies) {
                        enableCookies(userAgentString)
                    }
                }

            onProcessStarted()
            withContext(Dispatchers.Main) { onTaskStarted(template, url) }
            runCatching {
                    val response =
                        YoutubeDL.getInstance().execute(request = request, processId = taskId) {
                            progress,
                            _,
                            text ->
                            NotificationUtil.makeNotificationForCustomCommand(
                                notificationId = notificationId,
                                taskId = taskId,
                                progress = progress.toInt(),
                                templateName = template.name,
                                taskUrl = url,
                                text = text,
                            )
                            Downloader.updateTaskOutput(
                                template = template,
                                url = url,
                                line = text,
                                progress = progress,
                            )
                        }
                    onTaskEnded(template, url, response.out + "\n" + response.err)
                }
                .onFailure {
                    it.printStackTrace()
                    if (it is YoutubeDL.CanceledException) return@onFailure
                    it.message.run {
                        if (isNullOrEmpty()) onTaskEnded(template, url)
                        else onTaskError(this, template, url)
                    }
                }
            onProcessEnded()
        }
    }

    internal fun checkIfAv1HardwareAccelerated(): Boolean {
        if (PreferenceUtil.containsKey(AV1_HARDWARE_ACCELERATED)) {
            return AV1_HARDWARE_ACCELERATED.getBoolean()
        } else {
            val res =
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    false
                } else {
                    MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.any { info ->
                        info.supportedTypes.any { it.equals("video/av01", ignoreCase = true) } &&
                            info.isHardwareAccelerated
                    }
                }
            AV1_HARDWARE_ACCELERATED.updateBoolean(res)
            return res
        }
    }
}

fun DownloadPreferences.Companion.createFromPreferences(): DownloadPreferences {
    val downloadSubtitle = SUBTITLE.getBoolean()
    val embedSubtitle = EMBED_SUBTITLE.getBoolean()
    return DownloadPreferences(
        extractAudio = EXTRACT_AUDIO.getBoolean(),
        createThumbnail = THUMBNAIL.getBoolean(),
        downloadPlaylist = PLAYLIST.getBoolean(),
        subdirectoryExtractor = SUBDIRECTORY_EXTRACTOR.getBoolean(),
        subdirectoryPlaylistTitle = SUBDIRECTORY_PLAYLIST_TITLE.getBoolean(),
        commandDirectory = COMMAND_DIRECTORY.getString(),
        downloadSubtitle = downloadSubtitle,
        embedSubtitle = embedSubtitle,
        keepSubtitle = KEEP_SUBTITLE_FILES.getBoolean(),
        subtitleLanguage = SUBTITLE_LANGUAGE.getString(),
        autoSubtitle = AUTO_SUBTITLE.getBoolean(),
        autoTranslatedSubtitles = AUTO_TRANSLATED_SUBTITLES.getBoolean(),
        convertSubtitle = CONVERT_SUBTITLE.getInt(),
        concurrentFragments = CONCURRENT.getInt(),
        sponsorBlock = SPONSORBLOCK.getBoolean(),
        sponsorBlockCategory = PreferenceUtil.getSponsorBlockCategories(),
        cookies = COOKIES.getBoolean(),
        aria2c = ARIA2C.getBoolean(),
        useCustomAudioPreset = USE_CUSTOM_AUDIO_PRESET.getBoolean(),
        audioFormat = AUDIO_FORMAT.getInt(),
        audioQuality = AUDIO_QUALITY.getInt(),
        convertAudio = AUDIO_CONVERT.getBoolean(),
        formatSorting = FORMAT_SORTING.getBoolean(),
        sortingFields = SORTING_FIELDS.getString(),
        audioConvertFormat = PreferenceUtil.getAudioConvertFormat(),
        videoFormat = PreferenceUtil.getVideoFormat(),
        formatIdString = "",
        videoResolution = PreferenceUtil.getVideoResolution(),
        privateMode = PRIVATE_MODE.getBoolean(),
        rateLimit = RATE_LIMIT.getBoolean(),
        maxDownloadRate = PreferenceUtil.getMaxDownloadRate(),
        privateDirectory = PRIVATE_DIRECTORY.getBoolean(),
        cropArtwork = CROP_ARTWORK.getBoolean(),
        sdcard = SDCARD_DOWNLOAD.getBoolean(),
        sdcardUri = SDCARD_URI.getString(),
        embedThumbnail = EMBED_THUMBNAIL.getBoolean(),
        videoClips = emptyList<VideoClip>(),
        splitByChapter = false,
        debug = DEBUG.getBoolean(),
        proxy = PROXY.getBoolean(),
        proxyUrl = PROXY_URL.getString(),
        newTitle = "",
        userAgentString = USER_AGENT_STRING.run { if (USER_AGENT.getBoolean()) getString() else "" },
        outputTemplate = OUTPUT_TEMPLATE.getString(),
        useDownloadArchive = DOWNLOAD_ARCHIVE.getBoolean(),
        embedMetadata = EMBED_METADATA.getBoolean(),
        restrictFilenames = RESTRICT_FILENAMES.getBoolean(),
        supportAv1HardwareDecoding = DownloadUtil.checkIfAv1HardwareAccelerated(),
        forceIpv4 = FORCE_IPV4.getBoolean(),
        mergeAudioStream = false,
        mergeToMkv = (downloadSubtitle && embedSubtitle) || MERGE_OUTPUT_MKV.getBoolean(),
    )
}
