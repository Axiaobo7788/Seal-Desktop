package com.junkfood.seal.desktop.settings.network

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SignalWifi4Bar
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.SettingsEthernet
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.OfflineBolt
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.junkfood.seal.desktop.network.DesktopProxyAutoDetector
import com.junkfood.seal.desktop.settings.DesktopAppSettings
import com.junkfood.seal.desktop.settings.PreferenceInfo
import com.junkfood.seal.desktop.settings.PreferenceSubtitle
import com.junkfood.seal.desktop.settings.SelectionCard
import com.junkfood.seal.desktop.settings.SettingsPageScaffold
import com.junkfood.seal.desktop.settings.SwitchWithDividerCard
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
    appSettings: DesktopAppSettings,
    onUpdateAppSettings: ((DesktopAppSettings) -> DesktopAppSettings) -> Unit,
    onOpenCookies: () -> Unit,
    onBack: () -> Unit,
) {
    val cookiePath = DesktopYtDlpPaths.cookiesFile().toAbsolutePath().toString()
    val concurrentDesc =
        if (preferences.concurrentFragments > 0)
            stringResource(Res.string.concurrent_download_num, preferences.concurrentFragments)
        else stringResource(Res.string.concurrent_download_desc)
    val detectedProxy =
        remember(appSettings.autoProxyEnabled) {
            if (appSettings.autoProxyEnabled)
                DesktopProxyAutoDetector.detectXrayProxy()
            else null
        }

    var showRateLimitDialog by remember { mutableStateOf(false) }
    var showProxyDialog by remember { mutableStateOf(false) }
    var showConcurrentDialog by remember { mutableStateOf(false) }

    SettingsPageScaffold(title = stringResource(Res.string.network), onBack = onBack) {
        PreferenceSubtitle(text = stringResource(Res.string.general_settings))

        SwitchWithDividerCard(
            title = stringResource(Res.string.rate_limit),
            description = stringResource(Res.string.rate_limit_desc),
            icon = Icons.Rounded.Speed,
            checked = preferences.rateLimit,
            onClick = { showRateLimitDialog = true },
            onCheckedChange = { checked -> onUpdate { it.copy(rateLimit = checked) } }
        )

        RateLimitDialog(
            visible = showRateLimitDialog,
            initialRate = preferences.maxDownloadRate,
            onDismissRequest = { showRateLimitDialog = false },
            onConfirm = { rate -> onUpdate { it.copy(maxDownloadRate = rate) } }
        )

        PreferenceSubtitle(text = stringResource(Res.string.advanced_settings))

        ToggleCard(
            title = stringResource(Res.string.aria2),
            description = stringResource(Res.string.aria2_desc),
            icon = Icons.Rounded.Bolt,
            checked = preferences.aria2c,
        ) { checked -> onUpdate { it.copy(aria2c = checked) } }

        SwitchWithDividerCard(
            title = stringResource(Res.string.proxy),
            description = stringResource(Res.string.proxy_desc),
            icon = Icons.Rounded.VpnKey,
            checked = preferences.proxy,
            onClick = { showProxyDialog = true },
            onCheckedChange = { checked -> onUpdate { it.copy(proxy = checked) } }
        )

        ProxyConfigurationDialog(
            visible = showProxyDialog,
            initialProxy = preferences.proxyUrl,
            onDismissRequest = { showProxyDialog = false },
            onConfirm = { url -> onUpdate { it.copy(proxyUrl = url) } }
        )

        ToggleCard(
            title = "自动检测本机代理（Xray）",
            description = "开启后自动检测本机 xray 端口并覆盖上方代理地址",
            icon = Icons.Rounded.SignalWifi4Bar,
            checked = appSettings.autoProxyEnabled,
            enabled = preferences.proxy,
        ) { checked -> onUpdateAppSettings { it.copy(autoProxyEnabled = checked) } }

        if (preferences.proxy && appSettings.autoProxyEnabled) {
            PreferenceInfo(text = "当前检测结果：${detectedProxy ?: "未检测到可用 xray 代理"}")
        }

        SelectionCard(
            title = stringResource(Res.string.concurrent_download),
            description = concurrentDesc,
            icon = Icons.Rounded.OfflineBolt,
            enabled = !preferences.aria2c,
            onClick = { showConcurrentDialog = true }
        )

        ConcurrentDownloadDialog(
            visible = showConcurrentDialog,
            initialFragments = preferences.concurrentFragments,
            onDismissRequest = { showConcurrentDialog = false },
            onConfirm = { frags -> onUpdate { it.copy(concurrentFragments = frags) } }
        )

        ToggleCard(
            title = stringResource(Res.string.force_ipv4),
            description = stringResource(Res.string.force_ipv4_desc),
            icon = Icons.Rounded.SettingsEthernet,
            checked = preferences.forceIpv4,
        ) { checked -> onUpdate { it.copy(forceIpv4 = checked) } }

        SelectionCard(
            title = stringResource(Res.string.cookies),
            description = stringResource(Res.string.cookies_desc),
            icon = Icons.Rounded.Cookie,
            onClick = onOpenCookies,
        )

        if (preferences.cookies) {
            PreferenceInfo(text = cookiePath)
        }
    }
}
