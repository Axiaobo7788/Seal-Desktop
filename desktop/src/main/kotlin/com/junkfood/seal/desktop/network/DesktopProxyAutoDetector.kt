package com.junkfood.seal.desktop.network

import java.net.InetSocketAddress
import java.net.Socket

object DesktopProxyAutoDetector {
    private data class Candidate(
        val scheme: String,
        val host: String,
        val port: Int,
    ) {
        fun asUrl(): String = "$scheme://$host:$port"
    }

    fun detectXrayProxy(): String? {
        val candidates =
            listOf(
                Candidate("http", "127.0.0.1", 10809),
                Candidate("http", "127.0.0.1", 7890),
                Candidate("socks5", "127.0.0.1", 10808),
                Candidate("socks5", "127.0.0.1", 7891),
            )

        return candidates.firstOrNull { canConnect(it.host, it.port) }?.asUrl()
    }

    private fun canConnect(host: String, port: Int): Boolean {
        return runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 180)
            }
            true
        }.getOrDefault(false)
    }
}
