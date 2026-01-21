package com.junkfood.seal.desktop.settings.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.junkfood.seal.desktop.settings.SelectionCard
import com.junkfood.seal.desktop.settings.SettingsPageScaffold
import com.junkfood.seal.desktop.settings.ToggleCard
import com.junkfood.seal.desktop.theme.DarkThemePreference
import com.junkfood.seal.desktop.theme.DesktopThemeState
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.dark_theme
import com.junkfood.seal.shared.generated.resources.dynamic_color
import com.junkfood.seal.shared.generated.resources.dynamic_color_desc
import com.junkfood.seal.shared.generated.resources.follow_system
import com.junkfood.seal.shared.generated.resources.language
import com.junkfood.seal.shared.generated.resources.language_settings
import com.junkfood.seal.shared.generated.resources.look_and_feel
import com.junkfood.seal.shared.generated.resources.off
import com.junkfood.seal.shared.generated.resources.on
import com.junkfood.seal.shared.generated.resources.video_creator_sample_text
import com.junkfood.seal.shared.generated.resources.video_title_sample_text
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppearanceSettingsPage(
    themeState: DesktopThemeState,
    onOpenDarkTheme: () -> Unit,
    onOpenLanguage: () -> Unit,
    onBack: () -> Unit,
) {
    val prefs = themeState.preferences

    val swatches =
        listOf(
            Color(0xFFB8C6FF),
            Color(0xFFE1C2FF),
            Color(0xFFF6D08B),
            Color(0xFF9AE6B4),
        )

    val darkThemeDesc =
        when (prefs.darkThemeValue) {
            DarkThemePreference.FOLLOW_SYSTEM -> stringResource(Res.string.follow_system)
            DarkThemePreference.ON -> stringResource(Res.string.on)
            else -> stringResource(Res.string.off)
        }

    SettingsPageScaffold(title = stringResource(Res.string.look_and_feel), onBack = onBack) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 1.dp,
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f),
                                ),
                            ),
                        )
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 2.dp,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.video_title_sample_text),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(Res.string.video_creator_sample_text),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    swatches.forEachIndexed { index, color ->
                        val selected = index == prefs.seedColorIndex
                        Box(
                            modifier =
                                Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color =
                                            if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape,
                                    )
                                    .clickable {
                                        themeState.update { it.copy(seedColorIndex = index) }
                                    },
                        )
                    }
                }
            }
        }

        ToggleCard(
            title = stringResource(Res.string.dynamic_color),
            description = stringResource(Res.string.dynamic_color_desc),
            icon = Icons.Rounded.Palette,
            checked = prefs.dynamicColorEnabled,
        ) { checked -> themeState.update { it.copy(dynamicColorEnabled = checked) } }

        SelectionCard(
            title = stringResource(Res.string.dark_theme),
            description = darkThemeDesc,
            icon = Icons.Rounded.Palette,
            onClick = onOpenDarkTheme,
        )

        SelectionCard(
            title = stringResource(Res.string.language),
            description = stringResource(Res.string.language_settings),
            icon = Icons.Rounded.Language,
            onClick = onOpenLanguage,
        )
    }
}
