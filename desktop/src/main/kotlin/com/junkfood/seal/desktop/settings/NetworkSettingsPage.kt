package com.junkfood.seal.desktop.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SignalWifi4Bar
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.runtime.Composable
import com.junkfood.seal.desktop.ytdlp.DesktopYtDlpPaths
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.cookies
import com.junkfood.seal.shared.generated.resources.force_ipv4
import com.junkfood.seal.shared.generated.resources.force_ipv4_desc
import com.junkfood.seal.shared.generated.resources.network
import com.junkfood.seal.shared.generated.resources.proxy
import com.junkfood.seal.shared.generated.resources.proxy_desc
import com.junkfood.seal.shared.generated.resources.rate_limit
import com.junkfood.seal.shared.generated.resources.rate_limit_desc
import com.junkfood.seal.shared.generated.resources.network_settings_desc
import org.jetbrains.compose.resources.stringResource
import com.junkfood.seal.util.DownloadPreferences

@Composable
internal fun NetworkSettingsPage(
    preferences: DownloadPreferences,
    onUpdate: ((DownloadPreferences) -> DownloadPreferences) -> Unit,
    onBack: () -> Unit,
) {
    val cookiePath = DesktopYtDlpPaths.cookiesFile().toAbsolutePath().toString()

    SettingsPageScaffold(title = stringResource(Res.string.network), onBack = onBack) {
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

        ToggleCard(
            title = stringResource(Res.string.cookies),
            description = cookiePath,
            icon = Icons.Rounded.Folder,
            checked = preferences.cookies,
        ) { checked -> onUpdate { it.copy(cookies = checked) } }
    }
}
