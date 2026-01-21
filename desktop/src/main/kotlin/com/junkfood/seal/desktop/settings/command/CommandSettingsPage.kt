package com.junkfood.seal.desktop.settings.command

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.runtime.Composable
import com.junkfood.seal.desktop.settings.DesktopAppSettings
import com.junkfood.seal.desktop.settings.PreferenceInfo
import com.junkfood.seal.desktop.settings.SettingsPageScaffold
import com.junkfood.seal.desktop.settings.TextFieldCard
import com.junkfood.seal.desktop.settings.ToggleCard
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.custom_command
import com.junkfood.seal.shared.generated.resources.custom_command_desc
import com.junkfood.seal.shared.generated.resources.custom_command_enabled_hint
import com.junkfood.seal.shared.generated.resources.custom_command_template
import com.junkfood.seal.shared.generated.resources.edit_template_desc
import com.junkfood.seal.shared.generated.resources.template_label
import com.junkfood.seal.shared.generated.resources.use_custom_command
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CommandSettingsPage(
    settings: DesktopAppSettings,
    onUpdate: ((DesktopAppSettings) -> DesktopAppSettings) -> Unit,
    onBack: () -> Unit,
) {
    SettingsPageScaffold(title = stringResource(Res.string.custom_command), onBack = onBack) {
        PreferenceInfo(text = stringResource(Res.string.custom_command_desc))

        ToggleCard(
            title = stringResource(Res.string.use_custom_command),
            description = stringResource(Res.string.custom_command_enabled_hint),
            icon = Icons.Rounded.Terminal,
            checked = settings.customCommandEnabled,
        ) { checked -> onUpdate { it.copy(customCommandEnabled = checked) } }

        TextFieldCard(
            title = stringResource(Res.string.template_label),
            description = stringResource(Res.string.custom_command_template),
            icon = Icons.Rounded.Code,
            value = settings.customCommandLabel,
            enabled = settings.customCommandEnabled,
        ) { newValue -> onUpdate { it.copy(customCommandLabel = newValue) } }

        TextFieldCard(
            title = stringResource(Res.string.custom_command_template),
            description = stringResource(Res.string.edit_template_desc),
            icon = Icons.Rounded.Code,
            value = settings.customCommandTemplate,
            enabled = settings.customCommandEnabled,
            singleLine = false,
            maxLines = 8,
        ) { newValue -> onUpdate { it.copy(customCommandTemplate = newValue) } }
    }
}
