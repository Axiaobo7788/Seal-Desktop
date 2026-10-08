package com.junkfood.seal.desktop.ytdlp

import com.junkfood.seal.desktop.i18n.AndroidStrings
import com.junkfood.seal.desktop.settings.EnvPrefAuto
import com.junkfood.seal.desktop.settings.EnvPrefBundled
import com.junkfood.seal.desktop.settings.EnvPrefSystem
import com.junkfood.seal.desktop.storage.DesktopSqliteStorage
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.exists
import kotlin.io.path.isExecutable
import kotlin.io.path.setPosixFilePermissions

enum class DesktopDependencySource {
    AppPrivate,
    Packaged,
    SystemPath,
}

data class ResolvedDesktopDependency(
    val name: String,
    val path: Path,
    val source: DesktopDependencySource,
    val health: DesktopDependencyHealth =
        DesktopDependencyHealth(status = DesktopDependencyHealthStatus.Healthy),
)

data class DesktopDependencyResolution(
    val environmentPreference: Int,
    val ytDlp: ResolvedDesktopDependency?,
    val ffmpeg: ResolvedDesktopDependency?,
    val aria2c: ResolvedDesktopDependency?,
) {
    val isComplete: Boolean
        get() = ytDlp?.health?.isHealthy == true && ffmpeg?.health?.isHealthy == true

    val missingNames: List<String>
        get() =
            buildList {
                if (ytDlp == null) add("yt-dlp")
                if (ffmpeg == null) add("ffmpeg")
            }

    val brokenNames: List<String>
        get() =
            buildList {
                if (ytDlp != null && !ytDlp.health.isHealthy) add("yt-dlp")
                if (ffmpeg != null && !ffmpeg.health.isHealthy) add("ffmpeg")
            }

    internal fun missingPortableDependencies(): PortableDependencySelection =
        PortableDependencySelection(
            ytDlp = ytDlp?.health?.isHealthy != true,
            ffmpeg = ffmpeg?.health?.isHealthy != true,
        )
}

object DesktopDependencyResolver {
    private val platform: DependencyPlatform = detectDependencyPlatform()
    private val healthProbe = DesktopDependencyHealthProbe(isWindows = platform.isWindows)

    fun defaultEnvironmentPreference(): Int =
        DesktopSqliteStorage.readAppSettings()?.environmentPreference ?: EnvPrefAuto

    /** Performs process probes and must be called from a worker thread when used by UI code. */
    fun resolve(environmentPreference: Int = defaultEnvironmentPreference()): DesktopDependencyResolution {
        val ytDlp = resolveYtDlp(environmentPreference)
        val ffmpeg = resolveFfmpeg(environmentPreference, ytDlp)
        val aria2c = resolveAria2c(environmentPreference)
        return DesktopDependencyResolution(
            environmentPreference = environmentPreference,
            ytDlp = ytDlp,
            ffmpeg = ffmpeg,
            aria2c = aria2c,
        )
    }

    fun invalidateHealth(path: Path? = null) {
        healthProbe.invalidate(path)
    }

    fun requireComplete(environmentPreference: Int = defaultEnvironmentPreference()): DesktopDependencyResolution {
        val resolution = resolve(environmentPreference)
        if (resolution.isComplete) return resolution

        val message =
            if (resolution.missingNames.isNotEmpty()) {
                AndroidStrings.format(
                    "desktop_dependency_required_missing",
                    resolution.missingNames.joinToString(),
                )
            } else {
                AndroidStrings.format(
                    "desktop_dependency_required_broken",
                    resolution.brokenNames.joinToString(),
                )
            }
        throw EnvironmentMissingException(message)
    }

    private fun resolveYtDlp(environmentPreference: Int): ResolvedDesktopDependency? =
        when (environmentPreference) {
            EnvPrefBundled -> findPrivateBinary("yt-dlp", platform.privateYtDlpNames)
            EnvPrefSystem -> findSystemBinary("yt-dlp", platform.systemYtDlpName)
            else -> chooseAutoDependency(
                findPrivateBinary("yt-dlp", platform.privateYtDlpNames),
                findSystemBinary("yt-dlp", platform.systemYtDlpName),
            )
        }

    private fun resolveFfmpeg(
        environmentPreference: Int,
        ytDlp: ResolvedDesktopDependency?,
    ): ResolvedDesktopDependency? =
        when (environmentPreference) {
            EnvPrefBundled -> findPrivateFfmpeg(ytDlp)
            EnvPrefSystem -> findSystemBinary("ffmpeg", platform.ffmpegName)
            else -> chooseAutoDependency(
                findPrivateFfmpeg(ytDlp),
                findSystemBinary("ffmpeg", platform.ffmpegName),
            )
        }

    private fun findPrivateFfmpeg(ytDlp: ResolvedDesktopDependency?): ResolvedDesktopDependency? {
        val preferredRoot =
            ytDlp
                ?.takeIf { it.source != DesktopDependencySource.SystemPath }
                ?.path
                ?.parent

        return findPrivateBinary(
            name = "ffmpeg",
            fileNames = listOf(platform.ffmpegName),
            preferredRoot = preferredRoot,
        )
    }

    private fun resolveAria2c(environmentPreference: Int): ResolvedDesktopDependency? =
        when (environmentPreference) {
            EnvPrefBundled -> findPrivateBinary("aria2c", listOf(platform.aria2cName))
            EnvPrefSystem -> findSystemBinary("aria2c", platform.aria2cName)
            else -> chooseAutoDependency(
                findPrivateBinary("aria2c", listOf(platform.aria2cName)),
                findSystemBinary("aria2c", platform.aria2cName),
            )
        }

    private fun findPrivateBinary(
        name: String,
        fileNames: List<String>,
        preferredRoot: Path? = null,
    ): ResolvedDesktopDependency? {
        val roots: List<PrivateDependencyRoot> =
            buildList {
                preferredRoot?.let { root ->
                    add(
                        PrivateDependencyRoot(
                            path = root,
                            source =
                                if (root.toAbsolutePath().normalize() ==
                                    DesktopDependencyPaths.appPrivateDirectory().toAbsolutePath().normalize()
                                ) {
                                    DesktopDependencySource.AppPrivate
                                } else {
                                    DesktopDependencySource.Packaged
                                },
                        ),
                    )
                }
                addAll(privateRoots())
            }.distinctBy { it.path.toAbsolutePath().normalize().toString() }

        var firstBroken: ResolvedDesktopDependency? = null
        for (root in roots) {
            for (fileName in fileNames) {
                val candidate = root.path.resolve(fileName)
                if (candidate.exists()) {
                    if (root.source == DesktopDependencySource.AppPrivate) ensureExecutable(candidate)
                    val dependency =
                        ResolvedDesktopDependency(
                            name = name,
                            path = candidate,
                            source = root.source,
                            health = healthProbe.probe(name, candidate),
                        )
                    if (dependency.health.isHealthy) return dependency
                    if (firstBroken == null) firstBroken = dependency
                }
            }
        }
        return firstBroken
    }

    private fun findSystemBinary(name: String, fileName: String): ResolvedDesktopDependency? {
        var firstBroken: ResolvedDesktopDependency? = null
        for (candidate in DesktopSystemPaths.findExecutableCandidates(fileName)) {
            val dependency =
                ResolvedDesktopDependency(
                    name = name,
                    path = candidate,
                    source = DesktopDependencySource.SystemPath,
                    health = healthProbe.probe(name, candidate),
                )
            if (dependency.health.isHealthy) return dependency
            if (firstBroken == null) firstBroken = dependency
        }
        return firstBroken
    }

    private fun privateRoots(): List<PrivateDependencyRoot> =
        buildList {
            add(
                PrivateDependencyRoot(
                    DesktopDependencyPaths.appPrivateDirectory(),
                    DesktopDependencySource.AppPrivate,
                ),
            )
            runCatching {
                System.getProperty("compose.application.resources.dir")
                    ?.takeIf { it.isNotBlank() }
                    ?.let { add(PrivateDependencyRoot(Path.of(it), DesktopDependencySource.Packaged)) }
            }

            runCatching {
                val location = DesktopDependencyResolver::class.java.protectionDomain.codeSource.location
                val codePath = Path.of(location.toURI())
                val baseDir = if (Files.isDirectory(codePath)) codePath else codePath.parent
                if (baseDir != null) {
                    add(PrivateDependencyRoot(baseDir, DesktopDependencySource.Packaged))
                    baseDir.parent?.let { add(PrivateDependencyRoot(it, DesktopDependencySource.Packaged)) }
                    add(PrivateDependencyRoot(baseDir.resolve("bin"), DesktopDependencySource.Packaged))
                    baseDir.parent?.resolve("bin")?.let {
                        add(PrivateDependencyRoot(it, DesktopDependencySource.Packaged))
                    }
                }
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

private data class PrivateDependencyRoot(
    val path: Path,
    val source: DesktopDependencySource,
)

internal fun chooseAutoDependency(
    privateDependency: ResolvedDesktopDependency?,
    systemDependency: ResolvedDesktopDependency?,
): ResolvedDesktopDependency? =
    privateDependency?.takeIf { it.health.isHealthy }
        ?: systemDependency?.takeIf { it.health.isHealthy }
        ?: privateDependency
        ?: systemDependency

private data class DependencyPlatform(
    val privateYtDlpNames: List<String>,
    val systemYtDlpName: String,
    val ffmpegName: String,
    val aria2cName: String,
    val isWindows: Boolean,
)

private fun detectDependencyPlatform(): DependencyPlatform {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    val isArm = arch.contains("aarch64") || arch.contains("arm64")
    val isX86 = arch == "x86" || arch.contains("i386") || arch.contains("i686")
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
        aria2cName = if (isWin) "aria2c.exe" else "aria2c",
        isWindows = isWin,
    )
}
