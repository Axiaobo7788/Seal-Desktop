package com.junkfood.seal.desktop.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.about
import com.junkfood.seal.shared.generated.resources.auto_update
import com.junkfood.seal.shared.generated.resources.credits
import com.junkfood.seal.shared.generated.resources.credits_desc
import com.junkfood.seal.shared.generated.resources.enable_auto_update
import com.junkfood.seal.shared.generated.resources.matrix_space
import com.junkfood.seal.shared.generated.resources.pre_release_channel
import com.junkfood.seal.shared.generated.resources.readme
import com.junkfood.seal.shared.generated.resources.readme_desc
import com.junkfood.seal.shared.generated.resources.release
import com.junkfood.seal.shared.generated.resources.release_desc
import com.junkfood.seal.shared.generated.resources.sponsor
import com.junkfood.seal.shared.generated.resources.sponsor_desc
import com.junkfood.seal.shared.generated.resources.stable_channel
import com.junkfood.seal.shared.generated.resources.telegram_channel
import com.junkfood.seal.shared.generated.resources.update_channel
import com.junkfood.seal.shared.generated.resources.update_channel_desc
import com.junkfood.seal.shared.generated.resources.version
import org.jetbrains.compose.resources.stringResource

private const val repoUrl = "https://github.com/JunkFood02/Seal"
private const val releaseUrl = "https://github.com/JunkFood02/Seal/releases"
private const val sponsorUrl = "https://github.com/sponsors/JunkFood02"
private const val telegramUrl = "https://t.me/seal_app"
private const val matrixUrl = "https://matrix.to/#/#seal-space:matrix.org"

@Composable
internal fun AboutSettingsPage(
    settings: DesktopAppSettings,
    onUpdate: ((DesktopAppSettings) -> DesktopAppSettings) -> Unit,
    onOpenCredits: () -> Unit,
    onBack: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val appVersion = remember { System.getProperty("jpackage.app-version") ?: "dev" }
    var showChannelDialog by remember { mutableStateOf(false) }

    SettingsPageScaffold(title = stringResource(Res.string.about), onBack = onBack) {
        SelectionCard(
            title = stringResource(Res.string.readme),
            description = stringResource(Res.string.readme_desc),
            icon = Icons.Rounded.Description,
            onClick = { uriHandler.openUri(repoUrl) },
        )
        SelectionCard(
            title = stringResource(Res.string.release),
            description = stringResource(Res.string.release_desc),
            icon = Icons.Rounded.NewReleases,
            onClick = { uriHandler.openUri(releaseUrl) },
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
            onClick = { uriHandler.openUri(matrixUrl) },
        )
        SelectionCard(
            title = stringResource(Res.string.credits),
            description = stringResource(Res.string.credits_desc),
            icon = Icons.Rounded.AutoAwesome,
            onClick = onOpenCredits,
        )

        PreferenceSubtitle(text = stringResource(Res.string.auto_update))

        ToggleCard(
            title = stringResource(Res.string.enable_auto_update),
            description = stringResource(Res.string.update_channel_desc),
            icon = Icons.Rounded.Update,
            checked = settings.autoUpdateEnabled,
        ) { checked -> onUpdate { it.copy(autoUpdateEnabled = checked) } }

        SelectionCard(
            title = stringResource(Res.string.update_channel),
            description = updateChannelLabel(settings.updateChannel),
            icon = Icons.Rounded.Update,
            onClick = { showChannelDialog = true },
        )

        SelectionCard(
            title = stringResource(Res.string.version),
            description = appVersion,
            icon = Icons.Rounded.Info,
            onClick = { clipboardManager.setText(AnnotatedString(appVersion)) },
        )
    }

    if (showChannelDialog) {
        ChoiceDialog(
            title = stringResource(Res.string.update_channel),
            options = updateChannelOptions(),
            selected = settings.updateChannel,
            onSelect = { value -> onUpdate { it.copy(updateChannel = value) } },
            onDismiss = { showChannelDialog = false },
        )
    }
}

@Composable
private fun updateChannelOptions(): List<Pair<String, Int>> =
    listOf(
        stringResource(Res.string.stable_channel) to UpdateChannelStable,
        stringResource(Res.string.pre_release_channel) to UpdateChannelPreview,
    )

@Composable
private fun updateChannelLabel(value: Int): String =
    when (value) {
        UpdateChannelPreview -> stringResource(Res.string.pre_release_channel)
        else -> stringResource(Res.string.stable_channel)
    }