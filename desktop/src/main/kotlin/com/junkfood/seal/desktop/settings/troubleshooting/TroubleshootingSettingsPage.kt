package com.junkfood.seal.desktop.settings.troubleshooting

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Update
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import com.junkfood.seal.desktop.settings.PreferenceInfo
import com.junkfood.seal.desktop.settings.PreferenceSubtitle
import com.junkfood.seal.desktop.settings.SelectionCard
import com.junkfood.seal.desktop.settings.SettingsPageScaffold
import com.junkfood.seal.desktop.settings.ToggleCard
import com.junkfood.seal.desktop.ytdlp.DesktopYtDlpPaths
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.cookies
import com.junkfood.seal.shared.generated.resources.cookies_desc
import com.junkfood.seal.shared.generated.resources.download_directory
import com.junkfood.seal.shared.generated.resources.github_issue
import com.junkfood.seal.shared.generated.resources.github_issue_desc
import com.junkfood.seal.shared.generated.resources.issue_tracker
import com.junkfood.seal.shared.generated.resources.issue_tracker_hint
import com.junkfood.seal.shared.generated.resources.network
import com.junkfood.seal.shared.generated.resources.restrict_filenames
import com.junkfood.seal.shared.generated.resources.restrict_filenames_desc
import com.junkfood.seal.shared.generated.resources.trouble_shooting
import com.junkfood.seal.shared.generated.resources.update
import com.junkfood.seal.shared.generated.resources.ytdlp_update
import com.junkfood.seal.shared.generated.resources.ytdlp_update_action
import com.junkfood.seal.shared.generated.resources.yt_dlp_docs
import com.junkfood.seal.util.DownloadPreferences
import org.jetbrains.compose.resources.stringResource

private const val sealIssueUrl = "https://github.com/JunkFood02/Seal/issues"
private const val ytdlpIssueUrl = "https://github.com/yt-dlp/yt-dlp/issues"
private const val ytdlpDocsUrl = "https://github.com/yt-dlp/yt-dlp#usage-and-options"
private const val ytdlpReleasesUrl = "https://github.com/yt-dlp/yt-dlp/releases"

@Composable
internal fun TroubleshootingSettingsPage(
    preferences: DownloadPreferences,
    onUpdate: ((DownloadPreferences) -> DownloadPreferences) -> Unit,
    onBack: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val cookiePath = DesktopYtDlpPaths.cookiesFile().toAbsolutePath().toString()

    SettingsPageScaffold(title = stringResource(Res.string.trouble_shooting), onBack = onBack) {
        PreferenceSubtitle(text = stringResource(Res.string.issue_tracker))
        PreferenceInfo(text = stringResource(Res.string.issue_tracker_hint))

        SelectionCard(
            title = stringResource(Res.string.github_issue),
            description = stringResource(Res.string.github_issue_desc),
            icon = Icons.Rounded.BugReport,
            onClick = { uriHandler.openUri(sealIssueUrl) },
        )

        SelectionCard(
            title = "yt-dlp",
            description = stringResource(Res.string.github_issue_desc),
            icon = Icons.Rounded.BugReport,
            onClick = { uriHandler.openUri(ytdlpIssueUrl) },
        )

        PreferenceSubtitle(text = stringResource(Res.string.update))
        SelectionCard(
            title = stringResource(Res.string.ytdlp_update_action),
            description = stringResource(Res.string.ytdlp_update),
            icon = Icons.Rounded.Update,
            onClick = { uriHandler.openUri(ytdlpReleasesUrl) },
        )
        SelectionCard(
            title = stringResource(Res.string.yt_dlp_docs),
            description = ytdlpDocsUrl,
            icon = Icons.Rounded.Settings,
            onClick = { uriHandler.openUri(ytdlpDocsUrl) },
        )

        PreferenceSubtitle(text = stringResource(Res.string.network))
        SelectionCard(
            title = stringResource(Res.string.cookies),
            description = stringResource(Res.string.cookies_desc),
            icon = Icons.Rounded.Info,
            onClick = {},
            enabled = false,
        )
        if (preferences.cookies) {
            PreferenceInfo(text = cookiePath)
        }

        PreferenceSubtitle(text = stringResource(Res.string.download_directory))
        ToggleCard(
            title = stringResource(Res.string.restrict_filenames),
            description = stringResource(Res.string.restrict_filenames_desc),
            icon = Icons.Rounded.Settings,
            checked = preferences.restrictFilenames,
        ) { checked -> onUpdate { it.copy(restrictFilenames = checked) } }
    }
}
