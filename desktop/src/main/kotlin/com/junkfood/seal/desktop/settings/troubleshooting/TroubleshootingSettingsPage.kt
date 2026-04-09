package com.junkfood.seal.desktop.settings.troubleshooting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.Spellcheck
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.junkfood.seal.desktop.settings.ActionCard
import com.junkfood.seal.desktop.settings.DesktopAppSettings
import com.junkfood.seal.desktop.settings.PreferenceSubtitle
import com.junkfood.seal.desktop.settings.SelectionCard
import com.junkfood.seal.desktop.settings.SettingsPageScaffold
import com.junkfood.seal.desktop.settings.ToggleCard
import com.junkfood.seal.desktop.settings.YtdlpUpdateCard
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.cookies
import com.junkfood.seal.shared.generated.resources.cookies_desc
import com.junkfood.seal.shared.generated.resources.download_directory
import com.junkfood.seal.shared.generated.resources.issue_tracker_hint
import com.junkfood.seal.shared.generated.resources.network
import com.junkfood.seal.shared.generated.resources.restrict_filenames
import com.junkfood.seal.shared.generated.resources.restrict_filenames_desc
import com.junkfood.seal.shared.generated.resources.trouble_shooting
import com.junkfood.seal.shared.generated.resources.update
import com.junkfood.seal.util.DownloadPreferences
import org.jetbrains.compose.resources.stringResource

private const val sealIssueUrl = "https://github.com/JunkFood02/Seal/issues/1399"
private const val ytdlpIssueUrl = "https://github.com/yt-dlp/yt-dlp/issues/3766"

@Composable
internal fun TroubleshootingSettingsPage(
    appSettings: DesktopAppSettings,
    onUpdateAppSettings: ((DesktopAppSettings) -> DesktopAppSettings) -> Unit,
    preferences: DownloadPreferences,
    onUpdate: ((DownloadPreferences) -> DownloadPreferences) -> Unit,
    onBack: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    SettingsPageScaffold(title = stringResource(Res.string.trouble_shooting), onBack = onBack) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Row(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(Res.string.issue_tracker_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ActionCard(
                    title = "Seal Issue Tracker",
                    description = null,
                    icon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = { uriHandler.openUri(sealIssueUrl) }
                )
                ActionCard(
                    title = "yt-dlp Issue Tracker",
                    description = null,
                    icon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = { uriHandler.openUri(ytdlpIssueUrl) }
                )
            }
        }

        PreferenceSubtitle(text = stringResource(Res.string.update))
        
        YtdlpUpdateCard(
            appSettings = appSettings,
            onUpdateAppSettings = { newSettings -> onUpdateAppSettings { newSettings } }
        )

        PreferenceSubtitle(text = stringResource(Res.string.network))
        SelectionCard(
            title = stringResource(Res.string.cookies),
            description = stringResource(Res.string.cookies_desc),
            icon = Icons.Outlined.Cookie,
            onClick = { /* Do nothing for now unless we can link to cookies page */ },
        )

        PreferenceSubtitle(text = stringResource(Res.string.download_directory))
        ToggleCard(
            title = stringResource(Res.string.restrict_filenames),
            description = stringResource(Res.string.restrict_filenames_desc),
            icon = Icons.Outlined.Spellcheck,
            checked = preferences.restrictFilenames,
        ) { checked -> onUpdate { it.copy(restrictFilenames = checked) } }
    }
}
