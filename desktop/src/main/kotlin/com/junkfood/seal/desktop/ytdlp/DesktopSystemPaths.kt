package com.junkfood.seal.desktop.ytdlp

import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isExecutable

internal object DesktopSystemPaths {
    fun executableSearchDirectories(): List<Path> {
        val osName = System.getProperty("os.name").lowercase()
        return executableSearchDirectories(
            isWindows = osName.contains("win"),
            isMac = osName.contains("mac") || osName.contains("darwin"),
            userHome = System.getProperty("user.home"),
            pathEnvironment = System.getenv("PATH"),
            localAppData = System.getenv("LOCALAPPDATA"),
            chocolateyInstall = System.getenv("ChocolateyInstall"),
        )
    }

    fun findExecutable(fileName: String): Path? {
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        return executableSearchDirectories().firstNotNullOfOrNull { directory ->
            val candidate = runCatching { directory.resolve(fileName) }.getOrNull() ?: return@firstNotNullOfOrNull null
            candidate.takeIf { it.exists() && (isWindows || it.isExecutable()) }
        }
    }

    internal fun executableSearchDirectories(
        isWindows: Boolean,
        isMac: Boolean,
        userHome: String,
        pathEnvironment: String?,
        localAppData: String?,
        chocolateyInstall: String?,
    ): List<Path> =
        buildList {
            pathEnvironment
                ?.split(File.pathSeparator)
                ?.filter { it.isNotBlank() }
                ?.mapNotNull { runCatching { Path.of(it) }.getOrNull() }
                ?.let(::addAll)

            when {
                isWindows -> {
                    val localRoot = localAppData?.takeIf { it.isNotBlank() }?.let(Path::of)
                    localRoot?.resolve("Microsoft/WinGet/Links")?.let(::add)
                    localRoot?.resolve("Microsoft/WindowsApps")?.let(::add)
                    add(Path.of(userHome, "scoop", "shims"))
                    chocolateyInstall
                        ?.takeIf { it.isNotBlank() }
                        ?.let(Path::of)
                        ?.resolve("bin")
                        ?.let(::add)
                }
                isMac -> {
                    add(Path.of("/opt/homebrew/bin"))
                    add(Path.of("/usr/local/bin"))
                    add(Path.of("/opt/local/bin"))
                }
                else -> {
                    add(Path.of(userHome, ".local", "bin"))
                    add(Path.of("/usr/local/bin"))
                    add(Path.of("/usr/bin"))
                    add(Path.of("/bin"))
                }
            }
        }.distinctBy { it.toAbsolutePath().normalize().toString() }
}
