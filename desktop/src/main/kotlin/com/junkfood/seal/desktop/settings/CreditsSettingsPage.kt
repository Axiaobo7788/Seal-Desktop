package com.junkfood.seal.desktop.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.credits
import org.jetbrains.compose.resources.stringResource

private data class CreditEntry(
    val title: String,
    val url: String,
)

private val creditsList =
    listOf(
        CreditEntry("yt-dlp", "https://github.com/yt-dlp/yt-dlp"),
        CreditEntry("Read You", "https://github.com/Ashinch/ReadYou"),
        CreditEntry("youtubedl-android", "https://github.com/yausername/youtubedl-android"),
        CreditEntry("Termux", "https://github.com/termux/termux-app"),
        CreditEntry("FFmpeg", "https://ffmpeg.org/"),
        CreditEntry("Android Jetpack", "https://github.com/androidx/androidx"),
        CreditEntry("Kotlin", "https://kotlinlang.org/"),
        CreditEntry("Accompanist", "https://github.com/google/accompanist"),
        CreditEntry("Material Design 3", "https://m3.material.io/"),
        CreditEntry("Material Icons", "https://fonts.google.com/icons"),
        CreditEntry("Monet", "https://github.com/Kyant0/Monet"),
        CreditEntry("Material color utilities", "https://github.com/material-foundation/material-color-utilities"),
        CreditEntry("MMKV", "https://github.com/Tencent/MMKV"),
        CreditEntry("Coil", "https://github.com/coil-kt/coil"),
        CreditEntry("aria2", "https://github.com/aria2/aria2"),
        CreditEntry("OkHttp", "https://github.com/square/okhttp"),
        CreditEntry("material-motion-compose", "https://github.com/fornewid/material-motion-compose"),
        CreditEntry("unDraw", "https://undraw.co/"),
        CreditEntry("Icons8", "https://icons8.com/"),
    )

@Composable
internal fun CreditsSettingsPage(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    SettingsPageScaffold(title = stringResource(Res.string.credits), onBack = onBack) {
        PreferenceSubtitle(text = stringResource(Res.string.credits))
        creditsList.forEach { entry ->
            SelectionCard(
                title = entry.title,
                description = entry.url,
                icon = Icons.Rounded.AutoAwesome,
                onClick = { uriHandler.openUri(entry.url) },
            )
        }
    }
}