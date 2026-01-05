package com.junkfood.seal.util

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.OPEN_READONLY
import android.media.MediaCodecList
import android.os.Build
import android.util.Log
import android.webkit.CookieManager
import androidx.annotation.CheckResult
import com.junkfood.seal.App
import com.junkfood.seal.App.Companion.context
import com.junkfood.seal.Downloader
import com.junkfood.seal.Downloader.onProcessEnded
import com.junkfood.seal.Downloader.onProcessStarted
import com.junkfood.seal.Downloader.onTaskEnded
import com.junkfood.seal.Downloader.onTaskError
import com.junkfood.seal.Downloader.onTaskStarted
import com.junkfood.seal.Downloader.toNotificationId
import com.junkfood.seal.R
import com.junkfood.seal.BuildConfig
import com.junkfood.seal.database.objects.CommandTemplate
import com.junkfood.seal.database.objects.DownloadedVideoInfo
import com.junkfood.seal.ui.page.settings.network.Cookie
import com.junkfood.seal.download.CustomCommandPlan
import com.junkfood.seal.download.DownloadPlan
import com.junkfood.seal.download.YoutubeDlRequestAdapter
import com.junkfood.seal.download.buildDownloadPlan
import com.junkfood.seal.download.buildCustomCommandPlan
import com.junkfood.seal.util.FileUtil.getConfigFile
import com.junkfood.seal.util.FileUtil.getCookiesFile
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
import java.io.File

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

    /**
     * Debug aid: log shared plan options vs the request's final command to spot drift/regressions.
     * Emits in debug builds or when preferences.debug=true.
     */
    private fun logPlanDebug(plan: DownloadPlan, request: YoutubeDLRequest) {
        val flattenedPlan =
            plan.options.flatMap { opt ->
                when (opt) {
                    is com.junkfood.seal.download.YtDlpOption.Flag -> listOf(opt.name)
                    is com.junkfood.seal.download.YtDlpOption.KeyValue -> listOf(opt.name, opt.value)
                    is com.junkfood.seal.download.YtDlpOption.Multi -> listOf(opt.name) + opt.values
                }
            }
        val planLine = "plan options => ${flattenedPlan.joinToString(" ")}".also { Log.d(TAG, it) }
        val built = request.buildCommand().joinToString(" ")
        val reqLine = "request command => $built".also { Log.d(TAG, it) }
        appendPlanLog(listOf(planLine, reqLine))
    }

    /** Write plan debug info to a file so devices without logcat/adb can still export it. */
    private fun appendPlanLog(lines: List<String>) {
        runCatching {
            val logDir = context.getExternalFilesDir("logs") ?: return
            logDir.mkdirs()
            val logFile = File(logDir, "download-plan.txt")
            val header = "==== ${System.currentTimeMillis()} ===="
            logFile.appendText((listOf(header) + lines + "").joinToString(separator = "\n"))
        }.onFailure { Log.w(TAG, "plan log write failed", it) }
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
            YoutubeDlRequestAdapter.buildRequestFromPlan(
                    plan = plan,
                    videoInfo = videoInfo,
                    preferences = downloadPreferences,
                    playlistUrl = playlistUrl,
                )
                .getOrElse { return Result.failure(it) }

        if (downloadPreferences.debug || BuildConfig.DEBUG) {
            // Log in debug builds even if the user toggle is off, to ease regression checks.
            logPlanDebug(plan, request)
        }

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

        val plan = buildCustomCommandPlan(urlList, preferences, preferences.commandDirectory)
        val request = YoutubeDLRequest(urlList).apply {
            YoutubeDlRequestAdapter.applyCustomCommandPlan(this, plan, preferences)
            addOption(
                "--config-locations",
                FileUtil.writeContentToFile(template.template, context.getConfigFile())
                    .absolutePath,
            )
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
            val plan = buildCustomCommandPlan(urlList, downloadPreferences, commandDirectory)
            val request =
                YoutubeDLRequest(urlList).apply {
                    YoutubeDlRequestAdapter.applyCustomCommandPlan(
                        this,
                        plan,
                        downloadPreferences,
                    )
                    addOption(
                        "--config-locations",
                        FileUtil.writeContentToFile(template.template, context.getConfigFile())
                            .absolutePath,
                    )
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
