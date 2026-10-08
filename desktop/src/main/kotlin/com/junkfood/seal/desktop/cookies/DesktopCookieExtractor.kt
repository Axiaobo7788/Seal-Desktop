package com.junkfood.seal.desktop.cookies

import com.junkfood.seal.desktop.ytdlp.YtDlpFetcher
import java.io.InputStream
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

data class DesktopCookieProcessExecution(
    val finished: Boolean,
    val canceled: Boolean,
    val exitCode: Int?,
    val stdout: String,
    val stderr: String,
)

interface DesktopCookieProcessRunner {
    fun execute(command: List<String>, timeoutMillis: Long): DesktopCookieProcessExecution

    fun cancel()
}

sealed interface DesktopCookieExtractionResult {
    data class Success(
        val context: DesktopCookieContext.CachedFile,
        val validationTarget: DesktopCookieValidationTarget,
        val stats: DesktopCookiesStats,
        val exitCode: Int,
        val stdoutSummary: String,
        val stderrSummary: String,
        val permissionWarning: String?,
    ) : DesktopCookieExtractionResult

    data class Failure(
        val reason: Reason,
        val diagnostic: String,
        val exitCode: Int? = null,
        val stdoutSummary: String = "",
        val stderrSummary: String = "",
        val timedOut: Boolean = false,
    ) : DesktopCookieExtractionResult

    enum class Reason {
        InvalidUrl,
        DependencyUnavailable,
        ProcessFailed,
        TimedOut,
        Canceled,
        InvalidOutput,
        FileError,
    }
}

class DesktopCookieExtractor(
    private val binaryProvider: () -> Path = { YtDlpFetcher().ensureBinary() },
    private val runner: DesktopCookieProcessRunner = ProcessDesktopCookieRunner(),
    private val cache: DesktopCookieCache = DesktopCookieCache(),
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
) {
    fun extract(source: DesktopCookieContext.BrowserSource): DesktopCookieExtractionResult {
        val validationTarget =
            normalizeCookieValidationUrl(source.validationUrl).getOrElse {
                return failure(DesktopCookieExtractionResult.Reason.InvalidUrl, "Invalid validation URL")
            }
        val binary =
            runCatching { binaryProvider().toAbsolutePath().normalize() }.getOrElse { error ->
                return failure(
                    DesktopCookieExtractionResult.Reason.DependencyUnavailable,
                    error.message ?: error.toString(),
                )
            }
        val temporary = cache.createTemporaryTarget(source.targetFile)
        return try {
            val command =
                buildCookieExtractionCommand(
                    ytDlpPath = binary,
                    source = source.copy(validationUrl = validationTarget.url),
                    outputFile = temporary,
                )
            val execution = runner.execute(command, timeoutMillis)
            val stdout = sanitizeCookieDiagnostic(execution.stdout)
            val stderr = sanitizeCookieDiagnostic(execution.stderr)
            when {
                execution.canceled ->
                    failure(
                        DesktopCookieExtractionResult.Reason.Canceled,
                        "Cookie extraction canceled",
                        execution,
                        stdout,
                        stderr,
                    )
                !execution.finished ->
                    failure(
                        DesktopCookieExtractionResult.Reason.TimedOut,
                        "Cookie extraction timed out",
                        execution,
                        stdout,
                        stderr,
                        timedOut = true,
                    )
                execution.exitCode != 0 ->
                    failure(
                        DesktopCookieExtractionResult.Reason.ProcessFailed,
                        stderr.lineSequence().firstOrNull { it.isNotBlank() }
                            ?: stdout.lineSequence().firstOrNull { it.isNotBlank() }
                            ?: "yt-dlp exited with code ${execution.exitCode}",
                        execution,
                        stdout,
                        stderr,
                    )
                else -> {
                    val promoted = cache.promoteGeneratedFile(temporary, source.targetFile)
                    promoted.fold(
                        onSuccess = { fileResult ->
                            DesktopCookieExtractionResult.Success(
                                context =
                                    DesktopCookieContext.CachedFile(
                                        path = source.targetFile,
                                        userAgent = source.userAgent,
                                    ),
                                validationTarget = validationTarget,
                                stats = fileResult.stats,
                                exitCode = execution.exitCode,
                                stdoutSummary = stdout,
                                stderrSummary = stderr,
                                permissionWarning = fileResult.permissionWarning,
                            )
                        },
                        onFailure = { error ->
                            failure(
                                DesktopCookieExtractionResult.Reason.InvalidOutput,
                                error.message ?: error.toString(),
                                execution,
                                stdout,
                                stderr,
                            )
                        },
                    )
                }
            }
        } catch (error: Exception) {
            failure(DesktopCookieExtractionResult.Reason.FileError, error.message ?: error.toString())
        } finally {
            runCatching { java.nio.file.Files.deleteIfExists(temporary) }
        }
    }

    fun cancel() = runner.cancel()

    private fun failure(
        reason: DesktopCookieExtractionResult.Reason,
        diagnostic: String,
        execution: DesktopCookieProcessExecution? = null,
        stdout: String = "",
        stderr: String = "",
        timedOut: Boolean = false,
    ): DesktopCookieExtractionResult.Failure =
        DesktopCookieExtractionResult.Failure(
            reason = reason,
            diagnostic = sanitizeCookieDiagnostic(diagnostic).ifBlank { reason.name },
            exitCode = execution?.exitCode,
            stdoutSummary = stdout,
            stderrSummary = stderr,
            timedOut = timedOut,
        )

    private companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 30_000L
    }
}

internal fun buildCookieExtractionCommand(
    ytDlpPath: Path,
    source: DesktopCookieContext.BrowserSource,
    outputFile: Path,
): List<String> =
    buildList {
        add(ytDlpPath.toAbsolutePath().normalize().toString())
        add("--cookies-from-browser")
        add(source.browserArgument())
        add("--cookies")
        add(outputFile.toAbsolutePath().normalize().toString())
        source.userAgent?.trim()?.takeIf { it.isNotEmpty() }?.let {
            add("--add-header")
            add("User-Agent:$it")
        }
        add("--simulate")
        add("--ignore-errors")
        add(source.validationUrl)
    }

internal fun sanitizeCookieDiagnostic(text: String): String =
    text.lineSequence()
        .map { line ->
            when {
                line.trim().startsWith("#HttpOnly_") -> "[cookie data omitted]"
                line.count { it == '\t' } >= 6 -> "[cookie data omitted]"
                SENSITIVE_HEADER_PATTERN.containsMatchIn(line) ->
                    line.replace(SENSITIVE_HEADER_PATTERN) { match -> "${match.groupValues[1]}: [redacted]" }
                else -> line
            }
        }
        .joinToString("\n")
        .take(MAX_CAPTURE_CHARS)
        .trim()

private class ProcessDesktopCookieRunner : DesktopCookieProcessRunner {
    private val currentProcess = AtomicReference<Process?>()
    private val cancelRequested = AtomicBoolean(false)

    override fun execute(command: List<String>, timeoutMillis: Long): DesktopCookieProcessExecution {
        cancelRequested.set(false)
        val process = ProcessBuilder(command).start()
        currentProcess.set(process)
        val stdout = StringBuilder()
        val stderr = StringBuilder()
        val stdoutReader = consumeBounded(process.inputStream, stdout)
        val stderrReader = consumeBounded(process.errorStream, stderr)
        return try {
            val finished = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)
            if (!finished) terminate(process)
            stdoutReader.join(1_000)
            stderrReader.join(1_000)
            DesktopCookieProcessExecution(
                finished = finished,
                canceled = cancelRequested.get(),
                exitCode = if (finished) process.exitValue() else null,
                stdout = stdout.toString(),
                stderr = stderr.toString(),
            )
        } finally {
            currentProcess.compareAndSet(process, null)
        }
    }

    override fun cancel() {
        cancelRequested.set(true)
        currentProcess.get()?.let(::terminate)
    }

    private fun terminate(process: Process) {
        process.destroy()
        if (!process.waitFor(250, TimeUnit.MILLISECONDS)) {
            process.destroyForcibly()
            process.waitFor(1, TimeUnit.SECONDS)
        }
    }

    private fun consumeBounded(stream: InputStream, target: StringBuilder): Thread =
        thread(start = true, isDaemon = true, name = "desktop-cookie-extractor") {
            stream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    synchronized(target) {
                        if (target.length < MAX_CAPTURE_CHARS) {
                            val remaining = MAX_CAPTURE_CHARS - target.length
                            target.append(line.take(remaining)).append('\n')
                        }
                    }
                }
            }
        }
}

private val SENSITIVE_HEADER_PATTERN = Regex("(?i)\\b(cookie|set-cookie|authorization)\\s*:\\s*.*")
private const val MAX_CAPTURE_CHARS = 32_768
