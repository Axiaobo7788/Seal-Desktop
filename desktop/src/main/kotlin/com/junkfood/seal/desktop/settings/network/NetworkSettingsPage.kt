package com.junkfood.seal.desktop.settings.network

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SignalWifi4Bar
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.runtime.Composable
import com.junkfood.seal.desktop.settings.PreferenceInfo
import com.junkfood.seal.desktop.settings.PreferenceSubtitle
import com.junkfood.seal.desktop.settings.SettingsPageScaffold
import com.junkfood.seal.desktop.settings.TextFieldCard
import com.junkfood.seal.desktop.settings.ToggleCard
import com.junkfood.seal.desktop.ytdlp.DesktopYtDlpPaths
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.advanced_settings
import com.junkfood.seal.shared.generated.resources.aria2
import com.junkfood.seal.shared.generated.resources.aria2_desc
import com.junkfood.seal.shared.generated.resources.concurrent_download
import com.junkfood.seal.shared.generated.resources.concurrent_download_desc
import com.junkfood.seal.shared.generated.resources.concurrent_download_num
import com.junkfood.seal.shared.generated.resources.cookies
import com.junkfood.seal.shared.generated.resources.cookies_desc
import com.junkfood.seal.shared.generated.resources.force_ipv4
import com.junkfood.seal.shared.generated.resources.force_ipv4_desc
import com.junkfood.seal.shared.generated.resources.general_settings
import com.junkfood.seal.shared.generated.resources.network
import com.junkfood.seal.shared.generated.resources.proxy
import com.junkfood.seal.shared.generated.resources.proxy_desc
import com.junkfood.seal.shared.generated.resources.rate_limit
import com.junkfood.seal.shared.generated.resources.rate_limit_desc
import com.junkfood.seal.util.DownloadPreferences
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NetworkSettingsPage(
    preferences: DownloadPreferences,
    onUpdate: ((DownloadPreferences) -> DownloadPreferences) -> Unit,
    onBack: () -> Unit,
) {
    val cookiePath = DesktopYtDlpPaths.cookiesFile().toAbsolutePath().toString()
    val concurrentDesc =
        if (preferences.concurrentFragments > 0)
            stringResource(Res.string.concurrent_download_num, preferences.concurrentFragments)
        else stringResource(Res.string.concurrent_download_desc)

    SettingsPageScaffold(title = stringResource(Res.string.network), onBack = onBack) {
        PreferenceSubtitle(text = stringResource(Res.string.general_settings))

        ToggleCard(
            title = stringResource(Res.string.rate_limit),
            description = stringResource(Res.string.rate_limit_desc),
            icon = Icons.Rounded.Speed,
            checked = preferences.rateLimit,
        ) { checked -> onUpdate { it.copy(rateLimit = checked) } }

        TextFieldCard(
            title = stringResource(Res.string.rate_limit),
            description = "KB/s",
            icon = Icons.Rounded.Speed,
            value = preferences.maxDownloadRate,
            enabled = preferences.rateLimit,
        ) { newValue -> onUpdate { it.copy(maxDownloadRate = newValue.filter { ch -> ch.isDigit() }) } }

        TextFieldCard(
            title = stringResource(Res.string.concurrent_download),
            description = concurrentDesc,
            icon = Icons.Rounded.Speed,
            value = preferences.concurrentFragments.toString(),
        ) { newValue ->
            val value = newValue.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0
            onUpdate { it.copy(concurrentFragments = value) }
        }

        ToggleCard(
            title = stringResource(Res.string.proxy),
            description = stringResource(Res.string.proxy_desc),
            icon = Icons.Rounded.SignalWifi4Bar,
            checked = preferences.proxy,
        ) { checked -> onUpdate { it.copy(proxy = checked) } }

        TextFieldCard(
            title = stringResource(Res.string.proxy),
            description = "http://127.0.0.1:7890",
            icon = Icons.Rounded.SignalWifi4Bar,
            value = preferences.proxyUrl,
            enabled = preferences.proxy,
        ) { newValue -> onUpdate { it.copy(proxyUrl = newValue) } }

        ToggleCard(
            title = stringResource(Res.string.force_ipv4),
            description = stringResource(Res.string.force_ipv4_desc),
            icon = Icons.Rounded.Info,
            checked = preferences.forceIpv4,
        ) { checked -> onUpdate { it.copy(forceIpv4 = checked) } }

        PreferenceSubtitle(text = stringResource(Res.string.advanced_settings))

        ToggleCard(
            title = stringResource(Res.string.aria2),
            description = stringResource(Res.string.aria2_desc),
            icon = Icons.Rounded.Info,
            checked = preferences.aria2c,
        ) { checked -> onUpdate { it.copy(aria2c = checked) } }

        ToggleCard(
            title = stringResource(Res.string.cookies),
            description = stringResource(Res.string.cookies_desc),
            icon = Icons.Rounded.Folder,
            checked = preferences.cookies,
        ) { checked -> onUpdate { it.copy(cookies = checked) } }

        if (preferences.cookies) {
            PreferenceInfo(text = cookiePath)
        }
    }
}
