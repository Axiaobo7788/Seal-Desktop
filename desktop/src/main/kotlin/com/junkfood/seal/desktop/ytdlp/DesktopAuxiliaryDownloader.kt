package com.junkfood.seal.desktop.ytdlp

import com.junkfood.seal.desktop.i18n.AndroidStrings
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Locale
import java.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object DesktopAuxiliaryDownloader {
    const val YT_DLP_CHANNEL_STABLE = 0
    const val YT_DLP_CHANNEL_NIGHTLY = 1

    private val httpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(20))
        .build()

    suspend fun downloadYtDlpBinary(
        isWin: Boolean,
        isMac: Boolean,
        onLog: (String) -> Unit,
        ytDlpUpdateChannel: Int = YT_DLP_CHANNEL_STABLE,
    ): Boolean = withContext(Dispatchers.IO) {
        val dir = auxiliaryDirectory(isWin, isMac)
        if (!Files.exists(dir)) {
            Files.createDirectories(dir)
        }

        try {
            downloadYtDlpTo(dir, isWin, isMac, ytDlpUpdateChannel, onLog)
            onLog(AndroidStrings.format("desktop_dependency_update_complete", dir))
            return@withContext true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onLog(AndroidStrings.format("desktop_dependency_update_failed", e.message.orEmpty()))
            return@withContext false
        }
    }

    internal suspend fun downloadPortableDependencies(
        isWin: Boolean,
        isMac: Boolean,
        onLog: (String) -> Unit,
        ytDlpUpdateChannel: Int = YT_DLP_CHANNEL_STABLE,
        selection: PortableDependencySelection = PortableDependencySelection.All,
    ): Boolean = withContext(Dispatchers.IO) {
        val dir = auxiliaryDirectory(isWin, isMac)

        if (selection.isEmpty) {
            onLog(AndroidStrings.get("desktop_dependency_system_skip"))
            return@withContext true
        }

        if (!Files.exists(dir)) {
            Files.createDirectories(dir)
        }

        val ffmpegDownloads = if (selection.ffmpeg) getFfmpegDownloads(isWin, isMac) else emptyList()

        try {
            if (selection.ytDlp) {
                downloadYtDlpTo(dir, isWin, isMac, ytDlpUpdateChannel, onLog)
            } else {
                onLog(AndroidStrings.get("desktop_dependency_ytdlp_skip"))
            }

            for (download in ffmpegDownloads) {
                onLog(
                    AndroidStrings.format(
                        "desktop_dependency_downloading_source",
                        download.tools.joinToString(),
                        download.source,
                    ),
                )
                val archivePath = dir.resolve(download.archiveName)
                try {
                    retryDependencyTransfer(onRetry = { attempt ->
                        onLog(AndroidStrings.format("desktop_dependency_transfer_retry", attempt, 3))
                    }) {
                        downloadFile(download.url, archivePath)
                        onLog(AndroidStrings.format("desktop_dependency_downloaded_extracting", download.tools.joinToString()))
                        if (download.archiveName.endsWith(".zip")) {
                            extractZipAndMoveTools(archivePath, dir, isWin, download.tools)
                        } else if (download.archiveName.endsWith(".tar.xz")) {
                            extractTarXzAndMoveTools(archivePath, dir, download.tools)
                        } else {
                            error(AndroidStrings.format("desktop_dependency_unsupported_archive", download.archiveName))
                        }
                    }
                } finally {
                    Files.deleteIfExists(archivePath)
                }
            }

            DesktopDependencyResolver.invalidateHealth()
            onLog(AndroidStrings.format("desktop_dependency_setup_complete", dir))
            return@withContext true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onLog(AndroidStrings.format("desktop_dependency_setup_error", e.message.orEmpty()))
            return@withContext false
        }
    }

    internal fun auxiliaryDirectory(isWin: Boolean, isMac: Boolean): Path {
        val osName = System.getProperty("os.name").lowercase()
        val actualIsWindows = osName.contains("win")
        val actualIsMac = osName.contains("mac") || osName.contains("darwin")
        if (isWin == actualIsWindows && isMac == actualIsMac) {
            return DesktopDependencyPaths.appPrivateDirectory()
        }

        return DesktopDependencyPaths.defaultAppPrivateDirectory(
            isWindows = isWin,
            isMac = isMac,
            userHome = System.getProperty("user.home"),
            xdgDataHome = System.getenv("XDG_DATA_HOME"),
            localAppData = System.getenv("LOCALAPPDATA"),
        )
    }

    private suspend fun downloadYtDlpTo(
        dir: Path,
        isWin: Boolean,
        isMac: Boolean,
        ytDlpUpdateChannel: Int,
        onLog: (String) -> Unit,
    ) {
        val channelName =
            if (ytDlpUpdateChannel == YT_DLP_CHANNEL_NIGHTLY) {
                AndroidStrings.get("nightly_channel")
            } else {
                AndroidStrings.get("stable_channel")
            }
        onLog(AndroidStrings.format("desktop_dependency_downloading_ytdlp", channelName))

        val ytDlpUrl = getYtDlpUrl(isWin, isMac, ytDlpUpdateChannel)
        val ytDlpFileName = if (isWin) "yt-dlp.exe" else "yt-dlp"
        val ytDlpPath = dir.resolve(ytDlpFileName)
        retryDependencyTransfer { downloadFile(ytDlpUrl, ytDlpPath) }

        if (!isWin) {
            ytDlpPath.toFile().setExecutable(true, false)
        }
        DesktopDependencyResolver.invalidateHealth(ytDlpPath)
        onLog(AndroidStrings.get("desktop_dependency_ytdlp_ready"))
    }

    private fun getYtDlpUrl(isWin: Boolean, isMac: Boolean, ytDlpUpdateChannel: Int): String {
        val arch = System.getProperty("os.arch").lowercase()
        val isArm = arch.contains("aarch64") || arch.contains("arm64")
        val isX86 = arch == "x86" || arch.contains("i386") || arch.contains("i686")

        val binaryName = when {
            isWin && isArm -> "yt-dlp_arm64.exe"
            isWin && isX86 -> "yt-dlp_x86.exe"
            isWin -> "yt-dlp.exe"
            isMac -> "yt-dlp_macos"
            !isMac && !isWin && isArm -> "yt-dlp_linux_aarch64"
            else -> "yt-dlp_linux"
        }
        val repository =
            if (ytDlpUpdateChannel == YT_DLP_CHANNEL_NIGHTLY) {
                "yt-dlp-nightly-builds"
            } else {
                "yt-dlp"
            }
        return "https://github.com/yt-dlp/$repository/releases/latest/download/$binaryName"
    }

    internal fun getFfmpegDownloads(
        isWin: Boolean,
        isMac: Boolean,
        arch: String = System.getProperty("os.arch"),
    ): List<PortableDependencyDownload> {
        val normalizedArch = arch.lowercase(Locale.ROOT)
        val isArm = normalizedArch.contains("aarch64") || normalizedArch.contains("arm64")
        val isX86 = normalizedArch == "x86" || normalizedArch.contains("i386") || normalizedArch.contains("i686")
        if (isMac) {
            val suffix = if (isArm) "81arm" else "80intel"
            return listOf(
                PortableDependencyDownload(
                    url = "https://www.osxexperts.net/ffmpeg$suffix.zip",
                    archiveName = "ffmpeg-$suffix.zip",
                    tools = setOf("ffmpeg"),
                    source = "OSXExperts",
                ),
                PortableDependencyDownload(
                    url = "https://www.osxexperts.net/ffprobe$suffix.zip",
                    archiveName = "ffprobe-$suffix.zip",
                    tools = setOf("ffprobe"),
                    source = "OSXExperts",
                ),
            )
        }

        val url = when {
            isWin && isArm -> "https://github.com/yt-dlp/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-winarm64-gpl.zip"
            isWin && isX86 -> "https://github.com/yt-dlp/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win32-gpl.zip"
            isWin -> "https://github.com/yt-dlp/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl.zip"
            isArm -> "https://github.com/yt-dlp/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-linuxarm64-gpl.tar.xz"
            else -> "https://github.com/yt-dlp/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-linux64-gpl.tar.xz"
        }
        return listOf(
            PortableDependencyDownload(
                url = url,
                archiveName = url.substringAfterLast('/'),
                tools = setOf("ffmpeg", "ffprobe"),
                source = "yt-dlp/FFmpeg-Builds",
            )
        )
    }

    private fun downloadFile(url: String, target: Path) {
        val partialTarget = target.resolveSibling("${target.fileName}.part")
        Files.deleteIfExists(partialTarget)
        val request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofMinutes(3)).GET().build()
        try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(partialTarget))
            if (response.statusCode() !in 200..299) {
                throw IOException(
                    AndroidStrings.format("desktop_dependency_http_error", response.statusCode(), url),
                )
            }
            val expectedSize = response.headers().firstValueAsLong("Content-Length")
            if (Files.size(partialTarget) == 0L ||
                (expectedSize.isPresent && Files.size(partialTarget) != expectedSize.asLong)
            ) {
                throw IOException(AndroidStrings.get("desktop_dependency_incomplete_transfer"))
            }
            Files.move(partialTarget, target, StandardCopyOption.REPLACE_EXISTING)
        } catch (error: Exception) {
            Files.deleteIfExists(partialTarget)
            throw error
        }
    }

    internal fun extractZipAndMoveTools(
        zipFile: Path,
        targetDir: Path,
        isWin: Boolean,
        tools: Set<String>,
    ) {
        installDependencyZip(zipFile, targetDir, isWin, tools)
    }

    private fun extractTarXzAndMoveTools(tarFile: Path, targetDir: Path, tools: Set<String>) {
        val tmpDir = Files.createTempDirectory(targetDir, "ffmpeg_tmp")
        try {
            val process =
                ProcessBuilder("tar", "-xf", tarFile.toAbsolutePath().toString(), "-C", tmpDir.toAbsolutePath().toString())
                    .start()
            if (process.waitFor() != 0) {
                throw RuntimeException(AndroidStrings.get("desktop_dependency_tar_extract_failed"))
            }

            val remainingTools = tools.toMutableSet()
            tmpDir.toFile().walkTopDown().forEach { file ->
                if (file.isFile && file.name in remainingTools) {
                    val outPath = targetDir.resolve(file.name)
                    Files.move(file.toPath(), outPath, StandardCopyOption.REPLACE_EXISTING)
                    outPath.toFile().setExecutable(true, false)
                    remainingTools.remove(file.name)
                }
            }
            check(remainingTools.isEmpty()) {
                AndroidStrings.format("desktop_dependency_archive_missing_tools", remainingTools.joinToString())
            }
        } finally {
            tmpDir.toFile().deleteRecursively()
        }
    }

}

internal suspend fun retryDependencyTransfer(
    retryDelayMillis: Long = 1_000,
    onRetry: (Int) -> Unit = {},
    transfer: () -> Unit,
) {
    repeat(3) { attempt ->
        try {
            transfer()
            return
        } catch (error: IOException) {
            if (attempt == 2) throw error
            onRetry(attempt + 2)
            delay(retryDelayMillis)
        }
    }
}

internal data class PortableDependencySelection(
    val ytDlp: Boolean,
    val ffmpeg: Boolean,
) {
    val isEmpty: Boolean
        get() = !ytDlp && !ffmpeg

    companion object {
        val All = PortableDependencySelection(ytDlp = true, ffmpeg = true)
    }
}

internal data class PortableDependencyDownload(
    val url: String,
    val archiveName: String,
    val tools: Set<String>,
    val source: String,
)
