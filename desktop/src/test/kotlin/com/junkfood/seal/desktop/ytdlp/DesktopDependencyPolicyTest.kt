package com.junkfood.seal.desktop.ytdlp

import com.junkfood.seal.desktop.settings.EnvPrefAuto
import com.junkfood.seal.desktop.settings.EnvPrefBundled
import com.junkfood.seal.desktop.settings.EnvPrefSystem
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
    fun `broken system yt-dlp remains system managed and is never overwritten by updater`() {
        val resolution =
            resolution(
                preference = EnvPrefAuto,
                ytDlpSource = DesktopDependencySource.SystemPath,
                ytDlpHealthy = false,
            )

        assertEquals(YtDlpUpdateDisposition.SystemManaged, resolution.ytDlpUpdateDisposition())
        assertTrue(resolution.missingPortableDependencies().ytDlp)
        assertFalse(resolution.isComplete)
        assertEquals(listOf("yt-dlp"), resolution.brokenNames)
    }

    @Test
    fun `auto mode selects healthy system dependency over broken selfhost dependency`() {
        val privateDependency = dependency("yt-dlp", DesktopDependencySource.AppPrivate, healthy = false)
        val systemDependency = dependency("yt-dlp", DesktopDependencySource.SystemPath, healthy = true)

        assertEquals(systemDependency, chooseAutoDependency(privateDependency, systemDependency))
    }

    @Test
    fun `broken selfhost dependency is selected for portable repair without changing ownership`() {
        val resolution =
            resolution(
                preference = EnvPrefBundled,
                ytDlpSource = DesktopDependencySource.AppPrivate,
                ytDlpHealthy = false,
            )

        assertTrue(resolution.missingPortableDependencies().ytDlp)
        assertEquals(DesktopDependencySource.AppPrivate, resolution.ytDlp?.source)
        assertEquals(YtDlpUpdateDisposition.DownloadAppPrivate, resolution.ytDlpUpdateDisposition())
    }

    @Test
    fun `bundled and auto private dependencies remain app-managed`() {
        val bundled = resolution(EnvPrefBundled, DesktopDependencySource.AppPrivate)
        val auto = resolution(EnvPrefAuto, DesktopDependencySource.AppPrivate)

        assertEquals(YtDlpUpdateDisposition.DownloadAppPrivate, bundled.ytDlpUpdateDisposition())
        assertEquals(YtDlpUpdateDisposition.DownloadAppPrivate, auto.ytDlpUpdateDisposition())
    }

    @Test
    fun `packaged dependency is read only and updates install an app-private replacement`() {
        val resolution = resolution(EnvPrefBundled, DesktopDependencySource.Packaged)

        assertEquals(YtDlpUpdateDisposition.DownloadAppPrivate, resolution.ytDlpUpdateDisposition())
        assertEquals(DesktopDependencySource.Packaged, resolution.ytDlp?.source)
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
    fun `windows delegates broken system dependency repair to package manager`() {
        val commands =
            systemDependencyInstallCommands(
                isWindows = true,
                isMac = false,
                resolution =
                    resolution(
                        preference = EnvPrefSystem,
                        ytDlpSource = DesktopDependencySource.SystemPath,
                        ffmpegSource = DesktopDependencySource.SystemPath,
                        ytDlpHealthy = false,
                        ffmpegHealthy = true,
                    ),
            )

        assertEquals(listOf("yt-dlp.yt-dlp"), commands.map { it[it.indexOf("--id") + 1] })
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
        ytDlpHealthy: Boolean = true,
        ffmpegHealthy: Boolean = true,
    ): DesktopDependencyResolution =
        DesktopDependencyResolution(
            environmentPreference = preference,
            ytDlp = ytDlpSource?.let { dependency("yt-dlp", it, ytDlpHealthy) },
            ffmpeg = ffmpegSource?.let { dependency("ffmpeg", it, ffmpegHealthy) },
            aria2c = null,
        )

    private fun dependency(
        name: String,
        source: DesktopDependencySource,
        healthy: Boolean = true,
    ): ResolvedDesktopDependency =
        ResolvedDesktopDependency(
            name = name,
            path = Path.of("/test", name),
            source = source,
            health =
                DesktopDependencyHealth(
                    status =
                        if (healthy) DesktopDependencyHealthStatus.Healthy
                        else DesktopDependencyHealthStatus.Broken,
                    diagnostic = if (healthy) null else "test failure",
                ),
        )
}
