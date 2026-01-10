package com.junkfood.seal.desktop.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.SettingsApplications
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.ViewComfy
import androidx.compose.runtime.Composable
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.download_archive
import com.junkfood.seal.shared.generated.resources.download_archive_desc
import com.junkfood.seal.shared.generated.resources.download_playlist
import com.junkfood.seal.shared.generated.resources.download_playlist_desc
import com.junkfood.seal.shared.generated.resources.general_settings
import com.junkfood.seal.shared.generated.resources.restrict_filenames
import com.junkfood.seal.shared.generated.resources.restrict_filenames_desc
import com.junkfood.seal.shared.generated.resources.sponsorblock
import com.junkfood.seal.shared.generated.resources.sponsorblock_desc
import com.junkfood.seal.shared.generated.resources.create_thumbnail
import com.junkfood.seal.shared.generated.resources.create_thumbnail_summary
import com.junkfood.seal.shared.generated.resources.print_details
import com.junkfood.seal.shared.generated.resources.print_details_desc
import org.jetbrains.compose.resources.stringResource
import com.junkfood.seal.util.DownloadPreferences

@Composable
internal fun GeneralSettingsPage(
    preferences: DownloadPreferences,
    onUpdate: ((DownloadPreferences) -> DownloadPreferences) -> Unit,
    onBack: () -> Unit,
) {
    SettingsPageScaffold(title = stringResource(Res.string.general_settings), onBack = onBack) {
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

        ToggleCard(
            title = stringResource(Res.string.print_details),
            description = stringResource(Res.string.print_details_desc),
            icon = Icons.Rounded.Speed,
            checked = preferences.debug,
        ) { checked -> onUpdate { it.copy(debug = checked) } }
    }
}
