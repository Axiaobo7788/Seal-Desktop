package com.junkfood.seal.desktop.settings.general

import com.junkfood.seal.desktop.ytdlp.DesktopDependencyHealthStatus
import com.junkfood.seal.desktop.ytdlp.DesktopDependencyResolution
import com.junkfood.seal.desktop.ytdlp.DesktopDependencySource
import com.junkfood.seal.desktop.ytdlp.ResolvedDesktopDependency

internal enum class DependencyUiHealth {
    Healthy,
    Missing,
    Broken,
}

internal enum class EnvironmentSummaryHealth {
    Healthy,
    DegradedOptional,
    MissingRequired,
    BrokenRequired,
}

internal data class DependencyUiStatus(
    val name: String,
    val required: Boolean,
    val health: DependencyUiHealth,
    val source: DesktopDependencySource? = null,
    val shortVersion: String? = null,
    val path: String? = null,
    val diagnostic: String? = null,
)

internal data class EnvironmentSummaryUiModel(
    val health: EnvironmentSummaryHealth,
    val dependencies: List<DependencyUiStatus>,
) {
    val optionalIssues: List<String>
        get() = dependencies.filter { !it.required && it.health != DependencyUiHealth.Healthy }.map { it.name }

    val requiredIssues: List<String>
        get() = dependencies.filter { it.required && it.health != DependencyUiHealth.Healthy }.map { it.name }
}

internal fun DesktopDependencyResolution.toEnvironmentSummaryUiModel(): EnvironmentSummaryUiModel {
    val dependencies =
        listOf(
            ytDlp.toUiStatus(name = "yt-dlp", required = true),
            ffmpeg.toUiStatus(name = "ffmpeg", required = true),
            aria2c.toUiStatus(name = "aria2c", required = false),
        )
    val summaryHealth =
        when {
            dependencies.any { it.required && it.health == DependencyUiHealth.Broken } -> EnvironmentSummaryHealth.BrokenRequired
            dependencies.any { it.required && it.health == DependencyUiHealth.Missing } -> EnvironmentSummaryHealth.MissingRequired
            dependencies.any { !it.required && it.health != DependencyUiHealth.Healthy } -> EnvironmentSummaryHealth.DegradedOptional
            else -> EnvironmentSummaryHealth.Healthy
        }
    return EnvironmentSummaryUiModel(summaryHealth, dependencies)
}

private fun ResolvedDesktopDependency?.toUiStatus(
    name: String,
    required: Boolean,
): DependencyUiStatus {
    if (this == null) {
        return DependencyUiStatus(
            name = name,
            required = required,
            health = DependencyUiHealth.Missing,
        )
    }
    val uiHealth =
        when (health.status) {
            DesktopDependencyHealthStatus.Healthy -> DependencyUiHealth.Healthy
            DesktopDependencyHealthStatus.Missing -> DependencyUiHealth.Missing
            DesktopDependencyHealthStatus.Broken -> DependencyUiHealth.Broken
        }
    return DependencyUiStatus(
        name = name,
        required = required,
        health = uiHealth,
        source = source,
        shortVersion = health.version?.lineSequence()?.firstOrNull()?.trim()?.take(96)?.takeIf { it.isNotEmpty() },
        path = path.toAbsolutePath().normalize().toString(),
        diagnostic = health.diagnostic?.trim()?.takeIf { it.isNotEmpty() },
    )
}
