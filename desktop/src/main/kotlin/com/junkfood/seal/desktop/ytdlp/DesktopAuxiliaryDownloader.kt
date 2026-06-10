package com.junkfood.seal.desktop.ytdlp

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DesktopAuxiliaryDownloader {

    private val httpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    suspend fun downloadPortableDependencies(
        isWin: Boolean,
        isMac: Boolean,
        onLog: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val userHome = System.getProperty("user.home")
        val dir = when {
            isWin -> Path.of(System.getenv("LOCALAPPDATA") ?: "$userHome\\AppData\\Local", "Seal", "bin")
            isMac -> Path.of(userHome, "Library", "Application Support", "Seal", "bin")
            else -> Path.of(userHome, ".local", "bin")
        }

        if (!Files.exists(dir)) {
            Files.createDirectories(dir)
        }

        val ytDlpUrl = getYtDlpUrl(isWin, isMac)
        val ffmpegArchiveUrl = getFfmpegUrl(isWin, isMac)

        try {
            onLog("正在下载 yt-dlp (来源: GitHub Releases)...")
            val ytDlpFileName = if (isWin) "yt-dlp.exe" else "yt-dlp"
            val ytDlpPath = dir.resolve(ytDlpFileName)
            downloadFile(ytDlpUrl, ytDlpPath)
            
            if (!isWin) {
                ytDlpPath.toFile().setExecutable(true, false)
            }
            onLog("✅ yt-dlp 下载完成并配置。")

            onLog("正在下载 ffmpeg (来源: yt-dlp/FFmpeg-Builds)...")
            val archiveName = ffmpegArchiveUrl.substringAfterLast('/')
            val archivePath = dir.resolve(archiveName)
            downloadFile(ffmpegArchiveUrl, archivePath)
            onLog("✅ ffmpeg 下载完成，准备解压...")

            if (archiveName.endsWith(".zip")) {
                extractZipAndMoveFfmpeg(archivePath, dir, isWin)
            } else if (archiveName.endsWith(".tar.xz")) {
                extractTarXzAndMoveFfmpeg(archivePath, dir)
            }
            onLog("正在清理临时文件...")
            Files.deleteIfExists(archivePath)

            onLog("🎉 环境依赖全自动配置成功！\n文件存放在私人便携目录:\n$dir")
            return@withContext true
        } catch (e: Exception) {
            onLog("❌ 发生异常: ${e.message}")
            return@withContext false
        }
    }

    private fun getYtDlpUrl(isWin: Boolean, isMac: Boolean): String {
        val arch = System.getProperty("os.arch").lowercase()
        val isArm = arch.contains("aarch64") || arch.contains("arm64")
        val isX86 = arch.contains("x86") || arch.contains("i386") || arch.contains("i686")

        val binaryName = when {
            isWin && isArm -> "yt-dlp_arm64.exe"
            isWin && isX86 -> "yt-dlp_x86.exe"
            isWin -> "yt-dlp.exe"
            isMac -> "yt-dlp_macos"
            !isMac && !isWin && isArm -> "yt-dlp_linux_aarch64"
            else -> "yt-dlp_linux"
        }
        return "https://github.com/yt-dlp/yt-dlp/releases/latest/download/$binaryName"
    }

    private fun getFfmpegUrl(isWin: Boolean, isMac: Boolean): String {
        val arch = System.getProperty("os.arch").lowercase()
        val isArm = arch.contains("aarch64") || arch.contains("arm64")
        return when {
            isWin -> "https://github.com/yt-dlp/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl.zip"
            isMac -> "https://github.com/yt-dlp/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-macos64-gpl.zip"
            isArm -> "https://github.com/yt-dlp/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-linuxarm64-gpl.tar.xz"
            else -> "https://github.com/yt-dlp/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-linux64-gpl.tar.xz"
        }
    }

    private fun downloadFile(url: String, target: Path) {
        val request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(target))
        if (response.statusCode() !in 200..299) {
            throw RuntimeException("下载失败 HTTP ${response.statusCode()}: $url")
        }
    }

    private fun extractZipAndMoveFfmpeg(zipFile: Path, targetDir: Path, isWin: Boolean) {
        ZipInputStream(Files.newInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                if (!entry.isDirectory && name.contains("/bin/")) {
                    val fileName = name.substringAfterLast('/')
                    if (fileName.startsWith("ffmpeg") || fileName.startsWith("ffprobe")) {
                        val outPath = targetDir.resolve(fileName)
                        Files.copy(zis, outPath, StandardCopyOption.REPLACE_EXISTING)
                        if (!isWin) outPath.toFile().setExecutable(true, false)
                    }
                }
                entry = zis.nextEntry
            }
        }
    }

    private fun extractTarXzAndMoveFfmpeg(tarFile: Path, targetDir: Path) {
        val tmpDir = Files.createTempDirectory(targetDir, "ffmpeg_tmp")
        val pb = ProcessBuilder("tar", "-xf", tarFile.toAbsolutePath().toString(), "-C", tmpDir.toAbsolutePath().toString())
        val p = pb.start()
        val exit = p.waitFor()
        if (exit != 0) {
            throw RuntimeException("tar解压失败，请确保系统已安装 tar 和 xz-utils")
        }
        
        tmpDir.toFile().walkTopDown().forEach { file ->
            if (file.isFile && (file.name == "ffmpeg" || file.name == "ffprobe")) {
                val outPath = targetDir.resolve(file.name)
                Files.move(file.toPath(), outPath, StandardCopyOption.REPLACE_EXISTING)
                outPath.toFile().setExecutable(true, false)
            }
        }
        tmpDir.toFile().deleteRecursively()
    }
}
