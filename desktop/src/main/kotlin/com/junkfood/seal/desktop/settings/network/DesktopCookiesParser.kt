package com.junkfood.seal.desktop.settings.network

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

data class DesktopCookiesStats(
    val cookieCount: Int,
    val siteCount: Int,
)

object DesktopCookiesParser {
    fun parseStats(path: Path): DesktopCookiesStats {
        if (!path.exists()) return DesktopCookiesStats(0, 0)

        val lines = path.readText().lines()
        val dataLines = lines.filter { it.isNotBlank() && !it.startsWith("#") }
        val domains = dataLines.mapNotNull { line ->
            line.split("\t").getOrNull(0)?.trim()?.takeIf { it.isNotEmpty() }
        }.toSet()

        return DesktopCookiesStats(
            cookieCount = dataLines.size,
            siteCount = domains.size,
        )
    }

    fun isValidCookiesFile(path: Path): Boolean {
        if (!path.exists()) return false
        val text = path.readText()
        return text.contains("# Netscape HTTP Cookie File") ||
            text.lines().any { line ->
                line.isNotBlank() && !line.startsWith("#") && line.split("\t").size >= 7
            }
    }
}
