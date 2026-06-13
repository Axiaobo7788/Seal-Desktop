package com.junkfood.seal.desktop.ytdlp

import com.junkfood.seal.desktop.settings.EnvPrefAuto
import com.junkfood.seal.desktop.settings.EnvPrefBundled
import com.junkfood.seal.desktop.settings.EnvPrefSystem
import com.junkfood.seal.desktop.storage.DesktopSqliteStorage
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.exists
import kotlin.io.path.isExecutable
import kotlin.io.path.setPosixFilePermissions

enum class DesktopDependencySource {
    AppPrivate,
    SystemPath,
}

data class ResolvedDesktopDependency(
    val name: String,
    val path: Path,
    val source: DesktopDependencySource,
)

data class DesktopDependencyResolution(
    val environmentPreference: Int,
    val ytDlp: ResolvedDesktopDependency?,
    val ffmpeg: ResolvedDesktopDependency?,
) {
    val isComplete: Boolean
        get() = ytDlp != null && ffmpeg != null

    val missingNames: List<String>
        get() =
            buildList {
                if (ytDlp == null) add("yt-dlp")
                if (ffmpeg == null) add("ffmpeg")
            }
}

object DesktopDependencyResolver {
    private val platform: DependencyPlatform = detectDependencyPlatform()

    fun defaultEnvironmentPreference(): Int =
        DesktopSqliteStorage.readAppSettings()?.environmentPreference ?: EnvPrefAuto

    fun resolve(environmentPreference: Int = defaultEnvironmentPreference()): DesktopDependencyResolution {
        val ytDlp = resolveYtDlp(environmentPreference)
        val ffmpeg = resolveFfmpeg(environmentPreference, ytDlp)
        return DesktopDependencyResolution(
            environmentPreference = environmentPreference,
            ytDlp = ytDlp,
            ffmpeg = ffmpeg,
        )
    }

    fun requireComplete(environmentPreference: Int = defaultEnvironmentPreference()): DesktopDependencyResolution {
        val resolution = resolve(environmentPreference)
        if (resolution.isComplete) return resolution

        throw EnvironmentMissingException(
            "Missing required dependencies: ${resolution.missingNames.joinToString()}. " +
                "Check dependency configuration in Settings > General."
        )
    }

    private fun resolveYtDlp(environmentPreference: Int): ResolvedDesktopDependency? =
        when (environmentPreference) {
            EnvPrefBundled -> findPrivateBinary("yt-dlp", platform.privateYtDlpNames)
            EnvPrefSystem -> findSystemBinary("yt-dlp", platform.systemYtDlpName)
            else ->
                findPrivateBinary("yt-dlp", platform.privateYtDlpNames)
                    ?: findSystemBinary("yt-dlp", platform.systemYtDlpName)
        }

    private fun resolveFfmpeg(
        environmentPreference: Int,
        ytDlp: ResolvedDesktopDependency?,
    ): ResolvedDesktopDependency? =
        when (environmentPreference) {
            EnvPrefBundled -> findPrivateFfmpeg(ytDlp)
            EnvPrefSystem -> findSystemBinary("ffmpeg", platform.ffmpegName)
            else ->
                findPrivateFfmpeg(ytDlp)
                    ?: findSystemBinary("ffmpeg", platform.ffmpegName)
        }

    private fun findPrivateFfmpeg(ytDlp: ResolvedDesktopDependency?): ResolvedDesktopDependency? {
        val preferredRoot =
            ytDlp
                ?.takeIf { it.source == DesktopDependencySource.AppPrivate }
                ?.path
                ?.parent

        return findPrivateBinary(
            name = "ffmpeg",
            fileNames = listOf(platform.ffmpegName),
            preferredRoot = preferredRoot,
        )
    }

    private fun findPrivateBinary(
        name: String,
        fileNames: List<String>,
        preferredRoot: Path? = null,
    ): ResolvedDesktopDependency? {
        val roots =
            buildList {
                preferredRoot?.let(::add)
                addAll(privateRoots())
            }.distinctBy { it.toAbsolutePath().normalize().toString() }

        for (root in roots) {
            for (fileName in fileNames) {
                val candidate = root.resolve(fileName)
                if (candidate.exists()) {
                    ensureExecutable(candidate)
                    if (platform.isWindows || candidate.isExecutable()) {
                        return ResolvedDesktopDependency(name, candidate, DesktopDependencySource.AppPrivate)
                    }
                }
            }
        }
        return null
    }

    private fun findSystemBinary(name: String, fileName: String): ResolvedDesktopDependency? {
        val pathEnv = System.getenv("PATH") ?: return null
        for (dir in pathEnv.split(File.pathSeparator)) {
            if (dir.isBlank()) continue
            val candidate =
                runCatching { Path.of(dir).resolve(fileName) }
                    .getOrNull()
                    ?: continue
            if (candidate.exists() && (platform.isWindows || candidate.isExecutable())) {
                return ResolvedDesktopDependency(name, candidate, DesktopDependencySource.SystemPath)
            }
        }
        return null
    }

    private fun privateRoots(): List<Path> =
        buildList {
            runCatching {
                val cwd = Path.of(System.getProperty("user.dir"))
                add(cwd)
                add(cwd.resolve("bin"))
            }

            runCatching {
                System.getProperty("compose.application.resources.dir")
                    ?.takeIf { it.isNotBlank() }
                    ?.let { add(Path.of(it)) }
            }

            runCatching {
                val location = DesktopDependencyResolver::class.java.protectionDomain.codeSource.location
                val codePath = Path.of(location.toURI())
                val baseDir = if (Files.isDirectory(codePath)) codePath else codePath.parent
                if (baseDir != null) {
                    add(baseDir)
                    baseDir.parent?.let(::add)
                    add(baseDir.resolve("bin"))
                    baseDir.parent?.resolve("bin")?.let(::add)
                }
            }

            add(auxiliaryDirectory())
        }

    private fun auxiliaryDirectory(): Path {
        val osName = System.getProperty("os.name").lowercase()
        val userHome = System.getProperty("user.home")
        return when {
            osName.contains("win") ->
                Path.of(System.getenv("LOCALAPPDATA") ?: "$userHome\\AppData\\Local", "Seal", "bin")
            osName.contains("mac") || osName.contains("darwin") ->
                Path.of(userHome, "Library", "Application Support", "Seal", "bin")
            else ->
                Path.of(userHome, ".local", "bin")
        }
    }

    private fun ensureExecutable(target: Path) {
        if (platform.isWindows || target.isExecutable()) return

        runCatching {
            target.setPosixFilePermissions(
                setOf(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ,
                    PosixFilePermission.OTHERS_EXECUTE,
                )
            )
        }
        if (!target.isExecutable()) {
            target.toFile().setExecutable(true, false)
        }
    }
}

private data class DependencyPlatform(
    val privateYtDlpNames: List<String>,
    val systemYtDlpName: String,
    val ffmpegName: String,
    val isWindows: Boolean,
)

private fun detectDependencyPlatform(): DependencyPlatform {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    val isArm = arch.contains("aarch64") || arch.contains("arm64")
    val isX86 = arch.contains("x86") || arch.contains("i386") || arch.contains("i686")
    val isMac = os.contains("mac") || os.contains("darwin")
    val isWin = os.contains("win")

    val privateYtDlpNames =
        when {
            isWin && isArm -> listOf("yt-dlp_arm64.exe", "yt-dlp.exe")
            isWin && isX86 -> listOf("yt-dlp_x86.exe", "yt-dlp.exe")
            isWin -> listOf("yt-dlp.exe")
            isMac -> listOf("yt-dlp_macos", "yt-dlp")
            isArm -> listOf("yt-dlp_linux_aarch64", "yt-dlp")
            else -> listOf("yt-dlp_linux", "yt-dlp")
        }

    return DependencyPlatform(
        privateYtDlpNames = privateYtDlpNames,
        systemYtDlpName = if (isWin) "yt-dlp.exe" else "yt-dlp",
        ffmpegName = if (isWin) "ffmpeg.exe" else "ffmpeg",
        isWindows = isWin,
    )
}
