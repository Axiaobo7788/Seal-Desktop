package com.junkfood.seal.desktop.ytdlp

import com.junkfood.seal.desktop.settings.EnvPrefAuto
import com.junkfood.seal.desktop.settings.EnvPrefBundled
import com.junkfood.seal.desktop.settings.EnvPrefSystem
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopDependencyPolicyTest {
    @Test
    fun `auto mode leaves system-managed yt-dlp to package manager`() {
        val resolution = resolution(
            preference = EnvPrefAuto,
            ytDlpSource = DesktopDependencySource.SystemPath,
        )

        assertEquals(YtDlpUpdateDisposition.SystemManaged, resolution.ytDlpUpdateDisposition())
    }

    @Test
    fun `system mode never downloads a private yt-dlp when executable is missing`() {
        val resolution = resolution(preference = EnvPrefSystem, ytDlpSource = null)

        assertEquals(YtDlpUpdateDisposition.SystemManaged, resolution.ytDlpUpdateDisposition())
    }

    @Test
    fun `bundled and auto private dependencies remain app-managed`() {
        val bundled = resolution(EnvPrefBundled, DesktopDependencySource.AppPrivate)
        val auto = resolution(EnvPrefAuto, DesktopDependencySource.AppPrivate)

        assertEquals(YtDlpUpdateDisposition.DownloadAppPrivate, bundled.ytDlpUpdateDisposition())
        assertEquals(YtDlpUpdateDisposition.DownloadAppPrivate, auto.ytDlpUpdateDisposition())
    }

    @Test
    fun `portable setup downloads only dependencies missing from selected source`() {
        val resolution = resolution(
            preference = EnvPrefAuto,
            ytDlpSource = DesktopDependencySource.SystemPath,
            ffmpegSource = null,
        )

        assertEquals(
            PortableDependencySelection(ytDlp = false, ffmpeg = true),
            resolution.missingPortableDependencies(),
        )
    }

    @Test
    fun `windows installs missing dependencies with exact winget package ids`() {
        val commands =
            systemDependencyInstallCommands(
                isWindows = true,
                isMac = false,
                resolution = resolution(EnvPrefSystem, ytDlpSource = null, ffmpegSource = null),
            )

        assertEquals(2, commands.size)
        assertEquals(listOf("yt-dlp.yt-dlp", "Gyan.FFmpeg"), commands.map { it[it.indexOf("--id") + 1] })
        assertEquals(true, commands.all { "--exact" in it && "--disable-interactivity" in it })
    }

    @Test
    fun `macOS homebrew command contains only missing formulas`() {
        val commands =
            systemDependencyInstallCommands(
                isWindows = false,
                isMac = true,
                resolution =
                    resolution(
                        EnvPrefSystem,
                        ytDlpSource = DesktopDependencySource.SystemPath,
                        ffmpegSource = null,
                    ),
            )

        assertEquals(listOf(listOf("brew", "install", "ffmpeg")), commands)
    }

    @Test
    fun `linux does not attempt unattended privileged package installation`() {
        val commands =
            systemDependencyInstallCommands(
                isWindows = false,
                isMac = false,
                resolution = resolution(EnvPrefSystem, ytDlpSource = null, ffmpegSource = null),
            )

        assertEquals(emptyList(), commands)
    }

    @Test
    fun `macOS searches Homebrew locations even when GUI PATH is minimal`() {
        val directories =
            DesktopSystemPaths.executableSearchDirectories(
                isWindows = false,
                isMac = true,
                userHome = "/Users/tester",
                pathEnvironment = "/usr/bin:/bin",
                localAppData = null,
                chocolateyInstall = null,
            )

        assertEquals(true, Path.of("/opt/homebrew/bin") in directories)
        assertEquals(true, Path.of("/usr/local/bin") in directories)
    }

    @Test
    fun `windows searches common package manager shim locations`() {
        val directories =
            DesktopSystemPaths.executableSearchDirectories(
                isWindows = true,
                isMac = false,
                userHome = "C:\\Users\\Tester",
                pathEnvironment = "C:\\Windows\\System32",
                localAppData = "C:\\Users\\Tester\\AppData\\Local",
                chocolateyInstall = "C:\\ProgramData\\chocolatey",
            )

        assertEquals(
            true,
            Path.of("C:\\Users\\Tester\\AppData\\Local").resolve("Microsoft/WinGet/Links") in directories,
        )
        assertEquals(true, Path.of("C:\\Users\\Tester", "scoop", "shims") in directories)
    }

    private fun resolution(
        preference: Int,
        ytDlpSource: DesktopDependencySource?,
        ffmpegSource: DesktopDependencySource? = DesktopDependencySource.SystemPath,
    ): DesktopDependencyResolution =
        DesktopDependencyResolution(
            environmentPreference = preference,
            ytDlp = ytDlpSource?.let { dependency("yt-dlp", it) },
            ffmpeg = ffmpegSource?.let { dependency("ffmpeg", it) },
            aria2c = null,
        )

    private fun dependency(name: String, source: DesktopDependencySource): ResolvedDesktopDependency =
        ResolvedDesktopDependency(
            name = name,
            path = Path.of("/test", name),
            source = source,
        )
}
