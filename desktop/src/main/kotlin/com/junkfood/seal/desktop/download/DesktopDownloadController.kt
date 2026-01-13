package com.junkfood.seal.desktop.download

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.junkfood.seal.desktop.download.history.DesktopDownloadHistoryEntry
import com.junkfood.seal.desktop.download.history.DesktopDownloadHistoryStorage
import com.junkfood.seal.desktop.download.history.DesktopHistoryMediaType
import com.junkfood.seal.desktop.download.history.DesktopHistoryPlatform
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
    }

    fun deleteHistoryEntry(entryId: String) {
        historyEntries.removeAll { it.id == entryId }
        scope.launch { historyStorage.save(historyEntries.toList()) }
    }

    fun startDownload(url: String, type: DesktopDownloadType, basePreferences: DownloadPreferences) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return

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

        scope.launch {
            appendLog("start: $trimmed [${type.name.lowercase()}]")

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
                            onStdout = { appendLog(it) },
                            onStderr = { appendLog("[err] $it") },
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
                        progressText = if (success) "" else "Exit code ${result.exitCode}",
                        filePath = filePath,
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
