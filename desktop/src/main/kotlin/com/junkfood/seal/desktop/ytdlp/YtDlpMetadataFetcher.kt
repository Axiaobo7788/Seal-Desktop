package com.junkfood.seal.desktop.ytdlp

import com.junkfood.seal.desktop.i18n.AndroidStrings
import com.junkfood.seal.desktop.cookies.DesktopCookieContext
import com.junkfood.seal.desktop.cookies.ytDlpArguments
import com.junkfood.seal.util.VideoInfo
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class YtDlpMetadataFetcher(
    private val fetcher: YtDlpFetcher = YtDlpFetcher(),
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val runner: MetadataProcessRunner = JvmMetadataProcessRunner(),
    private val timeoutMillis: Long = 60_000,
    private val pathsProvider: () -> Pair<Path, Path?> = {
        val dependencies = fetcher.ensureDependencies()
        requireNotNull(dependencies.ytDlp).path to dependencies.ffmpeg?.path
    },
) {
    fun fetch(
        url: String,
        proxyUrl: String? = null,
        cookieContext: DesktopCookieContext = DesktopCookieContext.Disabled(),
        extraEnv: Map<String, String> = emptyMap(),
        runtimeOptions: MetadataRuntimeOptions = MetadataRuntimeOptions(proxyUrl = proxyUrl),
    ): VideoInfo {
        val (binary, ffmpeg) = pathsProvider()
        val command =
            buildMetadataCommand(
                ytDlpPath = binary,
                ffmpegPath = ffmpeg,
                url = url,
                proxyUrl = proxyUrl,
                cookieContext = cookieContext,
                runtimeOptions = runtimeOptions,
            )
        val result = runner.execute(command, extraEnv, timeoutMillis)
        if (result.timedOut) throw IllegalStateException(AndroidStrings.get("desktop_metadata_timeout"))
        if (result.outputLimitExceeded) throw IllegalStateException(AndroidStrings.get("desktop_metadata_output_limit"))
        if (result.exitCode != 0) {
            throw YtDlpMetadataException(
                exitCode = result.exitCode ?: -1,
                stdout = result.stdout.take(32_768),
                stderr = result.stderr,
            )
        }
        return try {
            json.decodeFromString(result.stdout)
        } catch (error: SerializationException) {
            throw IllegalStateException(AndroidStrings.get("desktop_metadata_invalid_json"), error)
        }
    }

    suspend fun fetchCancellable(
        url: String,
        cookieContext: DesktopCookieContext = DesktopCookieContext.Disabled(),
        runtimeOptions: MetadataRuntimeOptions = MetadataRuntimeOptions(),
    ): VideoInfo =
        runInterruptible(Dispatchers.IO) {
            fetch(url, cookieContext = cookieContext, runtimeOptions = runtimeOptions)
        }
}

class YtDlpMetadataException(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
) : IllegalStateException(AndroidStrings.format("desktop_ytdlp_exit_error", exitCode, stderr.trim()))

internal fun buildMetadataCommand(
    ytDlpPath: Path,
    ffmpegPath: Path?,
    url: String,
    proxyUrl: String? = null,
    cookieContext: DesktopCookieContext = DesktopCookieContext.Disabled(),
    runtimeOptions: MetadataRuntimeOptions = MetadataRuntimeOptions(proxyUrl = proxyUrl),
): List<String> =
    buildList {
        add(ytDlpPath.toAbsolutePath().toString())
        ffmpegPath?.parent?.let { ffmpegLocation ->
            add("--ffmpeg-location")
            add(ffmpegLocation.toAbsolutePath().toString())
        }
        add("-J")
        add("--no-playlist")
        runtimeOptions.proxyUrl?.trim()?.takeIf { it.isNotBlank() }?.let {
            add("--proxy")
            add(it)
        }
        if (runtimeOptions.forceIpv4) add("-4")
        runtimeOptions.formatSorting?.let {
            add("-S")
            add(it)
        }
        addAll(cookieContext.ytDlpArguments())
        add("--")
        add(url)
    }
