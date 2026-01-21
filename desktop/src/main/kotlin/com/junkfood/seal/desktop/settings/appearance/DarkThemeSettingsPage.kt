package com.junkfood.seal.desktop.settings.appearance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.junkfood.seal.desktop.settings.SettingsPageScaffold
import com.junkfood.seal.desktop.settings.ToggleCard
import com.junkfood.seal.desktop.theme.DarkThemePreference
import com.junkfood.seal.desktop.theme.DesktopThemeState
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.additional_settings
import com.junkfood.seal.shared.generated.resources.dark_theme
import com.junkfood.seal.shared.generated.resources.follow_system
import com.junkfood.seal.shared.generated.resources.high_contrast
import com.junkfood.seal.shared.generated.resources.off
import com.junkfood.seal.shared.generated.resources.on
import org.jetbrains.compose.resources.stringResource

@Composable
fun DarkThemeSettingsPage(
    themeState: DesktopThemeState,
    onBack: () -> Unit,
) {
    val prefs = themeState.preferences
    SettingsPageScaffold(title = stringResource(Res.string.dark_theme), onBack = onBack) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DarkThemeChoiceRow(
                title = stringResource(Res.string.follow_system),
                selected = prefs.darkThemeValue == DarkThemePreference.FOLLOW_SYSTEM,
                onSelect = { themeState.update { it.copy(darkThemeValue = DarkThemePreference.FOLLOW_SYSTEM) } },
            )
            DarkThemeChoiceRow(
                title = stringResource(Res.string.on),
                selected = prefs.darkThemeValue == DarkThemePreference.ON,
                onSelect = { themeState.update { it.copy(darkThemeValue = DarkThemePreference.ON) } },
            )
            DarkThemeChoiceRow(
                title = stringResource(Res.string.off),
                selected = prefs.darkThemeValue == DarkThemePreference.OFF,
                onSelect = { themeState.update { it.copy(darkThemeValue = DarkThemePreference.OFF) } },
            )
        }

        Text(
            text = stringResource(Res.string.additional_settings),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp),
        )

        ToggleCard(
            title = stringResource(Res.string.high_contrast),
            description = null,
            icon = Icons.Outlined.Contrast,
            checked = prefs.highContrastEnabled,
        ) { checked -> themeState.update { it.copy(highContrastEnabled = checked) } }
    }
}

@Composable
private fun DarkThemeChoiceRow(title: String, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        onClick = onSelect,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RadioButton(selected = selected, onClick = onSelect)
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
