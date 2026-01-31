package com.junkfood.seal.desktop.download

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.junkfood.seal.desktop.download.history.DesktopDownloadHistoryEntry
import com.junkfood.seal.desktop.download.history.DesktopDownloadHistoryStorage
import com.junkfood.seal.desktop.download.history.DesktopHistoryExportType
import com.junkfood.seal.desktop.download.history.DesktopHistoryImportMode
import com.junkfood.seal.desktop.download.history.DesktopHistoryMediaType
import com.junkfood.seal.desktop.download.history.DesktopHistoryPlatform
import com.junkfood.seal.desktop.download.history.decodeHistoryEntries
import com.junkfood.seal.desktop.download.history.decodeHistoryUrls
import com.junkfood.seal.desktop.download.history.encodeHistoryEntries
import com.junkfood.seal.desktop.download.history.encodeHistoryUrls
import com.junkfood.seal.desktop.ytdlp.DesktopYtDlpPaths
import com.junkfood.seal.desktop.ytdlp.DownloadPlanExecutor
import com.junkfood.seal.desktop.ytdlp.YtDlpMetadataFetcher
import com.junkfood.seal.download.buildDownloadPlan
import com.junkfood.seal.ui.download.queue.DownloadQueueItemState
import com.junkfood.seal.ui.download.queue.DownloadQueueFilter
import com.junkfood.seal.ui.download.queue.DownloadQueueMediaType
import com.junkfood.seal.ui.download.queue.DownloadQueueStatus
import com.junkfood.seal.ui.download.queue.DownloadQueueViewMode
import com.junkfood.seal.util.DownloadPreferences
import com.junkfood.seal.util.VideoInfo
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DesktopDownloadController(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val executor: DownloadPlanExecutor = DownloadPlanExecutor(),
    private val metadataFetcher: YtDlpMetadataFetcher = YtDlpMetadataFetcher(),
    private val historyStorage: DesktopDownloadHistoryStorage = DesktopDownloadHistoryStorage(),
) {
    var filter by mutableStateOf(DownloadQueueFilter.All)
    var viewMode by mutableStateOf(DownloadQueueViewMode.Grid)

    val queueItems = mutableStateListOf<DownloadQueueItemState>()
    val logLines = mutableStateListOf<String>()

    var runningItemId by mutableStateOf<String?>(null)
        private set
    var runningProcess by mutableStateOf<DownloadPlanExecutor.RunningProcess?>(null)
        private set

    private val canceledItemIds = ConcurrentHashMap.newKeySet<String>()
    private val requestByItemId = ConcurrentHashMap<String, DesktopDownloadRequest>()

    val historyEntries = mutableStateListOf<DesktopDownloadHistoryEntry>()

    init {
        scope.launch {
            val loaded = historyStorage.load()
            historyEntries.clear()
            historyEntries.addAll(loaded)
        }
    }

    fun appendLog(line: String) {
        logLines.add(line)
        if (logLines.size > 300) {
            repeat(logLines.size - 300) { logLines.removeFirst() }
        }
    }

    fun cancelIfRunning(itemId: String) {
        if (itemId == runningItemId) {
            canceledItemIds.add(itemId)
            runningProcess?.cancel()
            updateQueueItem(itemId) { it.copy(status = DownloadQueueStatus.Canceled, progressText = "") }
        }
    }

    fun deleteQueueItem(itemId: String) {
        cancelIfRunning(itemId)
        queueItems.removeAll { it.id == itemId }
        requestByItemId.remove(itemId)
    }

    fun deleteHistoryEntry(entryId: String) {
        historyEntries.removeAll { it.id == entryId }
        scope.launch { historyStorage.save(historyEntries.toList()) }
    }

    fun exportHistoryText(type: DesktopHistoryExportType): String =
        when (type) {
            DesktopHistoryExportType.DownloadHistory -> encodeHistoryEntries(historyEntries.toList())
            DesktopHistoryExportType.UrlList -> encodeHistoryUrls(historyEntries.toList())
        }

    fun exportHistoryToFile(
        target: Path,
        type: DesktopHistoryExportType,
        onComplete: (Result<Unit>) -> Unit = {},
    ) {
        scope.launch {
            val result: Result<Unit> = runCatching {
                when (type) {
                    DesktopHistoryExportType.DownloadHistory -> historyStorage.exportTo(target, historyEntries.toList())
                    DesktopHistoryExportType.UrlList ->
                        withContext(Dispatchers.IO) {
                            target.parent?.let { Files.createDirectories(it) }
                            Files.writeString(target, encodeHistoryUrls(historyEntries.toList()))
                        }
                }
                Unit
            }
            onComplete(result)
        }
    }

    fun importHistoryText(
        text: String,
        mode: DesktopHistoryImportMode = DesktopHistoryImportMode.Merge,
    ): Int {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return 0

        val importedEntries =
            runCatching { decodeHistoryEntries(trimmed) }
                .recoverCatching {
                    val urls = decodeHistoryUrls(trimmed)
                    urls.mapIndexed { index, url ->
                        DesktopDownloadHistoryEntry(
                            id = (System.currentTimeMillis() + index).toString(),
                            title = url,
                            url = url,
                            mediaType = DesktopHistoryMediaType.Video,
                            platform = DesktopHistoryPlatform.Other,
                        )
                    }
                }
                .getOrDefault(emptyList())

        if (importedEntries.isEmpty()) return 0

        val beforeSize = historyEntries.size

        val merged =
            when (mode) {
                DesktopHistoryImportMode.Replace -> importedEntries
                DesktopHistoryImportMode.Merge -> {
                    val existingIds = historyEntries.asSequence().map { it.id }.toHashSet()
                    val existingKeys =
                        historyEntries
                            .asSequence()
                            .map { it.url.takeIf { u -> u.isNotBlank() } ?: it.title }
                            .toHashSet()

                    val newOnes =
                        importedEntries.filter { e ->
                            val idOk = e.id.isNotBlank() && !existingIds.contains(e.id)
                            val key = e.url.takeIf { it.isNotBlank() } ?: e.title
                            val keyOk = key.isNotBlank() && !existingKeys.contains(key)
                            idOk && keyOk
                        }

                    // Imported entries go to the front, same as download completion behavior.
                    newOnes + historyEntries.toList()
                }
            }

        historyEntries.clear()
        historyEntries.addAll(merged.take(500))
        scope.launch { historyStorage.save(historyEntries.toList()) }

        return historyEntries.size - beforeSize
    }

    fun importHistoryFromFile(
        source: Path,
        mode: DesktopHistoryImportMode = DesktopHistoryImportMode.Merge,
        onComplete: (Result<Int>) -> Unit = {},
    ) {
        scope.launch {
            val result =
                runCatching {
                    val text = withContext(Dispatchers.IO) { Files.readString(source) }
                    importHistoryText(text, mode)
                }
            onComplete(result)
        }
    }

    fun startDownload(url: String, type: DesktopDownloadType, basePreferences: DownloadPreferences) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return

        if (runningItemId != null) return

        val itemId = System.currentTimeMillis().toString()
        val mediaType = if (type == DesktopDownloadType.Audio) DownloadQueueMediaType.Audio else DownloadQueueMediaType.Video
        val effectivePreferences = preferencesForType(basePreferences, type)

        queueItems.add(
            DownloadQueueItemState(
                id = itemId,
                title = trimmed,
                url = trimmed,
                mediaType = mediaType,
                status = DownloadQueueStatus.FetchingInfo,
            ),
        )

        val request = DesktopDownloadRequest(trimmed, type, effectivePreferences)
        requestByItemId[itemId] = request
        startDownloadInternal(itemId, request, reuseExisting = true)
    }

    fun resumeIfPossible(itemId: String) {
        if (runningItemId != null) return
        val request = requestByItemId[itemId] ?: return
        startDownloadInternal(itemId, request, reuseExisting = true)
    }

    private fun startDownloadInternal(
        itemId: String,
        request: DesktopDownloadRequest,
        reuseExisting: Boolean,
    ) {
        val trimmed = request.url
        val type = request.type
        val effectivePreferences = request.preferences
        val mediaType = if (type == DesktopDownloadType.Audio) DownloadQueueMediaType.Audio else DownloadQueueMediaType.Video

        if (reuseExisting) {
            updateQueueItem(itemId) {
                it.copy(
                    title = it.title.ifBlank { trimmed },
                    url = trimmed,
                    mediaType = mediaType,
                    status = DownloadQueueStatus.FetchingInfo,
                    progress = null,
                    progressText = "",
                    errorMessage = null,
                    filePath = null,
                )
            }
        }

        scope.launch {
            appendLog("start: $trimmed [${type.name.lowercase(Locale.getDefault())}]")

            val videoInfo =
                try {
                    withContext(Dispatchers.IO) { metadataFetcher.fetch(trimmed) }
                } catch (e: Exception) {
                    appendLog("metadata failed: ${e.message}")
                    VideoInfo(originalUrl = trimmed, webpageUrl = trimmed, title = trimmed)
                }

            updateQueueItem(itemId) {
                it.copy(
                    title = videoInfo.title.ifBlank { trimmed },
                    author = videoInfo.uploader.orEmpty(),
                    thumbnailUrl = videoInfo.thumbnail,
                    status = DownloadQueueStatus.Ready,
                )
            }

            val plan =
                buildDownloadPlan(
                    videoInfo,
                    effectivePreferences,
                    playlistUrl = trimmed,
                    playlistItem = if (type == DesktopDownloadType.Playlist) 0 else 0,
                )

            val config = executor.defaultConfigFor(plan, url = trimmed, paths = DesktopYtDlpPaths)

            try {
                runningItemId = itemId
                updateQueueItem(itemId) { it.copy(status = DownloadQueueStatus.Running, progressText = "") }

                val proc =
                    withContext(Dispatchers.IO) {
                        executor.start(
                            plan,
                            config,
                            onStdout = { line ->
                                appendLog(line)
                                updateProgressFromLine(itemId, line)
                            },
                            onStderr = { line ->
                                appendLog("[err] $line")
                                updateProgressFromLine(itemId, line)
                            },
                        )
                    }

                runningProcess = proc
                val result = withContext(Dispatchers.IO) { proc.waitForResult() }

                val canceled = canceledItemIds.remove(itemId)
                if (canceled) {
                    updateQueueItem(itemId) { it.copy(status = DownloadQueueStatus.Canceled, progressText = "") }
                    return@launch
                }

                val success = result.exitCode == 0
                val filePath = if (success) extractDestinationPath(result.stdout + result.stderr, config.workingDirectory) else null
                val fileSize = filePath?.let { runCatching { Files.size(Path.of(it)) }.getOrNull() }

                updateQueueItem(itemId) {
                    it.copy(
                        status = if (success) DownloadQueueStatus.Completed else DownloadQueueStatus.Error,
                        progress = if (success) 1f else it.progress,
                        progressText = if (success) "" else "Exit code ${result.exitCode}",
                        filePath = filePath,
                        fileSizeApproxBytes = fileSize?.toDouble() ?: it.fileSizeApproxBytes,
                    )
                }

                if (success) {
                    val entry = videoInfo.toHistoryEntry(
                        id = itemId,
                        url = trimmed,
                        mediaType = if (type == DesktopDownloadType.Audio) DesktopHistoryMediaType.Audio else DesktopHistoryMediaType.Video,
                        filePath = filePath,
                        fileSizeBytes = fileSize,
                    )
                    historyEntries.add(0, entry)
                    if (historyEntries.size > 500) {
                        repeat(historyEntries.size - 500) { historyEntries.removeLast() }
                    }
                    historyStorage.save(historyEntries.toList())
                }
            } catch (e: Exception) {
                appendLog("download failed: ${e.message}")
                val canceled = canceledItemIds.remove(itemId)
                if (canceled) {
                    updateQueueItem(itemId) { it.copy(status = DownloadQueueStatus.Canceled, progressText = "") }
                } else {
                    updateQueueItem(itemId) {
                        it.copy(
                            status = DownloadQueueStatus.Error,
                            errorMessage = e.message,
                            progressText = e.message.orEmpty(),
                        )
                    }
                }
            } finally {
                runningProcess = null
                runningItemId = null
            }
        }
    }

    private fun updateQueueItem(itemId: String, transform: (DownloadQueueItemState) -> DownloadQueueItemState) {
        val index = queueItems.indexOfFirst { it.id == itemId }
        if (index >= 0) {
            queueItems[index] = transform(queueItems[index])
        }
    }

    private fun updateProgressFromLine(itemId: String, line: String) {
        val parsed = parseProgressLine(line) ?: return
        updateQueueItem(itemId) {
            it.copy(
                progress = parsed.percent?.let { p -> (p / 100f).coerceIn(0f, 1f) } ?: it.progress,
                progressText = parsed.progressText ?: it.progressText,
                fileSizeApproxBytes = parsed.totalBytes ?: it.fileSizeApproxBytes,
            )
        }
    }
}

private data class DesktopDownloadRequest(
    val url: String,
    val type: DesktopDownloadType,
    val preferences: DownloadPreferences,
)

private data class ProgressSnapshot(
    val percent: Float?,
    val totalBytes: Double?,
    val progressText: String?,
)

private fun parseProgressLine(line: String): ProgressSnapshot? {
    val trimmed = line.trim()
    if (!trimmed.contains("%") && !trimmed.contains("ETA")) return null

    val progressRegex = Regex("""(\d{1,3}(?:\.\d+)?)%\s+of\s+~?([\d.]+)([KMG]i?B)\s+at\s+([\d.]+)([KMG]i?B/s)\s+ETA\s+([0-9:]+)""")
    val match = progressRegex.find(trimmed)
    if (match != null) {
        val percent = match.groupValues[1].toFloatOrNull()
        val total = parseSize(match.groupValues[2], match.groupValues[3])
        val speed = "${match.groupValues[4]}${match.groupValues[5]}"
        val eta = match.groupValues[6]
        return ProgressSnapshot(percent, total, "$speed • ETA $eta")
    }

    val etaRegex = Regex("""ETA\s+([0-9:]+)""")
    val eta = etaRegex.find(trimmed)?.groupValues?.getOrNull(1)
    if (eta != null) {
        return ProgressSnapshot(null, null, "ETA $eta")
    }

    return null
}

private fun parseSize(value: String, unit: String): Double? {
    val number = value.toDoubleOrNull() ?: return null
    val multiplier =
        when (unit) {
            "KiB" -> 1024.0
            "MiB" -> 1024.0 * 1024.0
            "GiB" -> 1024.0 * 1024.0 * 1024.0
            "KB" -> 1000.0
            "MB" -> 1000.0 * 1000.0
            "GB" -> 1000.0 * 1000.0 * 1000.0
            else -> 1.0
        }
    return number * multiplier
}

private fun VideoInfo.toHistoryEntry(
    id: String,
    url: String,
    mediaType: DesktopHistoryMediaType,
    filePath: String?,
    fileSizeBytes: Long?,
): DesktopDownloadHistoryEntry {
    val platform =
        when {
            extractorKey.contains("bili", ignoreCase = true) || webpageUrlDomain?.contains("bilibili", ignoreCase = true) == true -> DesktopHistoryPlatform.BiliBili
            extractorKey.contains("you", ignoreCase = true) || webpageUrlDomain?.contains("youtube", ignoreCase = true) == true || webpageUrlDomain?.contains("youtu", ignoreCase = true) == true -> DesktopHistoryPlatform.YouTube
            else -> DesktopHistoryPlatform.Other
        }
    return DesktopDownloadHistoryEntry(
        id = id,
        title = title.ifBlank { url },
        author = uploader.orEmpty(),
        url = url,
        mediaType = mediaType,
        platform = platform,
        extractor = extractorKey,
        thumbnailUrl = thumbnail,
        filePath = filePath,
        fileSizeBytes = fileSizeBytes,
    )
}

private fun extractDestinationPath(lines: List<String>, workingDir: Path?): String? {
    // yt-dlp commonly prints:
    // [download] Destination: file.ext
    // Destination: file.ext
    val regex = Regex("(?:\\[download\\]\\s*)?Destination:\\s*(.+)$")
    val raw = lines.asReversed().firstNotNullOfOrNull { line -> regex.find(line)?.groupValues?.getOrNull(1)?.trim() }
        ?: return null

    // Some outputs might be relative to working directory.
    val path = Path.of(raw)
    return if (path.isAbsolute) raw else workingDir?.resolve(path)?.toAbsolutePath()?.toString() ?: raw
}
