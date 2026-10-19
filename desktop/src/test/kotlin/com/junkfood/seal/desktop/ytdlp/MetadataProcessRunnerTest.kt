package com.junkfood.seal.desktop.ytdlp

import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeout

class MetadataProcessRunnerTest {
    private fun command(mode: String): List<String> =
        listOf(
            Path.of(
                System.getProperty("java.home"),
                "bin",
                if (System.getProperty("os.name").startsWith("Windows")) "java.exe" else "java",
            ).toString(),
            "-cp",
            Path.of(MetadataTestProcess::class.java.protectionDomain.codeSource.location.toURI()).toString(),
            MetadataTestProcess::class.java.name,
            mode,
        )

    @Test
    fun `normal JSON and warning stderr both succeed`() {
        listOf("normal", "warning").forEach { mode ->
            val result = JvmMetadataProcessRunner().execute(command(mode), emptyMap(), 10_000)
            assertEquals(0, result.exitCode)
            assertTrue(result.stdout.contains("Test video"))
            assertFalse(result.timedOut)
            if (mode == "warning") assertTrue(result.stderr.contains("Warning"))
        }
    }

    @Test
    fun `huge stderr is drained concurrently and retained diagnostics are bounded`() {
        val result = JvmMetadataProcessRunner().execute(command("large"), emptyMap(), 10_000)
        assertEquals(0, result.exitCode)
        assertTrue(result.stdout.contains("Test video"))
        assertEquals(32_768, result.stderr.length)
    }

    @Test
    fun `nonzero exit and spawn failure remain observable`() {
        assertEquals(7, JvmMetadataProcessRunner().execute(command("error"), emptyMap(), 10_000).exitCode)
        assertFailsWith<IOException> {
            JvmMetadataProcessRunner(startProcess = { _, _ -> throw IOException("spawn failed") })
                .execute(emptyList(), emptyMap(), 100)
        }
    }

    @Test
    fun `timeout and coroutine cancellation reap the owned process`() =
        runBlocking {
            val owned = AtomicReference<Process>()
            val runner =
                JvmMetadataProcessRunner(startProcess = { command, _ ->
                    ProcessBuilder(command).start().also(owned::set)
                })
            assertTrue(runner.execute(command("never"), emptyMap(), 100).timedOut)
            assertFalse(owned.get().isAlive)

            val cancellationStarted = CompletableDeferred<Unit>()
            val cancelRunner =
                JvmMetadataProcessRunner(startProcess = { command, _ ->
                    ProcessBuilder(command).start().also {
                        owned.set(it)
                        cancellationStarted.complete(Unit)
                    }
                })
            val job = launch(Dispatchers.Default) {
                runInterruptible { cancelRunner.execute(command("never"), emptyMap(), 60_000) }
            }
            withTimeout(5_000) {
                cancellationStarted.await()
                job.cancelAndJoin()
            }
            assertFalse(owned.get().isAlive)
        }

    @Test
    fun `invalid JSON timeout and oversized stdout fail explicitly`() {
        fun fetcher(result: MetadataProcessResult) =
            YtDlpMetadataFetcher(
                runner = MetadataProcessRunner { _, _, _ -> result },
                pathsProvider = { Path.of("test-yt-dlp") to null },
            )

        assertEquals("Test", fetcher(MetadataProcessResult(0, "{\"title\":\"Test\"}", "warning")).fetch("https://example.org").title)
        assertFailsWith<IllegalStateException> {
            fetcher(MetadataProcessResult(0, "not json", "")).fetch("https://example.org")
        }
        assertFailsWith<IllegalStateException> {
            fetcher(MetadataProcessResult(0, "{}", "", outputLimitExceeded = true)).fetch("https://example.org")
        }
        assertFailsWith<IllegalStateException> {
            fetcher(MetadataProcessResult(null, "", "", timedOut = true)).fetch("https://example.org")
        }
        assertFailsWith<YtDlpMetadataException> {
            fetcher(MetadataProcessResult(3, "{}", "failure")).fetch("https://example.org")
        }
    }
}
