package com.junkfood.seal.desktop.download.archive

import com.junkfood.seal.desktop.storage.writeTextAtomically
import com.junkfood.seal.desktop.ytdlp.DesktopYtDlpPaths
import com.junkfood.seal.desktop.ytdlp.DownloadPlanExecutor
import com.junkfood.seal.util.VideoInfo
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DesktopDownloadArchiveEntry(
    val extractor: String,
    val mediaId: String,
) {
    val archiveId: String = "$extractor $mediaId"
}

data class DesktopDownloadArchiveSnapshot(
    val path: Path,
    val content: String,
    val entries: List<DesktopDownloadArchiveEntry>,
    val malformedLineCount: Int,
) {
    val count: Int = entries.size
}

sealed interface DesktopDownloadArchivePrecheck {
    data object NotArchived : DesktopDownloadArchivePrecheck

    data class AlreadyArchived(
        val entry: DesktopDownloadArchiveEntry,
    ) : DesktopDownloadArchivePrecheck
}

class DesktopDownloadArchiveException(
    val archivePath: Path,
    cause: Throwable,
) : Exception("Could not access download archive at $archivePath", cause)

/**
 * Owns the editable yt-dlp download archive without interpreting it as download history.
 */
class DesktopDownloadArchiveService(
    private val pathProvider: () -> Path = { DesktopYtDlpPaths.archiveFile() },
) {
    private val lock = Any()

    val path: Path
        get() = pathProvider()

    suspend fun read(): DesktopDownloadArchiveSnapshot =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                val archivePath = path
                try {
                    val content = if (Files.exists(archivePath)) Files.readString(archivePath) else ""
                    parseDesktopDownloadArchive(archivePath, content)
                } catch (error: Exception) {
                    throw DesktopDownloadArchiveException(archivePath, error)
                }
            }
        }

    suspend fun count(): Int = read().count

    suspend fun contains(extractor: String, mediaId: String): Boolean =
        precheck(extractor, mediaId) is DesktopDownloadArchivePrecheck.AlreadyArchived

    suspend fun precheck(extractor: String, mediaId: String): DesktopDownloadArchivePrecheck {
        val normalizedExtractor = extractor.trim()
        val normalizedMediaId = mediaId.trim()
        if (normalizedExtractor.isEmpty() || normalizedMediaId.isEmpty()) {
            return DesktopDownloadArchivePrecheck.NotArchived
        }

        val match =
            read().entries.firstOrNull { entry ->
                entry.extractor == normalizedExtractor && entry.mediaId == normalizedMediaId
            }
        return if (match == null) {
            DesktopDownloadArchivePrecheck.NotArchived
        } else {
            DesktopDownloadArchivePrecheck.AlreadyArchived(match)
        }
    }

    suspend fun clear(): DesktopDownloadArchiveSnapshot = save("")

    suspend fun save(content: String): DesktopDownloadArchiveSnapshot =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                val archivePath = path
                try {
                    writeTextAtomically(archivePath, content)
                    parseDesktopDownloadArchive(archivePath, content)
                } catch (error: Exception) {
                    throw DesktopDownloadArchiveException(archivePath, error)
                }
            }
        }
}

internal fun VideoInfo.downloadArchiveIdentity(): DesktopDownloadArchiveEntry? {
    val archiveExtractor =
        extractor
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: extractorKey.trim().lowercase(Locale.ROOT).takeIf { it.isNotEmpty() }
    val archiveMediaId = id.trim().takeIf { it.isNotEmpty() }
    return if (archiveExtractor == null || archiveMediaId == null) {
        null
    } else {
        DesktopDownloadArchiveEntry(archiveExtractor, archiveMediaId)
    }
}

internal fun DownloadPlanExecutor.ExecutionResult.wasSkippedByDownloadArchive(): Boolean =
    (stdout.asSequence() + stderr.asSequence()).any { line ->
        line.contains("has already been recorded in the archive", ignoreCase = true) ||
            line.contains("already in download archive", ignoreCase = true)
    }

internal fun parseDesktopDownloadArchive(path: Path, content: String): DesktopDownloadArchiveSnapshot {
    val entries = ArrayList<DesktopDownloadArchiveEntry>()
    var malformedLineCount = 0

    content.lineSequence().forEach { rawLine ->
        val line = rawLine.trim()
        if (line.isEmpty()) return@forEach

        val separator = line.indexOfFirst(Char::isWhitespace)
        val extractor = if (separator > 0) line.substring(0, separator).trim() else ""
        val mediaId = if (separator > 0) line.substring(separator).trim() else ""
        if (extractor.isEmpty() || mediaId.isEmpty()) {
            malformedLineCount += 1
        } else {
            entries += DesktopDownloadArchiveEntry(extractor, mediaId)
        }
    }

    return DesktopDownloadArchiveSnapshot(
        path = path,
        content = content,
        entries = entries,
        malformedLineCount = malformedLineCount,
    )
}
