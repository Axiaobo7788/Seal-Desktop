package com.junkfood.seal.desktop.settings.directory

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.runtime.Composable
import com.junkfood.seal.desktop.settings.PreferenceSubtitle
import com.junkfood.seal.desktop.settings.SettingsPageScaffold
import com.junkfood.seal.desktop.settings.TextFieldCard
import com.junkfood.seal.desktop.settings.ToggleCard
import com.junkfood.seal.desktop.ytdlp.DesktopYtDlpPaths
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.advanced_settings
import com.junkfood.seal.shared.generated.resources.custom_command_directory
import com.junkfood.seal.shared.generated.resources.custom_command_directory_desc
import com.junkfood.seal.shared.generated.resources.download_archive
import com.junkfood.seal.shared.generated.resources.download_directory
import com.junkfood.seal.shared.generated.resources.download_directory_desc
import com.junkfood.seal.shared.generated.resources.download_playlist_desc
import com.junkfood.seal.shared.generated.resources.general_settings
import com.junkfood.seal.shared.generated.resources.output_template
import com.junkfood.seal.shared.generated.resources.output_template_desc
import com.junkfood.seal.shared.generated.resources.playlist_title
import com.junkfood.seal.shared.generated.resources.privacy
import com.junkfood.seal.shared.generated.resources.private_directory
import com.junkfood.seal.shared.generated.resources.private_directory_desc
import com.junkfood.seal.shared.generated.resources.subdirectory
import com.junkfood.seal.shared.generated.resources.subdirectory_desc
import com.junkfood.seal.util.DownloadPreferences
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DirectorySettingsPage(
    preferences: DownloadPreferences,
    onUpdate: ((DownloadPreferences) -> DownloadPreferences) -> Unit,
    onBack: () -> Unit,
) {
    val archivePath = DesktopYtDlpPaths.archiveFile().toAbsolutePath().toString()
    SettingsPageScaffold(title = stringResource(Res.string.download_directory), onBack = onBack) {
        PreferenceSubtitle(text = stringResource(Res.string.general_settings))

        TextFieldCard(
            title = stringResource(Res.string.output_template),
            description = stringResource(Res.string.output_template_desc),
            icon = Icons.Outlined.FolderSpecial,
            value = preferences.outputTemplate,
            placeholder = "%(title)s.%(ext)s",
        ) { newValue -> onUpdate { it.copy(outputTemplate = newValue) } }

        ToggleCard(
            title = stringResource(Res.string.playlist_title),
            description = stringResource(Res.string.download_playlist_desc),
            icon = Icons.Rounded.Folder,
            checked = preferences.subdirectoryPlaylistTitle,
        ) { checked -> onUpdate { it.copy(subdirectoryPlaylistTitle = checked) } }

        ToggleCard(
            title = stringResource(Res.string.subdirectory),
            description = stringResource(Res.string.subdirectory_desc),
            icon = Icons.Outlined.Lock,
            checked = preferences.subdirectoryExtractor,
        ) { checked -> onUpdate { it.copy(subdirectoryExtractor = checked) } }

        TextFieldCard(
            title = stringResource(Res.string.custom_command_directory),
            description = stringResource(Res.string.custom_command_directory_desc),
            icon = Icons.Rounded.Folder,
            value = preferences.commandDirectory,
        ) { newValue -> onUpdate { it.copy(commandDirectory = newValue) } }

        PreferenceSubtitle(text = stringResource(Res.string.privacy))

        ToggleCard(
            title = stringResource(Res.string.private_directory),
            description = stringResource(Res.string.private_directory_desc),
            icon = Icons.Outlined.Lock,
            checked = preferences.privateDirectory,
        ) { checked -> onUpdate { it.copy(privateDirectory = checked) } }

        PreferenceSubtitle(text = stringResource(Res.string.advanced_settings))

        ToggleCard(
            title = stringResource(Res.string.download_archive),
            description = archivePath,
            icon = Icons.Rounded.Archive,
            checked = preferences.useDownloadArchive,
        ) { checked -> onUpdate { it.copy(useDownloadArchive = checked) } }
    }
}
