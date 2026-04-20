package com.junkfood.seal.desktop.network

import androidx.compose.runtime.snapshotFlow
import com.junkfood.seal.desktop.settings.DesktopAppSettingsState
import com.junkfood.seal.desktop.settings.DesktopSettingsState
import java.io.IOException
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.net.InetSocketAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object DesktopGlobalProxy {
    private var defaultSelector: ProxySelector? = null
    
    @Volatile
    private var currentProxy: Proxy? = null

    fun initialize(
        appSettingsState: DesktopAppSettingsState,
        settingsState: DesktopSettingsState,
        scope: CoroutineScope
    ) {
        if (defaultSelector == null) {
            defaultSelector = ProxySelector.getDefault()
            
            ProxySelector.setDefault(object : ProxySelector() {
                override fun select(uri: URI?): MutableList<Proxy> {
                    val p = currentProxy
                    if (p != null) {
                        return mutableListOf(p)
                    }
                    return defaultSelector?.select(uri) ?: mutableListOf(Proxy.NO_PROXY)
                }

                override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
                    defaultSelector?.connectFailed(uri, sa, ioe)
                }
            })
        }

        scope.launch {
            snapshotFlow {
                settingsState.preferences to appSettingsState.settings
            }.collectLatest { (prefs, appSettings) ->
                val finalUrlStr = DesktopProxyResolver.resolveProxyUrl(prefs, appSettings)
                currentProxy = parseProxyUrl(finalUrlStr)
            }
        }
    }

    private fun parseProxyUrl(urlStr: String?): Proxy? {
        if (urlStr.isNullOrBlank()) return null
        return try {
            val uri = URI.create(urlStr)
            if (uri.host == null) return null
            val type = when (uri.scheme?.lowercase()) {
                "socks", "socks4", "socks5" -> Proxy.Type.SOCKS
                else -> Proxy.Type.HTTP
            }
            val port = if (uri.port > 0) uri.port else 80
            Proxy(type, InetSocketAddress(uri.host, port))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}