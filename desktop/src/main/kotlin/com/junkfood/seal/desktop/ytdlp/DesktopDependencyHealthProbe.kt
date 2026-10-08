package com.junkfood.seal.desktop.ytdlp

import com.junkfood.seal.desktop.i18n.AndroidStrings
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.io.path.exists
import kotlin.io.path.isExecutable

enum class DesktopDependencyHealthStatus {
    Missing,
    Healthy,
    Broken,
}

data class DesktopDependencyHealth(
    val status: DesktopDependencyHealthStatus,
    val version: String? = null,
    val diagnostic: String? = null,
    val stdout: String = "",
    val stderr: String = "",
    val exitCode: Int? = null,
    val timedOut: Boolean = false,
) {
    val isHealthy: Boolean
        get() = status == DesktopDependencyHealthStatus.Healthy
}

internal data class DesktopDependencyProbeExecution(
    val finished: Boolean,
    val exitCode: Int?,
    val stdout: String,
    val stderr: String,
)

internal fun interface DesktopDependencyProbeRunner {
    fun execute(command: List<String>, timeoutMillis: Long): DesktopDependencyProbeExecution
}

internal class DesktopDependencyHealthProbe(
    private val runner: DesktopDependencyProbeRunner = ProcessDependencyProbeRunner,
    private val isWindows: Boolean = System.getProperty("os.name").lowercase().contains("win"),
    private val isExecutable: (Path) -> Boolean = { path -> path.isExecutable() },
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
) {
    private data class Fingerprint(
        val probeName: String,
        val absolutePath: String,
        val lastModifiedMillis: Long,
        val size: Long,
        val executable: Boolean,
    )

    private val cache = ConcurrentHashMap<Fingerprint, DesktopDependencyHealth>()

    fun probe(name: String, path: Path): DesktopDependencyHealth {
        val absolutePath = path.toAbsolutePath().normalize()
        if (!absolutePath.exists()) {
            invalidate(absolutePath)
            return DesktopDependencyHealth(
                status = DesktopDependencyHealthStatus.Missing,
                diagnostic = AndroidStrings.format("desktop_dependency_probe_missing", absolutePath),
            )
        }
        if (!Files.isRegularFile(absolutePath)) {
            invalidate(absolutePath)
            return if (absolutePath.exists()) {
                DesktopDependencyHealth(
                    status = DesktopDependencyHealthStatus.Broken,
                    diagnostic = AndroidStrings.format("desktop_dependency_probe_not_file", absolutePath),
                )
            } else {
                DesktopDependencyHealth(
                    status = DesktopDependencyHealthStatus.Missing,
                    diagnostic = AndroidStrings.format("desktop_dependency_probe_missing", absolutePath),
                )
            }
        }

        val probeName = name.lowercase(Locale.ROOT)
        val fingerprint =
            runCatching {
                val executable = isWindows || isExecutable(absolutePath)
                Fingerprint(
                    probeName = probeName,
                    absolutePath = absolutePath.toString(),
                    lastModifiedMillis = Files.getLastModifiedTime(absolutePath).toMillis(),
                    size = Files.size(absolutePath),
                    executable = executable,
                )
            }.getOrElse { error ->
                invalidate(absolutePath)
                return if (error is NoSuchFileException || !absolutePath.exists()) {
                    DesktopDependencyHealth(
                        status = DesktopDependencyHealthStatus.Missing,
                        diagnostic = AndroidStrings.format("desktop_dependency_probe_missing", absolutePath),
                    )
                } else {
                    DesktopDependencyHealth(
                        status = DesktopDependencyHealthStatus.Broken,
                        diagnostic =
                            AndroidStrings.format(
                                "desktop_dependency_probe_inspection_failed",
                                error.message ?: error.toString(),
                            ),
                    )
                }
            }
        cache.keys.removeIf {
            it.probeName == fingerprint.probeName &&
                it.absolutePath == fingerprint.absolutePath &&
                it != fingerprint
        }
        return cache.computeIfAbsent(fingerprint) {
            if (!fingerprint.executable) {
                DesktopDependencyHealth(
                    status = DesktopDependencyHealthStatus.Broken,
                    diagnostic = AndroidStrings.format("desktop_dependency_probe_not_executable", absolutePath),
                )
            } else {
                executeProbe(name, absolutePath)
            }
        }
    }

    fun invalidate(path: Path? = null) {
        if (path == null) {
            cache.clear()
            return
        }
        val normalized = path.toAbsolutePath().normalize().toString()
        cache.keys.removeIf { it.absolutePath == normalized }
    }

    private fun executeProbe(name: String, path: Path): DesktopDependencyHealth {
        val command = listOf(path.toString()) + probeArguments(name)
        val execution =
            runCatching { runner.execute(command, timeoutMillis) }
                .getOrElse { error ->
                    return if (!path.exists()) {
                        DesktopDependencyHealth(
                            status = DesktopDependencyHealthStatus.Missing,
                            diagnostic = AndroidStrings.format("desktop_dependency_probe_missing", path),
                        )
                    } else {
                        DesktopDependencyHealth(
                            status = DesktopDependencyHealthStatus.Broken,
                            diagnostic = error.message ?: error.toString(),
                        )
                    }
                }

        if (!execution.finished) {
            return DesktopDependencyHealth(
                status = DesktopDependencyHealthStatus.Broken,
                diagnostic = AndroidStrings.format("desktop_dependency_probe_timeout", timeoutMillis),
                stdout = execution.stdout,
                stderr = execution.stderr,
                exitCode = execution.exitCode,
                timedOut = true,
            )
        }
        if (execution.exitCode != 0) {
            return DesktopDependencyHealth(
                status = DesktopDependencyHealthStatus.Broken,
                diagnostic =
                    execution.stderr.lineSequence().firstOrNull { it.isNotBlank() }
                        ?: execution.stdout.lineSequence().firstOrNull { it.isNotBlank() }
                        ?: AndroidStrings.format("desktop_dependency_probe_exit", execution.exitCode),
                stdout = execution.stdout,
                stderr = execution.stderr,
                exitCode = execution.exitCode,
            )
        }

        val version =
            execution.stdout.lineSequence().firstOrNull { it.isNotBlank() }
                ?: execution.stderr.lineSequence().firstOrNull { it.isNotBlank() }
        return DesktopDependencyHealth(
            status = DesktopDependencyHealthStatus.Healthy,
            version = version?.trim(),
            stdout = execution.stdout,
            stderr = execution.stderr,
            exitCode = execution.exitCode,
        )
    }

    private fun probeArguments(name: String): List<String> =
        when (name.lowercase()) {
            "ffmpeg" -> listOf("-version")
            "aria2c" -> listOf("--version")
            else -> listOf("--version")
        }

    private companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 5_000L
    }
}

private object ProcessDependencyProbeRunner : DesktopDependencyProbeRunner {
    override fun execute(command: List<String>, timeoutMillis: Long): DesktopDependencyProbeExecution {
        val process = ProcessBuilder(command).start()
        val stdout = StringBuilder()
        val stderr = StringBuilder()
        val stdoutReader = consumeBounded(process.inputStream, stdout)
        val stderrReader = consumeBounded(process.errorStream, stderr)
        val finished = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)
        if (!finished) {
            process.destroy()
            if (!process.waitFor(250, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly()
                process.waitFor(1, TimeUnit.SECONDS)
            }
        }
        stdoutReader.join(1_000)
        stderrReader.join(1_000)
        return DesktopDependencyProbeExecution(
            finished = finished,
            exitCode = if (finished) process.exitValue() else null,
            stdout = stdout.toString(),
            stderr = stderr.toString(),
        )
    }

    private fun consumeBounded(stream: java.io.InputStream, target: StringBuilder): Thread =
        thread(start = true, isDaemon = true, name = "dependency-health-probe") {
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

    private const val MAX_CAPTURE_CHARS = 32_768
}
