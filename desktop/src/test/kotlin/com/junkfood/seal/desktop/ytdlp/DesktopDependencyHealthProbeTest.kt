package com.junkfood.seal.desktop.ytdlp

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DesktopDependencyHealthProbeTest {
    @Test
    fun `successful probe captures version and output`() = withTemporaryBinary { binary ->
        val runner = QueueProbeRunner(success("2026.09.29\n"))
        val probe = DesktopDependencyHealthProbe(runner = runner, isWindows = true)

        val health = probe.probe("yt-dlp", binary)

        assertEquals(DesktopDependencyHealthStatus.Healthy, health.status)
        assertEquals("2026.09.29", health.version)
        assertEquals("2026.09.29\n", health.stdout)
        assertEquals(1, runner.calls)
        assertEquals(listOf(binary.toString(), "--version"), runner.commands.single())
    }

    @Test
    fun `nonzero probe is broken and preserves diagnostics`() = withTemporaryBinary { binary ->
        val runner =
            QueueProbeRunner(
                DesktopDependencyProbeExecution(
                    finished = true,
                    exitCode = 1,
                    stdout = "",
                    stderr = "ModuleNotFoundError: yt_dlp\n",
                ),
            )
        val health = DesktopDependencyHealthProbe(runner = runner, isWindows = true).probe("yt-dlp", binary)

        assertEquals(DesktopDependencyHealthStatus.Broken, health.status)
        assertEquals(1, health.exitCode)
        assertEquals("ModuleNotFoundError: yt_dlp", health.diagnostic)
        assertFalse(health.timedOut)
    }

    @Test
    fun `timed out probe is broken`() = withTemporaryBinary { binary ->
        val runner =
            QueueProbeRunner(
                DesktopDependencyProbeExecution(
                    finished = false,
                    exitCode = null,
                    stdout = "partial",
                    stderr = "",
                ),
            )
        val health =
            DesktopDependencyHealthProbe(
                runner = runner,
                isWindows = true,
                timeoutMillis = 25,
            ).probe("ffmpeg", binary)

        assertEquals(DesktopDependencyHealthStatus.Broken, health.status)
        assertTrue(health.timedOut)
        assertNull(health.exitCode)
        assertTrue(health.diagnostic.orEmpty().contains("25"))
        assertEquals(listOf(binary.toString(), "-version"), runner.commands.single())
    }

    @Test
    fun `missing executable is missing without starting a process`() {
        val runner = QueueProbeRunner(success("unused"))
        val missing = Path.of("build", "definitely-missing-dependency-${System.nanoTime()}")

        val health = DesktopDependencyHealthProbe(runner = runner, isWindows = true).probe("yt-dlp", missing)

        assertEquals(DesktopDependencyHealthStatus.Missing, health.status)
        assertEquals(0, runner.calls)
    }

    @Test
    fun `non executable file is broken without starting a process`() = withTemporaryBinary { binary ->
        val runner = QueueProbeRunner(success("unused"))
        val health =
            DesktopDependencyHealthProbe(
                runner = runner,
                isWindows = false,
                isExecutable = { false },
            ).probe("yt-dlp", binary)

        assertEquals(DesktopDependencyHealthStatus.Broken, health.status)
        assertNotNull(health.diagnostic)
        assertEquals(0, runner.calls)
    }

    @Test
    fun `same binary is probed independently for different tools`() = withTemporaryBinary { binary ->
        val runner = QueueProbeRunner(success("yt-dlp"), success("ffmpeg"))
        val probe = DesktopDependencyHealthProbe(runner = runner, isWindows = true)

        assertEquals("yt-dlp", probe.probe("yt-dlp", binary).version)
        assertEquals("ffmpeg", probe.probe("ffmpeg", binary).version)
        assertEquals(2, runner.calls)
    }

    @Test
    fun `binary removed while fingerprinting is reported missing`() = withTemporaryBinary { binary ->
        val runner = QueueProbeRunner(success("unused"))
        val probe =
            DesktopDependencyHealthProbe(
                runner = runner,
                isWindows = false,
                isExecutable = { candidate ->
                    Files.deleteIfExists(candidate)
                    true
                },
            )

        val health = probe.probe("yt-dlp", binary)

        assertEquals(DesktopDependencyHealthStatus.Missing, health.status)
        assertEquals(0, runner.calls)
    }

    @Test
    fun `unchanged binary uses cache and changed binary is reprobed healthy to broken`() =
        withTemporaryBinary { binary ->
            val runner = QueueProbeRunner(success("healthy"), failure("broken"))
            val probe = DesktopDependencyHealthProbe(runner = runner, isWindows = true)

            assertEquals(DesktopDependencyHealthStatus.Healthy, probe.probe("yt-dlp", binary).status)
            assertEquals(DesktopDependencyHealthStatus.Healthy, probe.probe("yt-dlp", binary).status)
            assertEquals(1, runner.calls)

            binary.writeText("changed-size")
            assertEquals(DesktopDependencyHealthStatus.Broken, probe.probe("yt-dlp", binary).status)
            assertEquals(2, runner.calls)
        }

    @Test
    fun `changed binary is reprobed broken to healthy`() = withTemporaryBinary { binary ->
        val runner = QueueProbeRunner(failure("broken"), success("healthy"))
        val probe = DesktopDependencyHealthProbe(runner = runner, isWindows = true)

        assertEquals(DesktopDependencyHealthStatus.Broken, probe.probe("ffmpeg", binary).status)
        binary.writeText("replacement-with-different-size")
        assertEquals(DesktopDependencyHealthStatus.Healthy, probe.probe("ffmpeg", binary).status)
        assertEquals(2, runner.calls)
    }

    @Test
    fun `explicit invalidation reprobes an unchanged binary`() = withTemporaryBinary { binary ->
        val runner = QueueProbeRunner(success("first"), success("second"))
        val probe = DesktopDependencyHealthProbe(runner = runner, isWindows = true)

        assertEquals("first", probe.probe("aria2c", binary).version)
        probe.invalidate(binary)
        assertEquals("second", probe.probe("aria2c", binary).version)
        assertEquals(2, runner.calls)
    }

    private class QueueProbeRunner(vararg results: DesktopDependencyProbeExecution) : DesktopDependencyProbeRunner {
        private val results = ArrayDeque(results.toList())
        val commands = mutableListOf<List<String>>()
        var calls: Int = 0
            private set

        override fun execute(command: List<String>, timeoutMillis: Long): DesktopDependencyProbeExecution {
            calls += 1
            commands += command
            return results.removeFirst()
        }
    }

    private fun success(output: String) =
        DesktopDependencyProbeExecution(
            finished = true,
            exitCode = 0,
            stdout = output,
            stderr = "",
        )

    private fun failure(error: String) =
        DesktopDependencyProbeExecution(
            finished = true,
            exitCode = 1,
            stdout = "",
            stderr = error,
        )

    private inline fun withTemporaryBinary(block: (Path) -> Unit) {
        val directory = Files.createTempDirectory("seal-dependency-probe-test")
        try {
            val binary = directory.resolve("tool")
            binary.writeText("initial")
            block(binary)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
