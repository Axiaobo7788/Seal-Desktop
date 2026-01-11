package com.junkfood.seal.desktop.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Composable
fun DesktopSealTheme(
    themeState: DesktopThemeState,
    content: @Composable () -> Unit,
) {
    val prefs = themeState.preferences
    val darkTheme =
        when (prefs.darkThemeValue) {
            DarkThemePreference.FOLLOW_SYSTEM -> isSystemInDarkTheme()
            DarkThemePreference.ON -> true
            else -> false
        }

    val base = if (darkTheme) darkColorScheme() else lightColorScheme()
    val scheme =
        if (prefs.dynamicColorEnabled) {
            applySeedAccent(
                base = base,
                seed = seedColorForIndex(prefs.seedColorIndex),
                darkTheme = darkTheme,
            )
        } else {
            base
        }

    val finalScheme =
        if (darkTheme && prefs.highContrastEnabled) {
            scheme.copy(surface = Color.Black, background = Color.Black)
        } else {
            scheme
        }

    MaterialTheme(colorScheme = finalScheme, content = content)
}

private fun seedColorForIndex(index: Int): Color {
    val swatches =
        listOf(
            Color(0xFFB8C6FF),
            Color(0xFFE1C2FF),
            Color(0xFFF6D08B),
            Color(0xFF9AE6B4),
        )
    return swatches.getOrElse(index) { swatches.first() }
}

private fun applySeedAccent(base: ColorScheme, seed: Color, darkTheme: Boolean): ColorScheme {
    val onSeed = if (seed.luminance() > 0.55f) Color.Black else Color.White
    val container =
        if (darkTheme) {
            mix(seed, Color.Black, 0.55f)
        } else {
            mix(seed, Color.White, 0.70f)
        }
    val onContainer = if (container.luminance() > 0.55f) Color.Black else Color.White

    val secondary = mix(seed, base.secondary, 0.40f)
    val onSecondary = if (secondary.luminance() > 0.55f) Color.Black else Color.White

    val tertiary = mix(seed, base.tertiary, 0.50f)
    val onTertiary = if (tertiary.luminance() > 0.55f) Color.Black else Color.White

    return base.copy(
        primary = seed,
        onPrimary = onSeed,
        primaryContainer = container,
        onPrimaryContainer = onContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        tertiary = tertiary,
        onTertiary = onTertiary,
    )
}

private fun mix(a: Color, b: Color, t: Float): Color {
    val clamped = t.coerceIn(0f, 1f)
    return Color(
        red = a.red * (1f - clamped) + b.red * clamped,
        green = a.green * (1f - clamped) + b.green * clamped,
        blue = a.blue * (1f - clamped) + b.blue * clamped,
        alpha = 1f,
    )
}
