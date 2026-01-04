package com.junkfood.seal.desktop.ytdlp

import com.junkfood.seal.util.VideoInfo
import java.nio.file.Path
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class YtDlpMetadataFetcher(
    private val fetcher: YtDlpFetcher = YtDlpFetcher(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun fetch(url: String): VideoInfo {
        val binary: Path = fetcher.ensureBinary()
        val command = listOf(binary.toAbsolutePath().toString(), "-J", "--no-playlist", url)
        val process = ProcessBuilder(command).start()
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val exit = process.waitFor()
        if (exit != 0) {
            throw IllegalStateException("yt-dlp exited $exit: $stderr")
        }
        return json.decodeFromString(stdout)
    }
}
