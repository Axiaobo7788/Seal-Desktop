package com.junkfood.seal.desktop.ytdlp

import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

data class MetadataProcessResult(
    val exitCode: Int?,
    val stdout: String,
    val stderr: String,
    val timedOut: Boolean = false,
    val outputLimitExceeded: Boolean = false,
)

fun interface MetadataProcessRunner {
    fun execute(command: List<String>, environment: Map<String, String>, timeoutMillis: Long): MetadataProcessResult
}

class JvmMetadataProcessRunner(
    private val startProcess: (List<String>, Map<String, String>) -> Process = { command, environment ->
        ProcessBuilder(command).apply { environment().putAll(environment) }.start()
    },
    private val stdoutLimit: Int = 16 * 1024 * 1024,
    private val stderrLimit: Int = 32_768,
) : MetadataProcessRunner {
    override fun execute(
        command: List<String>,
        environment: Map<String, String>,
        timeoutMillis: Long,
    ): MetadataProcessResult {
        require(timeoutMillis > 0)
        val process = startProcess(command, environment)
        val stdout = Capture(process.inputStream, stdoutLimit)
        val stderr = Capture(process.errorStream, stderrLimit)
        val readers = listOf(stdout.start(), stderr.start())
        try {
            val finished = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)
            if (!finished) terminate(process)
            readers.forEach { it.join(1_000) }
            if (readers.any { it.isAlive }) throw IOException("Metadata output pipes did not close")
            stdout.failure.get()?.let { throw it }
            stderr.failure.get()?.let { throw it }
            return MetadataProcessResult(
                exitCode = if (finished) process.exitValue() else null,
                stdout = stdout.text(),
                stderr = stderr.text(),
                timedOut = !finished,
                outputLimitExceeded = stdout.truncated,
            )
        } finally {
            val interrupted = Thread.interrupted()
            try {
                terminate(process)
                listOf(process.inputStream, process.errorStream, process.outputStream).forEach { runCatching { it.close() } }
                readers.forEach { it.join(1_000) }
            } finally {
                if (interrupted) Thread.currentThread().interrupt()
            }
        }
    }

    private fun terminate(process: Process) {
        if (!process.isAlive) return
        process.destroy()
        if (!process.waitFor(250, TimeUnit.MILLISECONDS)) {
            process.destroyForcibly()
            process.waitFor(1_000, TimeUnit.MILLISECONDS)
        }
    }

    private class Capture(
        private val stream: InputStream,
        private val limit: Int,
    ) {
        private val output = StringBuilder()
        val failure = AtomicReference<IOException?>()

        @Volatile
        var truncated = false
            private set

        fun start(): Thread =
            thread(isDaemon = true, name = "seal-metadata-output") {
                try {
                    stream.reader(Charsets.UTF_8).use { reader ->
                        val buffer = CharArray(4096)
                        while (true) {
                            val count = reader.read(buffer)
                            if (count < 0) break
                            synchronized(output) {
                                val accepted = minOf(count, limit - output.length)
                                output.append(buffer, 0, accepted)
                                if (accepted < count) truncated = true
                            }
                        }
                    }
                } catch (error: IOException) {
                    failure.set(error)
                }
            }

        fun text(): String = synchronized(output) { output.toString() }
    }
}
