package com.junkfood.seal.desktop.settings.general

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.SettingsApplications
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.ViewComfy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.junkfood.seal.desktop.settings.DesktopAppSettings
import com.junkfood.seal.desktop.settings.PreferenceInfo
import com.junkfood.seal.desktop.settings.PreferenceSubtitle
import com.junkfood.seal.desktop.settings.SelectionCard
import com.junkfood.seal.desktop.settings.SettingsPageScaffold
import com.junkfood.seal.desktop.settings.ChoiceDialog
import com.junkfood.seal.desktop.settings.EnvPrefAuto
import com.junkfood.seal.desktop.settings.EnvPrefBundled
import com.junkfood.seal.desktop.settings.EnvPrefSystem
import com.junkfood.seal.desktop.settings.ToggleCard
import com.junkfood.seal.desktop.settings.ActionCard
import com.junkfood.seal.desktop.ytdlp.DesktopDependencyResolution
import com.junkfood.seal.desktop.ytdlp.DesktopDependencyHealthStatus
import com.junkfood.seal.desktop.ytdlp.DesktopDependencyResolver
import com.junkfood.seal.desktop.ytdlp.DesktopDependencySource
import com.junkfood.seal.desktop.ytdlp.ResolvedDesktopDependency
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.advanced_settings
import com.junkfood.seal.shared.generated.resources.create_thumbnail
import com.junkfood.seal.shared.generated.resources.create_thumbnail_summary
import com.junkfood.seal.shared.generated.resources.disable_preview
import com.junkfood.seal.shared.generated.resources.disable_preview_desc
import com.junkfood.seal.shared.generated.resources.desktop_dependency_broken_summary
import com.junkfood.seal.shared.generated.resources.desktop_dependency_missing_summary
import com.junkfood.seal.shared.generated.resources.desktop_dependency_optional
import com.junkfood.seal.shared.generated.resources.desktop_dependency_source_packaged
import com.junkfood.seal.shared.generated.resources.desktop_dependency_source_selfhost
import com.junkfood.seal.shared.generated.resources.desktop_dependency_source_system
import com.junkfood.seal.shared.generated.resources.desktop_dependency_status_broken
import com.junkfood.seal.shared.generated.resources.desktop_dependency_status_detecting
import com.junkfood.seal.shared.generated.resources.desktop_dependency_status_healthy
import com.junkfood.seal.shared.generated.resources.desktop_dependency_status_missing
import com.junkfood.seal.shared.generated.resources.env_pref_auto
import com.junkfood.seal.shared.generated.resources.env_pref_bundled
import com.junkfood.seal.shared.generated.resources.env_pref_system
import com.junkfood.seal.shared.generated.resources.env_preference
import com.junkfood.seal.shared.generated.resources.download_archive
import com.junkfood.seal.shared.generated.resources.download_archive_desc
import com.junkfood.seal.shared.generated.resources.download_notification
import com.junkfood.seal.shared.generated.resources.download_notification_desc
import com.junkfood.seal.shared.generated.resources.download_playlist
import com.junkfood.seal.shared.generated.resources.download_playlist_desc
import com.junkfood.seal.shared.generated.resources.general_settings
import com.junkfood.seal.shared.generated.resources.print_details
import com.junkfood.seal.shared.generated.resources.print_details_desc
import com.junkfood.seal.shared.generated.resources.privacy
import com.junkfood.seal.shared.generated.resources.private_mode
import com.junkfood.seal.shared.generated.resources.private_mode_desc
import com.junkfood.seal.shared.generated.resources.settings_before_download
import com.junkfood.seal.shared.generated.resources.settings_before_download_desc
import com.junkfood.seal.shared.generated.resources.sponsorblock
import com.junkfood.seal.shared.generated.resources.sponsorblock_categories
import com.junkfood.seal.shared.generated.resources.sponsorblock_categories_desc
import com.junkfood.seal.shared.generated.resources.sponsorblock_desc
import com.junkfood.seal.util.DownloadPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun GeneralSettingsPage(
    preferences: DownloadPreferences,
    onUpdate: ((DownloadPreferences) -> DownloadPreferences) -> Unit,
    appSettings: DesktopAppSettings,
    onUpdateAppSettings: ((DesktopAppSettings) -> DesktopAppSettings) -> Unit,
    onBack: () -> Unit,
) {
    var showSponsorBlockDialog by remember { mutableStateOf(false) }
    var showEnvPrefDialog by remember { mutableStateOf(false) }
    val envResolution by
        produceState<DesktopDependencyResolution?>(null, appSettings.environmentPreference) {
            value =
                withContext(Dispatchers.IO) {
                    DesktopDependencyResolver.resolve(appSettings.environmentPreference)
                }
        }
    val envDetectionSummary = envResolution?.let { dependencyDetectionSummary(it) }.orEmpty()

    SettingsPageScaffold(title = stringResource(Res.string.general_settings), onBack = onBack) {
        PreferenceSubtitle(text = stringResource(Res.string.general_settings))

        com.junkfood.seal.desktop.settings.YtdlpUpdateCard(
            appSettings = appSettings,
            onUpdateAppSettings = { newSettings -> onUpdateAppSettings { newSettings } }
        )

        val envPrefLabel = when (appSettings.environmentPreference) {
            EnvPrefBundled -> stringResource(Res.string.env_pref_bundled)
            EnvPrefSystem -> stringResource(Res.string.env_pref_system)
            else -> stringResource(Res.string.env_pref_auto)
        }

        SelectionCard(
            title = stringResource(Res.string.env_preference),
            description =
                buildString {
                    append(envPrefLabel)
                    if (envDetectionSummary.isNotBlank()) {
                        append('\n')
                        append(envDetectionSummary)
                    }
                },
            icon = Icons.Rounded.SettingsApplications,
            onClick = { showEnvPrefDialog = true }
        )

        ToggleCard(
            title = stringResource(Res.string.download_notification),
            description = stringResource(Res.string.download_notification_desc),
            icon = Icons.Rounded.Notifications,
            checked = appSettings.downloadNotificationEnabled,
        ) { checked -> onUpdateAppSettings { it.copy(downloadNotificationEnabled = checked) } }

        ToggleCard(
            title = stringResource(Res.string.settings_before_download),
            description = stringResource(Res.string.settings_before_download_desc),
            icon = if (appSettings.configureBeforeDownload) Icons.Outlined.DoneAll else Icons.Outlined.RemoveDone,
            checked = appSettings.configureBeforeDownload,
        ) { checked -> onUpdateAppSettings { it.copy(configureBeforeDownload = checked) } }

        ToggleCard(
            title = stringResource(Res.string.create_thumbnail),
            description = stringResource(Res.string.create_thumbnail_summary),
            icon = Icons.Rounded.Image,
            checked = preferences.createThumbnail,
        ) { checked -> onUpdate { it.copy(createThumbnail = checked) } }

        ToggleCard(
            title = stringResource(Res.string.print_details),
            description = stringResource(Res.string.print_details_desc),
            icon = Icons.Outlined.Print,
            checked = preferences.debug,
        ) { checked -> onUpdate { it.copy(debug = checked) } }

        PreferenceSubtitle(text = stringResource(Res.string.privacy))

        ToggleCard(
            title = stringResource(Res.string.private_mode),
            description = stringResource(Res.string.private_mode_desc),
            icon = Icons.Rounded.SettingsApplications,
            checked = preferences.privateMode,
        ) { checked -> onUpdate { it.copy(privateMode = checked) } }

        ToggleCard(
            title = stringResource(Res.string.disable_preview),
            description = stringResource(Res.string.disable_preview_desc),
            icon = if (appSettings.disablePreview) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
            checked = appSettings.disablePreview,
        ) { checked -> onUpdateAppSettings { it.copy(disablePreview = checked) } }

        PreferenceSubtitle(text = stringResource(Res.string.advanced_settings))

        ToggleCard(
            title = stringResource(Res.string.download_playlist),
            description = stringResource(Res.string.download_playlist_desc),
            icon = Icons.Rounded.ViewComfy,
            checked = preferences.downloadPlaylist,
        ) { checked -> onUpdate { it.copy(downloadPlaylist = checked) } }

        ToggleCard(
            title = stringResource(Res.string.download_archive),
            description = stringResource(Res.string.download_archive_desc),
            icon = Icons.Rounded.Archive,
            checked = preferences.useDownloadArchive,
        ) { checked -> onUpdate { it.copy(useDownloadArchive = checked) } }

        ToggleCard(
            title = stringResource(Res.string.sponsorblock),
            description = stringResource(Res.string.sponsorblock_desc),
            icon = Icons.Rounded.BugReport,
            checked = preferences.sponsorBlock,
        ) { checked -> onUpdate { it.copy(sponsorBlock = checked) } }

        ActionCard(
            title = stringResource(Res.string.sponsorblock_categories),
            description = stringResource(Res.string.sponsorblock_categories_desc),
            icon = Icons.Rounded.BugReport,
            enabled = preferences.sponsorBlock,
            onClick = { showSponsorBlockDialog = true }
        )
    }

    if (showSponsorBlockDialog) {
        SponsorBlockDialog(
            visible = showSponsorBlockDialog,
            initialCategories = preferences.sponsorBlockCategory,
            onDismissRequest = { showSponsorBlockDialog = false },
            onConfirm = { categories ->
                onUpdate { it.copy(sponsorBlockCategory = categories) }
                showSponsorBlockDialog = false 
            }
        )
    }

    if (showEnvPrefDialog) {
        ChoiceDialog(
            visible = showEnvPrefDialog,
            title = stringResource(Res.string.env_preference),
            options = listOf(
                stringResource(Res.string.env_pref_auto) to EnvPrefAuto,
                stringResource(Res.string.env_pref_bundled) to EnvPrefBundled,
                stringResource(Res.string.env_pref_system) to EnvPrefSystem,
            ),
            selected = appSettings.environmentPreference,
            onSelect = { pref -> onUpdateAppSettings { it.copy(environmentPreference = pref) } },
            onDismiss = { showEnvPrefDialog = false },
            footer = { selectedPreference ->
                val selectedResolution by
                    produceState<DesktopDependencyResolution?>(null, selectedPreference) {
                        value =
                            withContext(Dispatchers.IO) {
                                DesktopDependencyResolver.resolve(selectedPreference)
                            }
                    }
                PreferenceInfo(
                    text =
                        selectedResolution
                            ?.let { dependencyDetectionSummary(it) }
                            ?: stringResource(Res.string.desktop_dependency_status_detecting),
                    applyPaddings = false,
                )
            }
        )
    }
}

@Composable
private fun dependencyDetectionSummary(resolution: DesktopDependencyResolution): String {
    val missing = stringResource(Res.string.desktop_dependency_status_missing)
    val healthy = stringResource(Res.string.desktop_dependency_status_healthy)
    val broken = stringResource(Res.string.desktop_dependency_status_broken)
    val optional = stringResource(Res.string.desktop_dependency_optional)
    val selfhost = stringResource(Res.string.desktop_dependency_source_selfhost)
    val packaged = stringResource(Res.string.desktop_dependency_source_packaged)
    val system = stringResource(Res.string.desktop_dependency_source_system)

    fun sourceLabel(source: DesktopDependencySource): String =
        when (source) {
            DesktopDependencySource.AppPrivate -> selfhost
            DesktopDependencySource.Packaged -> packaged
            DesktopDependencySource.SystemPath -> system
        }

    fun dependencyLine(name: String, dependency: ResolvedDesktopDependency?): String {
        if (dependency == null) return "$name: $missing"
        val status =
            when (dependency.health.status) {
                DesktopDependencyHealthStatus.Healthy -> healthy
                DesktopDependencyHealthStatus.Broken -> broken
                DesktopDependencyHealthStatus.Missing -> missing
            }
        val detail =
            dependency.health.version
                ?.takeIf { it.isNotBlank() }
                ?: dependency.health.diagnostic?.takeIf { it.isNotBlank() }
        return buildString {
            append("$name: ${sourceLabel(dependency.source)} · $status")
            detail?.let { append(" · ${it.lineSequence().first().take(160)}") }
            append("\n${dependency.path.toAbsolutePath()}")
        }
    }

    val ytDlpLine = dependencyLine("yt-dlp", resolution.ytDlp)
    val ffmpegLine = dependencyLine("ffmpeg", resolution.ffmpeg)
    val aria2cLine =
        resolution.aria2c?.let { dependencyLine("aria2c", it) }
            ?: "aria2c: $missing ($optional)"
    val missingSummary =
        resolution.missingNames.takeIf { it.isNotEmpty() }?.let { names ->
            stringResource(Res.string.desktop_dependency_missing_summary, names.joinToString())
        }
    val brokenSummary =
        resolution.brokenNames.takeIf { it.isNotEmpty() }?.let { names ->
            stringResource(Res.string.desktop_dependency_broken_summary, names.joinToString())
        }

    return buildString {
        appendLine(ytDlpLine)
        appendLine(ffmpegLine)
        append(aria2cLine)
        if (missingSummary != null) {
            appendLine()
            append(missingSummary)
        }
        if (brokenSummary != null) {
            appendLine()
            append(brokenSummary)
        }
    }
}
