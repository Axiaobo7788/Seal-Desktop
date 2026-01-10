package com.junkfood.seal.desktop.ytdlp

import com.junkfood.seal.download.DownloadPlan
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.concurrent.thread

/**
 * Execute a DownloadPlan via the yt-dlp binary (fetched per-platform).
 */
class DownloadPlanExecutor(
    private val fetcher: YtDlpFetcher = YtDlpFetcher(),
) {
    data class ExecutionConfig(
        val workingDirectory: Path? = null,
        val cookiesFile: Path? = null,
        val archiveFile: Path? = null,
        val extraEnv: Map<String, String> = emptyMap(),
        val redirectError: Boolean = false,
        val url: String,
    )

    data class ExecutionResult(
        val exitCode: Int,
        val stdout: List<String>,
        val stderr: List<String>,
    )

    class RunningProcess internal constructor(
        private val process: Process,
        private val stdoutSink: MutableList<String>,
        private val stderrSink: MutableList<String>,
        private val stdoutReader: Thread,
        private val stderrReader: Thread?,
    ) {
        fun cancel() {
            process.destroy()
        }

        fun waitForResult(): ExecutionResult {
            val exit = process.waitFor()
            stdoutReader.join()
            stderrReader?.join()
            return ExecutionResult(exit, stdoutSink.toList(), stderrSink.toList())
        }
    }

    fun start(
        plan: DownloadPlan,
        config: ExecutionConfig,
        onStdout: (String) -> Unit = {},
        onStderr: (String) -> Unit = {},
    ): RunningProcess {
        val process = launchProcess(plan, config)
        val stdout = mutableListOf<String>()
        val stderr = mutableListOf<String>()

        val stdoutReader = streamLines(process.inputStream, stdout, onStdout)
        val stderrReader =
            if (config.redirectError) null
            else streamLines(process.errorStream, stderr, onStderr)

        return RunningProcess(process, stdout, stderr, stdoutReader, stderrReader)
    }

    fun run(
        plan: DownloadPlan,
        config: ExecutionConfig,
        onStdout: (String) -> Unit = {},
        onStderr: (String) -> Unit = {},
    ): ExecutionResult = start(plan, config, onStdout, onStderr).waitForResult()

    fun defaultConfigFor(
        plan: DownloadPlan,
        url: String,
        paths: DesktopYtDlpPaths = DesktopYtDlpPaths,
    ): ExecutionConfig =
        ExecutionConfig(
            workingDirectory = paths.downloadDirectory(plan.downloadPathHint),
            cookiesFile = if (plan.needsCookiesFile) paths.cookiesFile() else null,
            archiveFile = if (plan.needsArchiveFile) paths.archiveFile() else null,
            url = url,
        )

    private fun launchProcess(plan: DownloadPlan, config: ExecutionConfig): Process {
        val binary = fetcher.ensureBinary()
        val command = buildCommand(binary, plan, config)
        val builder = ProcessBuilder(command)
        builder.redirectErrorStream(config.redirectError)
        config.workingDirectory?.let { builder.directory(it.toFile()) }
        if (config.extraEnv.isNotEmpty()) {
            builder.environment().putAll(config.extraEnv)
        }
        return builder.start()
    }

    private fun buildCommand(
        binary: Path,
        plan: DownloadPlan,
        config: ExecutionConfig,
    ): List<String> {
        val args = mutableListOf<String>()
        val absBinary = binary.toAbsolutePath()
        args += absBinary.toString()

        val ffmpegLocation = findBundledFfmpegLocation(absBinary)
        if (ffmpegLocation != null) {
            args += listOf("--ffmpeg-location", ffmpegLocation.toString())
        }

        args += plan.asCliArgs()
        if (plan.needsCookiesFile && config.cookiesFile != null) {
            args += listOf("--cookies", config.cookiesFile.toAbsolutePath().toString())
        }
        if (plan.needsArchiveFile && config.archiveFile != null) {
            args += listOf("--download-archive", config.archiveFile.toAbsolutePath().toString())
        }
        args += config.url
        return args
    }

    private fun findBundledFfmpegLocation(ytDlpBinary: Path): Path? {
        val os = System.getProperty("os.name")?.lowercase().orEmpty()
        val isWindows = os.contains("win")
        val ffmpegName = if (isWindows) "ffmpeg.exe" else "ffmpeg"

        val dir = ytDlpBinary.parent ?: return null
        val ffmpeg = dir.resolve(ffmpegName)
        return if (Files.exists(ffmpeg)) dir else null
    }

    private fun streamLines(
        stream: InputStream,
        sink: MutableList<String>,
        consumer: (String) -> Unit,
    ): Thread =
        thread(start = true, isDaemon = true, name = "yt-dlp-stream") {
            stream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    sink += line
                    consumer(line)
                }
            }
        }
}
