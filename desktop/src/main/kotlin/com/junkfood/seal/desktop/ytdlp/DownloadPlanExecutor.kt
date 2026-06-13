package com.junkfood.seal.desktop.ytdlp

import com.junkfood.seal.download.DownloadPlan
import java.io.InputStream
import java.nio.file.Path
import kotlin.concurrent.thread

/**
 * Execute a DownloadPlan via the yt-dlp binary (fetched per-platform).
 */
class DownloadPlanExecutor(
    private val environmentPreferenceProvider: () -> Int = { DesktopDependencyResolver.defaultEnvironmentPreference() },
) {
    data class ExecutionConfig(
        val workingDirectory: Path? = null,
        val cookiesFile: Path? = null,
        val archiveFile: Path? = null,
        val extraEnv: Map<String, String> = emptyMap(),
        val redirectError: Boolean = false,
        val environmentPreference: Int? = null,
        val url: String,
    )

    data class ExecutionResult(
        val exitCode: Int,
        val stdout: List<String>,
        val stderr: List<String>,
    )

    data class CommandExecutionConfig(
        val workingDirectory: Path? = null,
        val extraEnv: Map<String, String> = emptyMap(),
        val redirectError: Boolean = false,
        val environmentPreference: Int? = null,
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

    fun startArgs(
        args: List<String>,
        config: CommandExecutionConfig,
        onStdout: (String) -> Unit = {},
        onStderr: (String) -> Unit = {},
    ): RunningProcess {
        val process = launchProcessForArgs(args, config)
        val stdout = mutableListOf<String>()
        val stderr = mutableListOf<String>()

        val stdoutReader = streamLines(process.inputStream, stdout, onStdout)
        val stderrReader =
            if (config.redirectError) null
            else streamLines(process.errorStream, stderr, onStderr)

        return RunningProcess(process, stdout, stderr, stdoutReader, stderrReader)
    }

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
        val dependencies = resolveDependencies(config.environmentPreference)
        val command = buildCommand(dependencies, plan, config)
        val builder = ProcessBuilder(command)
        builder.redirectErrorStream(config.redirectError)
        config.workingDirectory?.let { builder.directory(it.toFile()) }
        if (config.extraEnv.isNotEmpty()) {
            builder.environment().putAll(config.extraEnv)
        }
        return builder.start()
    }

    private fun launchProcessForArgs(args: List<String>, config: CommandExecutionConfig): Process {
        val dependencies = resolveDependencies(config.environmentPreference)
        val command = buildRawCommand(dependencies, args)
        val builder = ProcessBuilder(command)
        builder.redirectErrorStream(config.redirectError)
        config.workingDirectory?.let { builder.directory(it.toFile()) }
        if (config.extraEnv.isNotEmpty()) {
            builder.environment().putAll(config.extraEnv)
        }
        return builder.start()
    }

    private fun buildCommand(
        dependencies: DesktopDependencyResolution,
        plan: DownloadPlan,
        config: ExecutionConfig,
    ): List<String> {
        val args = mutableListOf<String>()
        val absBinary = dependencies.ytDlp!!.path.toAbsolutePath()
        args += absBinary.toString()

        dependencies.ffmpeg!!.path.parent?.let { ffmpegLocation ->
            args += listOf("--ffmpeg-location", ffmpegLocation.toAbsolutePath().toString())
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

    private fun buildRawCommand(dependencies: DesktopDependencyResolution, args: List<String>): List<String> {
        val command = mutableListOf<String>()
        val absBinary = dependencies.ytDlp!!.path.toAbsolutePath()
        command += absBinary.toString()

        dependencies.ffmpeg!!.path.parent?.let { ffmpegLocation ->
            command += listOf("--ffmpeg-location", ffmpegLocation.toAbsolutePath().toString())
        }

        command += args
        return command
    }

    private fun resolveDependencies(environmentPreference: Int?): DesktopDependencyResolution =
        DesktopDependencyResolver.requireComplete(environmentPreference ?: environmentPreferenceProvider())

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
