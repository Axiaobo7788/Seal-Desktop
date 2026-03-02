package com.junkfood.seal.desktop.ytdlp

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
        extraEnv: Map<String, String> = emptyMap(),
    ): VideoInfo {
        val binary: Path = fetcher.ensureBinary()
        val command =
            buildList {
                add(binary.toAbsolutePath().toString())
                add("-J")
                add("--no-playlist")
                proxyUrl?.trim()?.takeIf { it.isNotBlank() }?.let {
                    add("--proxy")
                    add(it)
                }
                add(url)
            }
        val processBuilder = ProcessBuilder(command)
        if (extraEnv.isNotEmpty()) {
            processBuilder.environment().putAll(extraEnv)
        }
        val process = processBuilder.start()
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val exit = process.waitFor()
        if (exit != 0) {
            throw IllegalStateException("yt-dlp exited $exit: $stderr")
        }
        return json.decodeFromString(stdout)
    }
}
