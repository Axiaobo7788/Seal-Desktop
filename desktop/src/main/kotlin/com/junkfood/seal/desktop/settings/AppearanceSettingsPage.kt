package com.junkfood.seal.desktop.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.dark_theme
import com.junkfood.seal.shared.generated.resources.dynamic_color
import com.junkfood.seal.shared.generated.resources.dynamic_color_desc
import com.junkfood.seal.shared.generated.resources.follow_system
import com.junkfood.seal.shared.generated.resources.language
import com.junkfood.seal.shared.generated.resources.language_settings
import com.junkfood.seal.shared.generated.resources.look_and_feel
import com.junkfood.seal.shared.generated.resources.video_creator_sample_text
import com.junkfood.seal.shared.generated.resources.video_title_sample_text
import org.jetbrains.compose.resources.stringResource

/**
 * 视觉和样式页面搬运自 Android：包含示例卡片、调色板、动态色彩开关、深色模式选项、语言入口。
 */
@Composable
fun AppearanceSettingsPage(onBack: () -> Unit) {
    var dynamicColor by rememberSaveable { mutableStateOf(true) }
    var darkTheme by rememberSaveable { mutableStateOf(false) }
    var selectedColor by rememberSaveable { mutableStateOf(0) }

    val swatches = listOf(
        Color(0xFFB8C6FF),
        Color(0xFFE1C2FF),
        Color(0xFFF6D08B),
        Color(0xFF9AE6B4),
    )

    SettingsPageScaffold(title = stringResource(Res.string.look_and_feel), onBack = onBack) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 1.dp,
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier
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
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = stringResource(Res.string.video_title_sample_text), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = stringResource(Res.string.video_creator_sample_text),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    swatches.forEachIndexed { index, color ->
                        val selected = index == selectedColor
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape,
                                )
                                .clickable { selectedColor = index },
                        )
                    }
                }
            }
        }

        ToggleCard(
            title = stringResource(Res.string.dynamic_color),
            description = stringResource(Res.string.dynamic_color_desc),
            icon = Icons.Rounded.Palette,
            checked = dynamicColor,
        ) { checked -> dynamicColor = checked }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = stringResource(Res.string.dark_theme), style = MaterialTheme.typography.titleMedium)
            DarkModeChoiceRow(
                title = stringResource(Res.string.follow_system),
                selected = !darkTheme,
                onSelect = { darkTheme = false },
            )
            DarkModeChoiceRow(
                title = stringResource(Res.string.dark_theme),
                selected = darkTheme,
                onSelect = { darkTheme = true },
            )
        }

        SelectionCard(
            title = stringResource(Res.string.language),
            description = stringResource(Res.string.language_settings),
            icon = Icons.Rounded.Language,
            onClick = {},
        )
    }
}

@Composable
private fun DarkModeChoiceRow(title: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable { onSelect() }
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
    }
}
