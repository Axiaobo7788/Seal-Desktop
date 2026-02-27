package com.junkfood.seal.desktop.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import java.awt.Toolkit
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

private const val SYSTEM_THEME_POLL_INTERVAL_MS = 5000L

@Composable
fun DesktopSealTheme(
    themeState: DesktopThemeState,
    content: @Composable () -> Unit,
) {
    val prefs = themeState.preferences
    val composeSystemDark = isSystemInDarkTheme()
    var observedSystemDark by remember { mutableStateOf(composeSystemDark) }

    LaunchedEffect(prefs.darkThemeValue, composeSystemDark) {
        if (prefs.darkThemeValue != DarkThemePreference.FOLLOW_SYSTEM) return@LaunchedEffect
        observedSystemDark = detectSystemDarkTheme() ?: composeSystemDark
        while (true) {
            delay(SYSTEM_THEME_POLL_INTERVAL_MS)
            val latest = detectSystemDarkTheme() ?: composeSystemDark
            if (latest != observedSystemDark) {
                observedSystemDark = latest
            }
        }
    }

    val darkTheme =
        when (prefs.darkThemeValue) {
            DarkThemePreference.FOLLOW_SYSTEM -> observedSystemDark
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

private fun detectSystemDarkTheme(): Boolean? {
    detectAwtAppearance()?.let { return it }

    val osName = System.getProperty("os.name")?.lowercase(Locale.getDefault()).orEmpty()
    return when {
        osName.contains("linux") -> detectLinuxDarkTheme()
        osName.contains("mac") -> detectMacDarkTheme()
        osName.contains("win") -> detectWindowsDarkTheme()
        else -> null
    }
}

private fun detectAwtAppearance(): Boolean? {
    return runCatching {
        val value = Toolkit.getDefaultToolkit().getDesktopProperty("awt.application.appearance")
        when (value?.toString()?.lowercase(Locale.getDefault())) {
            "dark" -> true
            "light" -> false
            else -> null
        }
    }.getOrNull()
}

private fun detectLinuxDarkTheme(): Boolean? {
    val colorScheme = runCommand("gsettings", "get", "org.gnome.desktop.interface", "color-scheme")
    if (colorScheme != null) {
        val normalized = colorScheme.lowercase(Locale.getDefault())
        if (normalized.contains("prefer-dark") || normalized.contains("dark")) return true
        if (normalized.contains("default") || normalized.contains("light")) return false
    }

    val gtkTheme = runCommand("gsettings", "get", "org.gnome.desktop.interface", "gtk-theme")
    if (gtkTheme != null) {
        return gtkTheme.lowercase(Locale.getDefault()).contains("dark")
    }

    val envGtkTheme = System.getenv("GTK_THEME")
    if (!envGtkTheme.isNullOrBlank()) {
        return envGtkTheme.lowercase(Locale.getDefault()).contains("dark")
    }

    return null
}

private fun detectMacDarkTheme(): Boolean? {
    val output = runCommand("defaults", "read", "-g", "AppleInterfaceStyle") ?: return false
    return output.lowercase(Locale.getDefault()).contains("dark")
}

private fun detectWindowsDarkTheme(): Boolean? {
    val output =
        runCommand(
            "reg",
            "query",
            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
            "/v",
            "AppsUseLightTheme",
        ) ?: return null

    val normalized = output.lowercase(Locale.getDefault())
    val value =
        when {
            normalized.contains("0x0") || normalized.contains(" 0 ") -> 0
            normalized.contains("0x1") || normalized.contains(" 1 ") -> 1
            else -> null
        }
    return when (value) {
        0 -> true
        1 -> false
        else -> null
    }
}

private fun runCommand(vararg args: String): String? {
    return runCatching {
        val process = ProcessBuilder(*args).redirectErrorStream(true).start()
        if (!process.waitFor(800, TimeUnit.MILLISECONDS)) {
            process.destroyForcibly()
            return null
        }
        process.inputStream.bufferedReader().use { it.readText() }.trim().ifBlank { null }
    }.getOrNull()
}

private fun seedColorForIndex(index: Int): Color {
    val swatches =
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
