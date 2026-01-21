package com.junkfood.seal.desktop.settings.general

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.SettingsApplications
import androidx.compose.material.icons.rounded.Speed
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
import com.junkfood.seal.shared.generated.resources.download_playlist
import com.junkfood.seal.shared.generated.resources.download_playlist_desc
import com.junkfood.seal.shared.generated.resources.general_settings
import com.junkfood.seal.shared.generated.resources.print_details
import com.junkfood.seal.shared.generated.resources.print_details_desc
import com.junkfood.seal.shared.generated.resources.privacy
import com.junkfood.seal.shared.generated.resources.private_mode
import com.junkfood.seal.shared.generated.resources.private_mode_desc
import com.junkfood.seal.shared.generated.resources.restrict_filenames
import com.junkfood.seal.shared.generated.resources.restrict_filenames_desc
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
    val ytdlpUpdateText = stringResource(Res.string.ytdlp_update)
    val ytdlpVersionLabel = stringResource(Res.string.ytdlp_version)
    val ytdlpUpdateFailText = stringResource(Res.string.yt_dlp_update_fail)

    val scope = rememberCoroutineScope()
    val fetcher = remember { YtDlpFetcher() }
    var isUpdatingYtDlp by remember { mutableStateOf(false) }
    var ytDlpDesc by remember(ytdlpUpdateText) { mutableStateOf(ytdlpUpdateText) }

    LaunchedEffect(Unit) {
        val existing = fetcher.findExistingBinary()
        if (existing != null) {
            val version = readYtDlpVersion(existing)
            ytDlpDesc =
                if (!version.isNullOrBlank()) {
                    "$ytdlpVersionLabel: $version"
                } else {
                    ytdlpUpdateText
                }
        }
    }

    SettingsPageScaffold(title = stringResource(Res.string.general_settings), onBack = onBack) {
        PreferenceSubtitle(text = stringResource(Res.string.general_settings))

        SelectionCard(
            title = stringResource(Res.string.ytdlp_update_action),
            description = ytDlpDesc,
            icon = Icons.Outlined.Update,
            enabled = !isUpdatingYtDlp,
            onClick = {
                scope.launch {
                    isUpdatingYtDlp = true
                    ytDlpDesc = ytdlpUpdateText
                    val updated =
                        withContext(Dispatchers.IO) {
                            runCatching {
                                fetcher.invalidateCachedBinary()
                                val binary = fetcher.ensureBinary()
                                readYtDlpVersion(binary)
                            }
                        }

                    ytDlpDesc =
                        updated.getOrNull()?.takeIf { it.isNotBlank() }?.let { v ->
                            "$ytdlpVersionLabel: $v"
                        } ?: ytdlpUpdateFailText

                    isUpdatingYtDlp = false
                }
            },
        )

        ToggleCard(
            title = stringResource(Res.string.download_playlist),
            description = stringResource(Res.string.download_playlist_desc),
            icon = Icons.Rounded.ViewComfy,
            checked = preferences.downloadPlaylist,
        ) { checked -> onUpdate { it.copy(downloadPlaylist = checked) } }

        ToggleCard(
            title = stringResource(Res.string.create_thumbnail),
            description = stringResource(Res.string.create_thumbnail_summary),
            icon = Icons.Rounded.Image,
            checked = preferences.createThumbnail,
        ) { checked -> onUpdate { it.copy(createThumbnail = checked) } }

        ToggleCard(
            title = stringResource(Res.string.restrict_filenames),
            description = stringResource(Res.string.restrict_filenames_desc),
            icon = Icons.Rounded.Code,
            checked = preferences.restrictFilenames,
        ) { checked -> onUpdate { it.copy(restrictFilenames = checked) } }

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

        TextFieldCard(
            title = stringResource(Res.string.sponsorblock_categories),
            description = stringResource(Res.string.sponsorblock_categories_desc),
            icon = Icons.Rounded.BugReport,
            value = preferences.sponsorBlockCategory,
            enabled = preferences.sponsorBlock,
        ) { newValue -> onUpdate { it.copy(sponsorBlockCategory = newValue) } }

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
            title = stringResource(Res.string.print_details),
            description = stringResource(Res.string.print_details_desc),
            icon = Icons.Rounded.Speed,
            checked = preferences.debug,
        ) { checked -> onUpdate { it.copy(debug = checked) } }
    }
}
