package com.junkfood.seal.desktop.ytdlp

import com.junkfood.seal.desktop.settings.EnvPrefBundled
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.io.path.exists
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val osName = System.getProperty("os.name").lowercase(Locale.ROOT)
    val isWindows = osName.contains("win")
    val isMac = osName.contains("mac") || osName.contains("darwin")
    val targetDirectory = DesktopAuxiliaryDownloader.auxiliaryDirectory(isWindows, isMac).toAbsolutePath().normalize()

    require(
        System.getProperty("seal.desktop.auxiliaryDir")?.isNotBlank() == true ||
            System.getenv("SEAL_DESKTOP_AUXILIARY_DIR")?.isNotBlank() == true
    ) {
        "Dependency self-check requires an isolated directory via " +
            "seal.desktop.auxiliaryDir or SEAL_DESKTOP_AUXILIARY_DIR."
    }

    deleteRecursively(targetDirectory)
    Files.createDirectories(targetDirectory)
    println("Dependency smoke directory: $targetDirectory")

    val downloaded =
        DesktopAuxiliaryDownloader.downloadPortableDependencies(
            isWin = isWindows,
            isMac = isMac,
            onLog =(::println),
        )
    check(downloaded) { "Desktop dependency downloader reported failure." }

    val executableSuffix = if (isWindows) ".exe" else ""
    val ytDlp = targetDirectory.resolve("yt-dlp$executableSuffix")
    val ffmpeg = targetDirectory.resolve("ffmpeg$executableSuffix")
    val ffprobe = targetDirectory.resolve("ffprobe$executableSuffix")

    verifyTool(ytDlp, "--version")
    verifyTool(ffmpeg, "-version")
    verifyTool(ffprobe, "-version")

    val resolution = DesktopDependencyResolver.resolve(EnvPrefBundled)
    check(resolution.isComplete) {
        "Bundled dependency resolution failed after download: ${resolution.missingNames.joinToString()}"
    }
    check(resolution.ytDlp?.path?.toAbsolutePath()?.normalize() == ytDlp) {
        "Resolver selected unexpected yt-dlp path: ${resolution.ytDlp?.path}"
    }
    check(resolution.ffmpeg?.path?.toAbsolutePath()?.normalize() == ffmpeg) {
        "Resolver selected unexpected ffmpeg path: ${resolution.ffmpeg?.path}"
    }

    println("Desktop dependency download self-check passed.")
}

private fun verifyTool(path: Path, versionArgument: String) {
    check(path.exists()) { "Downloaded tool is missing: $path" }
    if (!System.getProperty("os.name").lowercase(Locale.ROOT).contains("win")) {
        check(Files.isExecutable(path)) { "Downloaded tool is not executable: $path" }
    }

    val process =
        ProcessBuilder(path.toString(), versionArgument)
            .redirectErrorStream(true)
            .start()
    if (!process.waitFor(45, TimeUnit.SECONDS)) {
        process.destroyForcibly()
        error("Timed out while executing $path $versionArgument")
    }
    val output = process.inputStream.bufferedReader().use { it.readText() }
    check(process.exitValue() == 0) {
        "$path $versionArgument exited with ${process.exitValue()}:\n$output"
    }
    println("${path.fileName}: ${output.lineSequence().firstOrNull().orEmpty()}")
}

private fun deleteRecursively(path: Path) {
    if (!path.exists()) return
    Files.walk(path).use { paths ->
        paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
    }
}
