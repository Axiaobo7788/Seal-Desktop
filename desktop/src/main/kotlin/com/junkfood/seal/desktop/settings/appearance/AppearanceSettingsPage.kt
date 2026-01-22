package com.junkfood.seal.desktop.settings.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.junkfood.seal.desktop.settings.SelectionCard
import com.junkfood.seal.desktop.settings.SettingsPageScaffold
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
import com.junkfood.seal.shared.generated.resources.status_downloading
import com.junkfood.seal.shared.generated.resources.video_creator_sample_text
import com.junkfood.seal.shared.generated.resources.video_title_sample_text
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collect

@Composable
fun AppearanceSettingsPage(
    themeState: DesktopThemeState,
    onOpenDarkTheme: () -> Unit,
    onOpenLanguage: () -> Unit,
    onBack: () -> Unit,
) {
    val prefs = themeState.preferences
    val isDarkTheme =
        when (prefs.darkThemeValue) {
            DarkThemePreference.FOLLOW_SYSTEM -> isSystemInDarkTheme()
            DarkThemePreference.ON -> true
            else -> false
        }

    val swatches = appearanceSwatches

    val darkThemeDesc =
        when (prefs.darkThemeValue) {
            DarkThemePreference.FOLLOW_SYSTEM -> stringResource(Res.string.follow_system)
            DarkThemePreference.ON -> stringResource(Res.string.on)
            else -> stringResource(Res.string.off)
        }

    SettingsPageScaffold(title = stringResource(Res.string.look_and_feel), onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppearancePreviewCard()

            AppearancePresetPager(
                swatches = swatches,
                selectedIndex = prefs.seedColorIndex.coerceIn(0, swatches.lastIndex),
                onSelect = { index -> themeState.update { it.copy(seedColorIndex = index) } },
            )
        }

        SwitchCard(
            title = stringResource(Res.string.dynamic_color),
            description = stringResource(Res.string.dynamic_color_desc),
            icon = Icons.Outlined.Colorize,
            checked = prefs.dynamicColorEnabled,
            onCheckedChange = { checked -> themeState.update { it.copy(dynamicColorEnabled = checked) } },
            onClick = { themeState.update { it.copy(dynamicColorEnabled = !prefs.dynamicColorEnabled) } },
        )

        DarkThemeSwitchCard(
            title = stringResource(Res.string.dark_theme),
            description = darkThemeDesc,
            icon = if (isDarkTheme) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
            checked = isDarkTheme,
            onCheckedChange = {
                val target = if (isDarkTheme) DarkThemePreference.OFF else DarkThemePreference.ON
                themeState.update { it.copy(darkThemeValue = target) }
            },
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

@Composable
private fun AppearancePreviewCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f),
                                ),
                            ),
                        ),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.68f),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.status_downloading),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }

                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    shape = CircleShape,
                    tonalElevation = 3.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                ) {
                    Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Outlined.Pause,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.68f),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                ) {
                    Text(
                        text = "0.00 MB  00:00",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppearancePresetPager(
    swatches: List<Color>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val pageSize = 4
    val pageCount = ((swatches.size + pageSize - 1) / pageSize).coerceAtLeast(1)
    val initialPage = (selectedIndex / pageSize).coerceIn(0, pageCount - 1)
    val pagerState = rememberPagerState(initialPage = initialPage) { pageCount }

    LaunchedEffect(selectedIndex) {
        val targetPage = (selectedIndex / pageSize).coerceIn(0, pageCount - 1)
        if (targetPage != pagerState.currentPage) {
            pagerState.scrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                val start = page * pageSize
                if (selectedIndex !in start until (start + pageSize)) {
                    onSelect(start.coerceAtMost(swatches.lastIndex))
                }
            }
    }

    HorizontalPager(
        modifier = Modifier.fillMaxWidth(),
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 12.dp),
        pageSpacing = 12.dp,
    ) { page ->
        val start = page * pageSize
        val end = (start + pageSize).coerceAtMost(swatches.size)
        val pageColors = swatches.subList(start, end)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            pageColors.forEachIndexed { index, seed ->
                val realIndex = start + index
                PalettePresetButton(
                    colors = previewPalette(seed),
                    selected = realIndex == selectedIndex,
                ) { onSelect(realIndex) }
            }
            if (pageColors.size < pageSize) {
                repeat(pageSize - pageColors.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }

    PagerDots(
        total = pageCount,
        current = pagerState.currentPage,
        dotSize = 6.dp,
        spacing = 6.dp,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp),
    )
}

@Composable
private fun PalettePresetButton(
    colors: List<Color>,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Surface(
        shape = RoundedCornerShape(14.dp),
        tonalElevation = if (selected) 2.dp else 0.dp,
        modifier =
            Modifier
                .size(64.dp)
                .border(width = if (selected) 2.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(14.dp))
                .clickable { onClick() },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth().background(colors[0]))
                Box(modifier = Modifier.weight(1f).fillMaxWidth().background(colors[1]))
            }
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth().background(colors[2]))
                Box(modifier = Modifier.weight(1f).fillMaxWidth().background(colors[3]))
            }
        }
    }
}

@Composable
private fun PagerDots(
    total: Int,
    current: Int,
    dotSize: Dp,
    spacing: Dp,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.Center) {
        repeat(total) { index ->
            val color = if (index == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            Box(
                modifier =
                    Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(color),
            )
            if (index != total - 1) Spacer(Modifier.width(spacing))
        }
    }
}

private fun previewPalette(seed: Color): List<Color> {
    val light = seed.copy(alpha = 0.55f)
    val lighter = seed.copy(alpha = 0.25f)
    val dark = seed.copy(alpha = 0.85f)
    return listOf(seed, light, dark, lighter)
}

@Composable
private fun DarkThemeSwitchCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.large,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Divider(modifier = Modifier.height(24.dp).width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun SwitchCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.large,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Divider(modifier = Modifier.height(24.dp).width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

private val appearanceSwatches =
    listOf(
        Color(0xFFB8C6FF),
        Color(0xFFE1C2FF),
        Color(0xFFF6D08B),
        Color(0xFF9AE6B4),
        Color(0xFFFFB4A2),
        Color(0xFF80D8FF),
        Color(0xFFFFD166),
        Color(0xFFA3D8F4),
        Color(0xFFB3E5FC),
        Color(0xFFC5CAE9),
        Color(0xFFFFCCBC),
        Color(0xFFB2F7EF),
    )
