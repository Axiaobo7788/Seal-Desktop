package com.junkfood.seal.desktop.settings.general

import com.junkfood.seal.desktop.settings.EnvPrefAuto
import com.junkfood.seal.desktop.ytdlp.DesktopDependencyHealth
import com.junkfood.seal.desktop.ytdlp.DesktopDependencyHealthStatus
import com.junkfood.seal.desktop.ytdlp.DesktopDependencyResolution
import com.junkfood.seal.desktop.ytdlp.DesktopDependencySource
import com.junkfood.seal.desktop.ytdlp.ResolvedDesktopDependency
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DependencyEnvironmentPresentationTest {
    @Test
    fun `healthy required tools with missing optional tool is degraded only`() {
        val model = resolution(healthy("yt-dlp"), healthy("ffmpeg"), null).toEnvironmentSummaryUiModel()

        assertEquals(EnvironmentSummaryHealth.DegradedOptional, model.health)
        assertEquals(emptyList(), model.requiredIssues)
        assertEquals(listOf("aria2c"), model.optionalIssues)
    }

    @Test
    fun `broken required dependency takes precedence over missing required dependency`() {
        val model = resolution(null, broken("ffmpeg"), null).toEnvironmentSummaryUiModel()

        assertEquals(EnvironmentSummaryHealth.BrokenRequired, model.health)
        assertEquals(listOf("yt-dlp", "ffmpeg"), model.requiredIssues)
    }

    @Test
    fun `missing dependency has no invented source or path`() {
        val status = resolution(null, healthy("ffmpeg"), null).toEnvironmentSummaryUiModel().dependencies.first()

        assertEquals(DependencyUiHealth.Missing, status.health)
        assertNull(status.source)
        assertNull(status.path)
    }

    @Test
    fun `version summary is bounded to one line`() {
        val dependency =
            healthy("yt-dlp").copy(
                health = DesktopDependencyHealth(DesktopDependencyHealthStatus.Healthy, version = "first line\nsecond line"),
            )
        val status = resolution(dependency, healthy("ffmpeg"), healthy("aria2c")).toEnvironmentSummaryUiModel().dependencies.first()

        assertEquals("first line", status.shortVersion)
        assertEquals(EnvironmentSummaryHealth.Healthy, resolution(dependency, healthy("ffmpeg"), healthy("aria2c")).toEnvironmentSummaryUiModel().health)
    }

    private fun resolution(
        ytDlp: ResolvedDesktopDependency?,
        ffmpeg: ResolvedDesktopDependency?,
        aria2c: ResolvedDesktopDependency?,
    ) = DesktopDependencyResolution(EnvPrefAuto, ytDlp, ffmpeg, aria2c)

    private fun healthy(name: String) =
        ResolvedDesktopDependency(
            name = name,
            path = Path.of("build", "tools", name),
            source = DesktopDependencySource.SystemPath,
            health = DesktopDependencyHealth(DesktopDependencyHealthStatus.Healthy, version = "$name 1.0"),
        )

    private fun broken(name: String) =
        healthy(name).copy(
            health = DesktopDependencyHealth(DesktopDependencyHealthStatus.Broken, diagnostic = "exit 1"),
        )
}
