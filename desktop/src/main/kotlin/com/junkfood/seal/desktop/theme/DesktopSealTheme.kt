package com.junkfood.seal.desktop.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.kyant.monet.PaletteStyle
import com.kyant.monet.TonalPalettes.Companion.toTonalPalettes
import com.kyant.monet.LocalTonalPalettes
import com.kyant.monet.dynamicColorScheme
import java.awt.Toolkit
import java.beans.PropertyChangeListener
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel

private const val SYSTEM_THEME_POLL_INTERVAL_MS = 5000L

private enum class DesktopOs {
    Linux,
    Mac,
    Windows,
    Other,
}

@Composable
fun DesktopSealTheme(
    themeState: DesktopThemeState,
    window: java.awt.Window? = null,
    content: @Composable () -> Unit,
) {
    val prefs = themeState.preferences
    val currentOs = remember { detectDesktopOs() }
    val composeSystemDark = isSystemInDarkTheme()
    var observedSystemDark by remember { mutableStateOf(composeSystemDark) }
    val portalThemeEvents = remember { Channel<Boolean?>(Channel.CONFLATED) }

    LaunchedEffect(portalThemeEvents, composeSystemDark) {
        for (event in portalThemeEvents) {
            val fromPortal = event
            observedSystemDark = fromPortal ?: composeSystemDark
        }
    }

    DisposableEffect(prefs.darkThemeValue, currentOs) {
        if (prefs.darkThemeValue != DarkThemePreference.FOLLOW_SYSTEM || currentOs != DesktopOs.Linux) {
            onDispose { }
        } else {
            val watcher = startPortalSettingsWatcher { isDark ->
                portalThemeEvents.trySend(isDark)
            }
            onDispose { watcher?.close() }
        }
    }

    DisposableEffect(prefs.darkThemeValue) {
        if (prefs.darkThemeValue != DarkThemePreference.FOLLOW_SYSTEM) {
            onDispose { }
        } else {
            val toolkit = runCatching { Toolkit.getDefaultToolkit() }.getOrNull()
            val listener =
                PropertyChangeListener { evt ->
                    val normalized = evt.newValue?.toString()?.lowercase(Locale.getDefault())
                    val next =
                        when (normalized) {
                            "dark" -> true
                            "light" -> false
                            else -> null
                        }
                    if (next != null && next != observedSystemDark) {
                        observedSystemDark = next
                    }
                }

            runCatching {
                toolkit?.addPropertyChangeListener("awt.application.appearance", listener)
            }

            onDispose {
                runCatching {
                    toolkit?.removePropertyChangeListener("awt.application.appearance", listener)
                }
            }
        }
    }

    LaunchedEffect(prefs.darkThemeValue, composeSystemDark, currentOs) {
        if (prefs.darkThemeValue != DarkThemePreference.FOLLOW_SYSTEM) return@LaunchedEffect
        observedSystemDark = detectSystemDarkTheme() ?: composeSystemDark
        if (currentOs == DesktopOs.Linux) {
            while (true) {
                delay(SYSTEM_THEME_POLL_INTERVAL_MS)
                val latest = detectSystemDarkTheme() ?: composeSystemDark
                if (latest != observedSystemDark) {
                    observedSystemDark = latest
                }
            }
        }
    }

    val darkTheme =
        when (prefs.darkThemeValue) {
            DarkThemePreference.FOLLOW_SYSTEM -> observedSystemDark
            DarkThemePreference.ON -> true
            else -> false
        }

    LaunchedEffect(window, darkTheme) {
        if (window is javax.swing.JFrame) {
            window.rootPane.putClientProperty("jetbrains.awt.windowDarkAppearance", darkTheme)
        } else if (window is javax.swing.JDialog) {
            window.rootPane.putClientProperty("jetbrains.awt.windowDarkAppearance", darkTheme)
        }
    }

    val dynamicSeedState = remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(currentOs, prefs.dynamicColorEnabled) {
        if (!prefs.dynamicColorEnabled) return@LaunchedEffect
        // 初始获取
        dynamicSeedState.value = detectSystemAccentColor()
        
        // 可选：对于某些系统，我们可以像深色模式那样轮询或者监听，这里简单加个定期刷新防止用户中途修改
        if (currentOs == DesktopOs.Linux) {
            while (true) {
                delay(SYSTEM_THEME_POLL_INTERVAL_MS)
                val latest = detectSystemAccentColor()
                if (latest != dynamicSeedState.value) {
                    dynamicSeedState.value = latest
                }
            }
        }
    }

    val dynamicSeed = dynamicSeedState.value
    val seed =
        when {
            prefs.dynamicColorEnabled && dynamicSeed != null -> dynamicSeed
            prefs.paletteStyleIndex == PaletteStylePreference.MONOCHROME -> Color.Black
            else -> seedColorForIndex(prefs.seedColorIndex)
        }

    val style = when(if (prefs.dynamicColorEnabled) PaletteStylePreference.TONAL_SPOT else prefs.paletteStyleIndex) {
        PaletteStylePreference.TONAL_SPOT -> PaletteStyle.TonalSpot
        PaletteStylePreference.SPRITZ -> PaletteStyle.Spritz
        PaletteStylePreference.FRUIT_SALAD -> PaletteStyle.FruitSalad
        PaletteStylePreference.VIBRANT -> PaletteStyle.Vibrant
        PaletteStylePreference.MONOCHROME -> PaletteStyle.Monochrome
        else -> PaletteStyle.TonalSpot
    }

    val tonalPalettes = seed.toTonalPalettes(style)
    
    CompositionLocalProvider(LocalTonalPalettes provides tonalPalettes) {
        val scheme = dynamicColorScheme(isLight = !darkTheme)
        val finalScheme =
            if (darkTheme && prefs.highContrastEnabled) {
                scheme.copy(
                    surface = Color.Black,
                    background = Color.Black,
                    surfaceContainerLowest = Color.Black,
                    surfaceContainerLow = scheme.surfaceContainerLowest,
                    surfaceContainer = scheme.surfaceContainerLow,
                    surfaceContainerHigh = scheme.surfaceContainerLow,
                    surfaceContainerHighest = scheme.surfaceContainer,
                )
            } else {
                scheme
            }

        MaterialTheme(colorScheme = finalScheme, content = content)
    }
}

private fun detectDesktopOs(): DesktopOs {
    val osName = System.getProperty("os.name")?.lowercase(Locale.getDefault()).orEmpty()
    return when {
        osName.contains("linux") -> DesktopOs.Linux
        osName.contains("mac") -> DesktopOs.Mac
        osName.contains("win") -> DesktopOs.Windows
        else -> DesktopOs.Other
    }
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
    readPortalColorScheme()?.let { return it }

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

private fun readPortalColorScheme(): Boolean? {
    val output =
        runCommand(
            "gdbus",
            "call",
            "--session",
            "--dest",
            "org.freedesktop.portal.Desktop",
            "--object-path",
            "/org/freedesktop/portal/desktop",
            "--method",
            "org.freedesktop.portal.Settings.Read",
            "org.freedesktop.appearance",
            "color-scheme",
        ) ?: return null

    val value = Regex("uint32\\s+(\\d+)").find(output)?.groupValues?.getOrNull(1)?.toIntOrNull()
    return colorSchemeValueToDarkMode(value)
}

private fun startPortalSettingsWatcher(onThemeChanged: (Boolean?) -> Unit): AutoCloseable? {
    return runCatching {
        val process =
            ProcessBuilder(
                "gdbus",
                "monitor",
                "--session",
                "--dest",
                "org.freedesktop.portal.Desktop",
                "--object-path",
                "/org/freedesktop/portal/desktop",
            )
                .redirectErrorStream(true)
                .start()

        val readerThread =
            Thread {
                process.inputStream.bufferedReader().useLines { lines ->
                    var pendingPortalSignal = false
                    lines.forEach { line ->
                        val normalized = line.lowercase(Locale.getDefault())
                        val matchesSignal =
                            normalized.contains("settingchanged") &&
                                normalized.contains("org.freedesktop.appearance") &&
                                normalized.contains("color-scheme")

                        if (matchesSignal) {
                            pendingPortalSignal = true
                        }

                        val raw = Regex("uint32\\s+(\\d+)").find(normalized)?.groupValues?.getOrNull(1)?.toIntOrNull()
                        if (pendingPortalSignal && raw != null) {
                            onThemeChanged(colorSchemeValueToDarkMode(raw))
                            pendingPortalSignal = false
                        }
                    }
                }
            }.apply {
                name = "portal-settings-watcher"
                isDaemon = true
                start()
            }

        AutoCloseable {
            runCatching { process.destroy() }
            runCatching { readerThread.interrupt() }
            runCatching {
                if (process.isAlive) {
                    process.destroyForcibly()
                }
            }
        }
    }.getOrNull()
}

private fun colorSchemeValueToDarkMode(value: Int?): Boolean? {
    return when (value) {
        1 -> true
        2 -> false
        else -> null
    }
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

private fun detectSystemAccentColor(): Color? {
    return when (detectDesktopOs()) {
        DesktopOs.Mac -> detectMacAccentColor()
        DesktopOs.Windows -> detectWindowsAccentColor()
        DesktopOs.Linux -> detectLinuxAccentColor()
        DesktopOs.Other -> null
    }
}

private fun detectMacAccentColor(): Color? {
    val raw = runCommand("defaults", "read", "-g", "AppleAccentColor") ?: return Color(0xFF0A84FF)
    val value = raw.trim().toIntOrNull() ?: return Color(0xFF0A84FF)
    return when (value) {
        -1 -> Color(0xFF8E8E93)
        0 -> Color(0xFFFF453A)
        1 -> Color(0xFFFF9F0A)
        2 -> Color(0xFFFFD60A)
        3 -> Color(0xFF30D158)
        4 -> Color(0xFF0A84FF)
        5 -> Color(0xFFBF5AF2)
        6 -> Color(0xFFFF375F)
        else -> Color(0xFF0A84FF)
    }
}

private fun detectWindowsAccentColor(): Color? {
    // 优先使用 Java AWT 原生提供的方法，避免创建子进程
    val awtColor = runCatching {
        Toolkit.getDefaultToolkit().getDesktopProperty("win.dwm.colorizationColor") as? java.awt.Color
    }.getOrNull()
    if (awtColor != null) {
        return Color(awtColor.red, awtColor.green, awtColor.blue, awtColor.alpha)
    }

    // 后备方案：命令行
    val output =
        runCommand(
            "reg",
            "query",
            "HKCU\\Software\\Microsoft\\Windows\\DWM",
            "/v",
            "AccentColor",
        ) ?: return null
    val hex = Regex("0x([0-9a-fA-F]+)").find(output)?.groupValues?.getOrNull(1) ?: return null
    val value = hex.toLongOrNull(16) ?: return null
    val bgr = (value and 0x00FFFFFF).toInt()
    val red = bgr and 0xFF
    val green = (bgr shr 8) and 0xFF
    val blue = (bgr shr 16) and 0xFF
    return Color(red = red / 255f, green = green / 255f, blue = blue / 255f, alpha = 1f)
}

private fun detectLinuxAccentColor(): Color? {
    val currentDesktop = System.getenv("XDG_CURRENT_DESKTOP")?.lowercase(Locale.getDefault()) ?: ""

    // 如果当前是在 KDE 环境，优先解析 kdeglobals 配置避免被 GNOME gsettings 默认值覆盖
    if (currentDesktop.contains("kde")) {
        try {
            val userHome = System.getProperty("user.home")
            val kdeGlobals = java.io.File(userHome, ".config/kdeglobals")
            if (kdeGlobals.exists()) {
                val lines = kdeGlobals.readLines()
                
                // 1. 优先尝试提取 KDE 的全局主题强调色 (如 AccentColor=140,149,75)
                val accentColorLine = lines.firstOrNull { it.trim().startsWith("AccentColor=") }
                if (accentColorLine != null) {
                    val rgbStr = accentColorLine.substringAfter("=").split(",")
                    if (rgbStr.size == 3) {
                        return Color(
                            red = rgbStr[0].trim().toFloat() / 255f,
                            green = rgbStr[1].trim().toFloat() / 255f,
                            blue = rgbStr[2].trim().toFloat() / 255f,
                            alpha = 1f
                        )
                    }
                }

                // 2. 如果未设置，兜底获取 [Colors:Selection] 模块下的 BackgroundNormal 颜色
                var inSelectionSection = false
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.startsWith("[Colors:Selection]")) {
                        inSelectionSection = true
                        continue
                    } else if (trimmed.startsWith("[")) {
                        inSelectionSection = false
                        continue
                    }
                    
                    if (inSelectionSection && trimmed.startsWith("BackgroundNormal=")) {
                        val rgbStr = trimmed.substringAfter("=").split(",")
                        if (rgbStr.size == 3) {
                            return Color(
                                red = rgbStr[0].trim().toFloat() / 255f,
                                green = rgbStr[1].trim().toFloat() / 255f,
                                blue = rgbStr[2].trim().toFloat() / 255f,
                                alpha = 1f
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 后备方案：检测 GNOME 方案（包含对 gsettings 返回默认值的处理）
    val output = runCommand("gsettings", "get", "org.gnome.desktop.interface", "accent-color")
    if (output != null) {
        val normalized = output.lowercase(Locale.getDefault())
        return when {
            normalized.contains("red") -> Color(0xFFE53935)
            normalized.contains("orange") -> Color(0xFFFB8C00)
            normalized.contains("yellow") -> Color(0xFFFBC02D)
            normalized.contains("green") -> Color(0xFF43A047)
            normalized.contains("teal") -> Color(0xFF00897B)
            normalized.contains("blue") -> Color(0xFF1E88E5)
            normalized.contains("purple") -> Color(0xFF8E24AA)
            normalized.contains("pink") -> Color(0xFFD81B60)
            normalized.contains("slate") -> Color(0xFF546E7A)
            else -> null
        }
    }

    return null
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

fun md3RolePreviewSwatches(
    seed: Color,
    paletteStyleIndex: Int,
): List<Color> {
    val style = when(paletteStyleIndex) {
        PaletteStylePreference.TONAL_SPOT -> PaletteStyle.TonalSpot
        PaletteStylePreference.SPRITZ -> PaletteStyle.Spritz
        PaletteStylePreference.FRUIT_SALAD -> PaletteStyle.FruitSalad
        PaletteStylePreference.VIBRANT -> PaletteStyle.Vibrant
        PaletteStylePreference.MONOCHROME -> PaletteStyle.Monochrome
        else -> PaletteStyle.TonalSpot
    }
    
    val palettes = seed.toTonalPalettes(style)
    return listOf(
        palettes.accent1(80.0),
        palettes.accent2(90.0),
        palettes.accent3(60.0)
    )
}
