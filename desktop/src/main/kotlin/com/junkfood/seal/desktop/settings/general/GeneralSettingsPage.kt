package com.junkfood.seal.desktop.settings.general

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.SettingsApplications
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.ViewComfy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.junkfood.seal.desktop.settings.DesktopAppSettings
import com.junkfood.seal.desktop.settings.PreferenceSubtitle
import com.junkfood.seal.desktop.settings.SelectionCard
import com.junkfood.seal.desktop.settings.SettingsPageScaffold
import com.junkfood.seal.desktop.settings.TextFieldCard
import com.junkfood.seal.desktop.settings.ToggleCard
import com.junkfood.seal.desktop.settings.ActionCard
import com.junkfood.seal.desktop.ytdlp.YtDlpFetcher
import com.junkfood.seal.desktop.ytdlp.readYtDlpVersion
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.advanced_settings
import com.junkfood.seal.shared.generated.resources.create_thumbnail
import com.junkfood.seal.shared.generated.resources.create_thumbnail_summary
import com.junkfood.seal.shared.generated.resources.disable_preview
import com.junkfood.seal.shared.generated.resources.disable_preview_desc
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
import com.junkfood.seal.shared.generated.resources.yt_dlp_update_fail
import com.junkfood.seal.shared.generated.resources.ytdlp_update
import com.junkfood.seal.shared.generated.resources.ytdlp_update_action
import com.junkfood.seal.shared.generated.resources.ytdlp_version
import com.junkfood.seal.util.DownloadPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    
    SettingsPageScaffold(title = stringResource(Res.string.general_settings), onBack = onBack) {
        PreferenceSubtitle(text = stringResource(Res.string.general_settings))

        com.junkfood.seal.desktop.settings.YtdlpUpdateCard(
            appSettings = appSettings,
            onUpdateAppSettings = { newSettings -> onUpdateAppSettings { newSettings } }
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
            onConfirm = { 
                onUpdate { it.copy(sponsorBlockCategory = it.toString()) }
                showSponsorBlockDialog = false 
            }
        )
    }
}
