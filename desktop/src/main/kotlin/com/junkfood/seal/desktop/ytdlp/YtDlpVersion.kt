package com.junkfood.seal.desktop.ytdlp

import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the yt-dlp version by executing `yt-dlp --version`.
 * Returns null on failure.
 */
suspend fun readYtDlpVersion(binary: Path): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val process =
                ProcessBuilder(binary.toAbsolutePath().toString(), "--version")
                    .redirectErrorStream(true)
                    .start()

            val output =
                BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }.trim()

            val code = process.waitFor()
            if (code == 0) output.takeIf { it.isNotBlank() } else null
        }.getOrNull()
    }
