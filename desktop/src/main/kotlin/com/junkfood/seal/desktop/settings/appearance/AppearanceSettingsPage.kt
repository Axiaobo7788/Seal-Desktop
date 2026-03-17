package com.junkfood.seal.desktop.settings.appearance

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.layout.ExperimentalLayoutApi

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.junkfood.seal.desktop.settings.SelectionCard
import com.junkfood.seal.desktop.settings.SettingsPageScaffold
import com.junkfood.seal.desktop.theme.DarkThemePreference
import com.junkfood.seal.desktop.theme.DesktopThemeState
import com.junkfood.seal.desktop.theme.PaletteStylePreference
import com.junkfood.seal.desktop.theme.md3RolePreviewSwatches
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
import com.junkfood.seal.shared.generated.resources.preset
import com.junkfood.seal.shared.generated.resources.status_downloading
import com.junkfood.seal.shared.generated.resources.video_creator_sample_text
import com.junkfood.seal.shared.generated.resources.video_title_sample_text
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.stringResource

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
            DarkThemePreference.FOLLOW_SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            DarkThemePreference.ON -> true
            else -> false
        }

    val darkThemeDesc =
        when (prefs.darkThemeValue) {
            DarkThemePreference.FOLLOW_SYSTEM -> stringResource(Res.string.follow_system)
            DarkThemePreference.ON -> stringResource(Res.string.on)
            else -> stringResource(Res.string.off)
        }

    SettingsPageScaffold(title = stringResource(Res.string.look_and_feel), onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { AppearancePreviewCard(isDarkTheme = isDarkTheme) }
            AppearancePresetPager(
                swatches = appearanceSwatches,
                selectedIndex = prefs.seedColorIndex.coerceIn(0, appearanceSwatches.lastIndex),
                selectedStyle = prefs.paletteStyleIndex,
                onSelect = { index -> themeState.update { it.copy(seedColorIndex = index) } },
                onSelectStyle = { style -> themeState.update { it.copy(paletteStyleIndex = style) } },
                onTapStyle = { style ->
                    themeState.update {
                        it.copy(
                            dynamicColorEnabled = false,
                            paletteStyleIndex = style,
                        )
                    }
                },
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
            checked = prefs.darkThemeValue != DarkThemePreference.OFF,
            onCheckedChange = {
                themeState.update {
                    it.copy(
                        darkThemeValue =
                            if (prefs.darkThemeValue == DarkThemePreference.OFF) {
                                DarkThemePreference.FOLLOW_SYSTEM
                            } else {
                                DarkThemePreference.OFF
                            },
                    )
                }
            },
            onClick = onOpenDarkTheme,
        )

        SelectionCard(
            title = stringResource(Res.string.language),
            description = stringResource(Res.string.language_settings),
            icon = Icons.Outlined.Language,
            onClick = onOpenLanguage,
        )
    }
}

@Composable
private fun AppearancePreviewCard(isDarkTheme: Boolean) {
    val previewImagePath = remember { samplePreviewDrawables.random() }
    val containerColor =
        if (isDarkTheme) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        }
    val actionButtonBackground = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.68f)
    val actionButtonContent = MaterialTheme.colorScheme.secondaryContainer

    Surface(
        modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.large,
        color = containerColor,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                Image(
                    painter = painterResource(previewImagePath),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.68f),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.align(Alignment.TopStart).padding(vertical = 12.dp, horizontal = 8.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.status_downloading),
                        modifier = Modifier.padding(horizontal = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }

                Box(
                    modifier =
                        Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .drawBehind {
                                drawCircle(actionButtonBackground)
                            }
                            .align(Alignment.Center),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp).align(Alignment.Center),
                        tint = actionButtonContent,
                    )
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.68f),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
                ) {
                    Text(
                        text = "0.00 MB  00:00",
                        modifier = Modifier.padding(horizontal = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Column(modifier = Modifier.weight(1f).padding(12.dp)) {
                    Text(
                        text = stringResource(Res.string.video_title_sample_text),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(Res.string.video_creator_sample_text),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = {}, modifier = Modifier.align(Alignment.CenterVertically).offset(x = 4.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AppearancePresetPager(
    swatches: List<Color>,
    selectedIndex: Int,
    selectedStyle: Int,
    onSelect: (Int) -> Unit,
    onSelectStyle: (Int) -> Unit,
    onTapStyle: (Int) -> Unit,
) {
    val itemsPerPage = 3
    val totalItems = swatches.size + 1
    val pageCount = ((totalItems + itemsPerPage - 1) / itemsPerPage).coerceAtLeast(1)
    
    val activeIndex = if (selectedStyle == PaletteStylePreference.MONOCHROME) swatches.size else selectedIndex
    val initialPage = (activeIndex / itemsPerPage).coerceIn(0, maxOf(0, pageCount - 1))
    val pagerState = rememberPagerState(initialPage = initialPage) { pageCount }

    LaunchedEffect(selectedIndex, selectedStyle) {
        val targetIndex = if (selectedStyle == PaletteStylePreference.MONOCHROME) swatches.size else selectedIndex
        val targetPage = (targetIndex / itemsPerPage).coerceIn(0, maxOf(0, pageCount - 1))
        if (targetPage != pagerState.currentPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    val coroutineScope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                val offset = pagerState.currentPageOffsetFraction
                                val target = if (offset > 0.2f) pagerState.currentPage + 1
                                else if (offset < -0.2f) pagerState.currentPage - 1
                                else pagerState.currentPage
                                pagerState.animateScrollToPage(target.coerceIn(0, pageCount - 1))
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                pagerState.scrollBy(-dragAmount)
                            }
                        }
                    )
                },
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 16.dp
        ) { page ->
            val start = page * itemsPerPage
            val end = (start + itemsPerPage).coerceAtMost(totalItems)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            ) {
                for (index in start until end) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (index == swatches.size) {
                                StyleColorButton(
                                    seed = Color.Black,
                                    style = PaletteStylePreference.MONOCHROME,
                                    selected = selectedStyle == PaletteStylePreference.MONOCHROME,
                                    onClick = {
                                        onTapStyle(PaletteStylePreference.MONOCHROME)
                                    }
                                )
                            } else {
                                StyleColorButtons(
                                    seed = swatches[index],
                                    selectedStyle = if (index == activeIndex && selectedStyle != PaletteStylePreference.MONOCHROME) selectedStyle else -1,
                                    onSelectStyle = { style ->
                                        if (selectedIndex != index) {
                                            onSelect(index)
                                        }
                                        onTapStyle(style)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        PagerDots(
            total = pageCount,
            current = pagerState.currentPage,
            dotSize = 6.dp,
            spacing = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        )
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


@Composable
private fun StyleColorButtons(
    seed: Color,
    selectedStyle: Int,
    onSelectStyle: (Int) -> Unit,
) {
    listOf(
        PaletteStylePreference.TONAL_SPOT,
        PaletteStylePreference.SPRITZ,
        PaletteStylePreference.FRUIT_SALAD,
        PaletteStylePreference.VIBRANT,
    ).forEach { style ->
        StyleColorButton(
            seed = seed,
            style = style,
            selected = style == selectedStyle,
            onClick = { onSelectStyle(style) },
        )
    }
}

@Composable
private fun StyleColorButton(
    seed: Color,
    style: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val swatches = previewPalette(seed, style)
    val containerSize by animateDpAsState(targetValue = if (selected) 28.dp else 0.dp)
    val iconSize by animateDpAsState(targetValue = if (selected) 16.dp else 0.dp)
    val checkContainer = MaterialTheme.colorScheme.primaryContainer

    Surface(
        modifier =
            Modifier
                .padding(4.dp)
                .size(56.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        onClick = onClick,
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .drawBehind { drawCircle(swatches[0]) }
                        .align(Alignment.Center),
            ) {
                Surface(color = swatches[1], modifier = Modifier.align(Alignment.BottomStart).size(24.dp)) {}
                Surface(color = swatches[2], modifier = Modifier.align(Alignment.BottomEnd).size(24.dp)) {}
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .size(containerSize)
                            .drawBehind { drawCircle(checkContainer) },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        modifier = Modifier.size(iconSize).align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}


private fun previewPalette(seed: Color, style: Int): List<Color> {
    return md3RolePreviewSwatches(seed = seed, paletteStyleIndex = style)
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
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            VerticalDivider(modifier = Modifier.height(24.dp), color = MaterialTheme.colorScheme.outlineVariant)
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
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            VerticalDivider(modifier = Modifier.height(24.dp), color = MaterialTheme.colorScheme.outlineVariant)
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

private val samplePreviewDrawables =
    listOf(
        "drawable/sample.webp",
        "drawable/sample1.webp",
        "drawable/sample2.webp",
        "drawable/sample3.webp",
    )
