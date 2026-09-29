package com.junkfood.seal.desktop.ytdlp

import com.junkfood.seal.desktop.i18n.AndroidStrings
import com.junkfood.seal.desktop.cookies.DesktopCookieContext
import com.junkfood.seal.desktop.cookies.ytDlpArguments
import com.junkfood.seal.util.VideoInfo
import java.nio.file.Path
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class YtDlpMetadataFetcher(
    private val fetcher: YtDlpFetcher = YtDlpFetcher(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun fetch(
        url: String,
        proxyUrl: String? = null,
        cookieContext: DesktopCookieContext = DesktopCookieContext.Disabled(),
        extraEnv: Map<String, String> = emptyMap(),
    ): VideoInfo {
        val dependencies = fetcher.ensureDependencies()
        val binary: Path = requireNotNull(dependencies.ytDlp).path
        val command =
            buildMetadataCommand(
                ytDlpPath = binary,
                ffmpegPath = dependencies.ffmpeg?.path,
                url = url,
                proxyUrl = proxyUrl,
                cookieContext = cookieContext,
            )
        val processBuilder = ProcessBuilder(command)
        if (extraEnv.isNotEmpty()) {
            processBuilder.environment().putAll(extraEnv)
        }
        val process = processBuilder.start()
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val exit = process.waitFor()
        if (exit != 0) {
            throw IllegalStateException(
                AndroidStrings.format("desktop_ytdlp_exit_error", exit, stderr.trim()),
            )
        }
        return json.decodeFromString(stdout)
    }
}

internal fun buildMetadataCommand(
    ytDlpPath: Path,
    ffmpegPath: Path?,
    url: String,
    proxyUrl: String? = null,
    cookieContext: DesktopCookieContext = DesktopCookieContext.Disabled(),
): List<String> =
    buildList {
        add(ytDlpPath.toAbsolutePath().toString())
        ffmpegPath?.parent?.let { ffmpegLocation ->
            add("--ffmpeg-location")
            add(ffmpegLocation.toAbsolutePath().toString())
        }
        add("-J")
        add("--no-playlist")
        proxyUrl?.trim()?.takeIf { it.isNotBlank() }?.let {
            add("--proxy")
            add(it)
        }
        addAll(cookieContext.ytDlpArguments())
        add(url)
    }
