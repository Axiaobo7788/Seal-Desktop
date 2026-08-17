package com.junkfood.seal.desktop.ytdlp

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Locale
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DesktopAuxiliaryDownloader {
    const val YT_DLP_CHANNEL_STABLE = 0
    const val YT_DLP_CHANNEL_NIGHTLY = 1

    private val httpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
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
            onLog("yt-dlp 更新完成。\n文件存放在私人便携目录:\n$dir")
            return@withContext true
        } catch (e: Exception) {
            onLog("yt-dlp 更新失败: ${e.message}")
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
            onLog("已使用检测到的系统依赖，无需下载 Seal 自管副本。")
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
                onLog("已检测到可用的 yt-dlp，跳过自管副本下载。")
            }

            for (download in ffmpegDownloads) {
                onLog("正在下载 ${download.tools.joinToString()} (来源: ${download.source})...")
                val archivePath = dir.resolve(download.archiveName)
                downloadFile(download.url, archivePath)
                try {
                    onLog("${download.tools.joinToString()} 下载完成，准备解压...")

                    if (download.archiveName.endsWith(".zip")) {
                        extractZipAndMoveTools(archivePath, dir, isWin, download.tools)
                    } else if (download.archiveName.endsWith(".tar.xz")) {
                        extractTarXzAndMoveTools(archivePath, dir, download.tools)
                    } else {
                        error("不支持的依赖压缩格式: ${download.archiveName}")
                    }
                } finally {
                    Files.deleteIfExists(archivePath)
                }
            }

            onLog("🎉 环境依赖全自动配置成功！\n文件存放在私人便携目录:\n$dir")
            return@withContext true
        } catch (e: Exception) {
            onLog("❌ 发生异常: ${e.message}")
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

    private fun downloadYtDlpTo(
        dir: Path,
        isWin: Boolean,
        isMac: Boolean,
        ytDlpUpdateChannel: Int,
        onLog: (String) -> Unit,
    ) {
        val channelName = if (ytDlpUpdateChannel == YT_DLP_CHANNEL_NIGHTLY) "Nightly" else "Stable"
        onLog("正在下载 yt-dlp ($channelName, 来源: GitHub Releases)...")

        val ytDlpUrl = getYtDlpUrl(isWin, isMac, ytDlpUpdateChannel)
        val ytDlpFileName = if (isWin) "yt-dlp.exe" else "yt-dlp"
        val ytDlpPath = dir.resolve(ytDlpFileName)
        downloadFile(ytDlpUrl, ytDlpPath)

        if (!isWin) {
            ytDlpPath.toFile().setExecutable(true, false)
        }
        onLog("yt-dlp 下载完成并配置。")
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
        val request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build()
        try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(partialTarget))
            if (response.statusCode() !in 200..299) {
                throw RuntimeException("下载失败 HTTP ${response.statusCode()}: $url")
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
        val remainingTools = tools.toMutableSet()
        ZipInputStream(Files.newInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val archiveFileName = entry.name.substringAfterLast('/').substringAfterLast('\\')
                    val toolName = archiveFileName.removeSuffix(".exe")
                    if (toolName in remainingTools) {
                        val outputFileName = if (isWin) "$toolName.exe" else toolName
                        val outPath = targetDir.resolve(outputFileName)
                        Files.copy(zis, outPath, StandardCopyOption.REPLACE_EXISTING)
                        if (!isWin) outPath.toFile().setExecutable(true, false)
                        remainingTools.remove(toolName)
                    }
                }
                entry = zis.nextEntry
            }
        }
        check(remainingTools.isEmpty()) {
            "压缩包中缺少工具: ${remainingTools.joinToString()}"
        }
    }

    private fun extractTarXzAndMoveTools(tarFile: Path, targetDir: Path, tools: Set<String>) {
        val tmpDir = Files.createTempDirectory(targetDir, "ffmpeg_tmp")
        try {
            val process =
                ProcessBuilder("tar", "-xf", tarFile.toAbsolutePath().toString(), "-C", tmpDir.toAbsolutePath().toString())
                    .start()
            if (process.waitFor() != 0) {
                throw RuntimeException("tar解压失败，请确保系统已安装 tar 和 xz-utils")
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
                "压缩包中缺少工具: ${remainingTools.joinToString()}"
            }
        } finally {
            tmpDir.toFile().deleteRecursively()
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
