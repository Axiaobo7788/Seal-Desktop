package com.junkfood.seal.desktop.settings.interaction

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ViewComfy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.junkfood.seal.desktop.settings.DesktopAppSettings
import com.junkfood.seal.desktop.settings.DownloadTypeNone
import com.junkfood.seal.desktop.settings.DownloadTypePrevious
import com.junkfood.seal.desktop.settings.SettingsPageScaffold
import com.junkfood.seal.desktop.settings.SelectionCard
import com.junkfood.seal.desktop.settings.PreferenceSubtitle
import com.junkfood.seal.desktop.settings.PreferenceInfo
import com.junkfood.seal.desktop.settings.ChoiceDialog
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.download_type
import com.junkfood.seal.shared.generated.resources.interface_and_interaction
import com.junkfood.seal.shared.generated.resources.none
import com.junkfood.seal.shared.generated.resources.settings_before_download
import com.junkfood.seal.shared.generated.resources.settings_before_download_desc
import com.junkfood.seal.shared.generated.resources.use_previous_selection
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun InteractionSettingsPage(
    settings: DesktopAppSettings,
    onUpdate: ((DesktopAppSettings) -> DesktopAppSettings) -> Unit,
    onBack: () -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    SettingsPageScaffold(title = stringResource(Res.string.interface_and_interaction), onBack = onBack) {
        PreferenceSubtitle(text = stringResource(Res.string.settings_before_download))
        PreferenceInfo(text = stringResource(Res.string.settings_before_download_desc))

        SelectionCard(
            title = stringResource(Res.string.download_type),
            description = downloadTypeLabel(settings.downloadTypeInitialization),
            icon = Icons.Rounded.ViewComfy,
            onClick = { showDialog = true },
        )
    }

    if (showDialog) {
        ChoiceDialog(
            title = stringResource(Res.string.download_type),
            options = downloadTypeOptions(),
            selected = settings.downloadTypeInitialization,
            onSelect = { value -> onUpdate { it.copy(downloadTypeInitialization = value) } },
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
private fun downloadTypeOptions(): List<Pair<String, Int>> =
    listOf(
        stringResource(Res.string.use_previous_selection) to DownloadTypePrevious,
        stringResource(Res.string.none) to DownloadTypeNone,
    )

@Composable
private fun downloadTypeLabel(value: Int): String =
    when (value) {
        DownloadTypePrevious -> stringResource(Res.string.use_previous_selection)
        else -> stringResource(Res.string.none)
    }
