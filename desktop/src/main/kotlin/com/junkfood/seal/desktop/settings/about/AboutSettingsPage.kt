package com.junkfood.seal.desktop.settings.about

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import com.junkfood.seal.desktop.settings.SelectionCard
import com.junkfood.seal.desktop.settings.SettingsPageScaffold
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.about
import com.junkfood.seal.shared.generated.resources.credits
import com.junkfood.seal.shared.generated.resources.credits_desc
import com.junkfood.seal.shared.generated.resources.desktop_app_update
import com.junkfood.seal.shared.generated.resources.desktop_manual_update_desc
import com.junkfood.seal.shared.generated.resources.matrix_space
import com.junkfood.seal.shared.generated.resources.package_name
import com.junkfood.seal.shared.generated.resources.readme
import com.junkfood.seal.shared.generated.resources.readme_desc
import com.junkfood.seal.shared.generated.resources.sponsor
import com.junkfood.seal.shared.generated.resources.sponsor_desc
import com.junkfood.seal.shared.generated.resources.telegram_channel
import com.junkfood.seal.shared.generated.resources.version
import org.jetbrains.compose.resources.stringResource

private const val repoUrl = "https://github.com/JunkFood02/Seal"
internal const val desktopReleaseUrl = "https://github.com/Axiaobo7788/Seal-Desktop/releases"
private const val sponsorUrl = "https://github.com/sponsors/JunkFood02"
private const val telegramUrl = "https://t.me/seal_app"
private const val matrixUrl = "https://matrix.to/#/#seal-space:matrix.org"

@Composable
internal fun AboutSettingsPage(
    onOpenCredits: () -> Unit,
    onOpenUpdate: () -> Unit,
    onBack: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val appVersion =
        remember {
            System.getProperty("seal.app.version")
                ?: System.getProperty("jpackage.app-version")
                ?: "dev"
        }

    SettingsPageScaffold(title = stringResource(Res.string.about), onBack = onBack) {
        SelectionCard(
            title = stringResource(Res.string.readme),
            description = stringResource(Res.string.readme_desc),
            icon = Icons.Rounded.Description,
            onClick = { uriHandler.openUri(repoUrl) },
        )
        SelectionCard(
            title = stringResource(Res.string.sponsor),
            description = stringResource(Res.string.sponsor_desc),
            icon = Icons.Rounded.VolunteerActivism,
            onClick = { uriHandler.openUri(sponsorUrl) },
        )
        SelectionCard(
            title = stringResource(Res.string.telegram_channel),
            description = telegramUrl,
            icon = Icons.Rounded.Info,
            onClick = { uriHandler.openUri(telegramUrl) },
        )
        SelectionCard(
            title = stringResource(Res.string.matrix_space),
            description = matrixUrl,
            icon = Icons.Rounded.Info,
            onClick = { uriHandler.openUri(matrixUrl.replace("#seal-space", "%23seal-space")) },
        )
        SelectionCard(
            title = stringResource(Res.string.credits),
            description = stringResource(Res.string.credits_desc),
            icon = Icons.Rounded.AutoAwesome,
            onClick = onOpenCredits,
        )

        SelectionCard(
            title = stringResource(Res.string.desktop_app_update),
            description = stringResource(Res.string.desktop_manual_update_desc),
            icon = Icons.Rounded.NewReleases,
            onClick = onOpenUpdate,
        )

        SelectionCard(
            title = stringResource(Res.string.version),
            description = appVersion,
            icon = Icons.Rounded.Info,
            onClick = { clipboardManager.setText(AnnotatedString(appVersion)) },
        )

        SelectionCard(
            title = stringResource(Res.string.package_name),
            description = "com.junkfood.seal.desktop",
            icon = null,
            onClick = { clipboardManager.setText(AnnotatedString("com.junkfood.seal.desktop")) },
        )
    }
}
