package com.junkfood.seal.desktop.ytdlp

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DesktopAuxiliaryDownloaderTest {
    @Test
    fun `linux app-private directory does not overlap user PATH bin`() {
        val directory =
            DesktopDependencyPaths.defaultAppPrivateDirectory(
                isWindows = false,
                isMac = false,
                userHome = "/home/tester",
                xdgDataHome = null,
                localAppData = null,
            )

        assertEquals(Path.of("/home/tester/.local/share/Seal/bin"), directory)
    }

    @Test
    fun `linux app-private directory honors absolute XDG data home`() {
        val directory =
            DesktopDependencyPaths.defaultAppPrivateDirectory(
                isWindows = false,
                isMac = false,
                userHome = "/home/tester",
                xdgDataHome = "/data/tester",
                localAppData = null,
            )

        assertEquals(Path.of("/data/tester/Seal/bin"), directory)
    }

    @Test
    fun `windows app-private directory stays below local app data`() {
        val directory =
            DesktopDependencyPaths.defaultAppPrivateDirectory(
                isWindows = true,
                isMac = false,
                userHome = "C:\\Users\\Tester",
                xdgDataHome = null,
                localAppData = "C:\\Users\\Tester\\AppData\\Local",
            )

        assertEquals(Path.of("C:\\Users\\Tester\\AppData\\Local", "Seal", "bin"), directory)
    }

    @Test
    fun `macOS app-private directory stays below application support`() {
        val directory =
            DesktopDependencyPaths.defaultAppPrivateDirectory(
                isWindows = false,
                isMac = true,
                userHome = "/Users/tester",
                xdgDataHome = null,
                localAppData = null,
            )

        assertEquals(Path.of("/Users/tester/Library/Application Support/Seal/bin"), directory)
    }

    @Test
    fun `macOS downloads ffmpeg and ffprobe from architecture-specific archives`() {
        val intel = DesktopAuxiliaryDownloader.getFfmpegDownloads(isWin = false, isMac = true, arch = "x86_64")
        val arm = DesktopAuxiliaryDownloader.getFfmpegDownloads(isWin = false, isMac = true, arch = "aarch64")

        assertEquals(listOf(setOf("ffmpeg"), setOf("ffprobe")), intel.map { it.tools })
        assertTrue(intel.all { "intel" in it.url })
        assertTrue(arm.all { "arm" in it.url })
    }

    @Test
    fun `windows architecture selects matching ffmpeg build`() {
        val x64 = DesktopAuxiliaryDownloader.getFfmpegDownloads(isWin = true, isMac = false, arch = "amd64").single()
        val x86 = DesktopAuxiliaryDownloader.getFfmpegDownloads(isWin = true, isMac = false, arch = "x86").single()
        val arm64 = DesktopAuxiliaryDownloader.getFfmpegDownloads(isWin = true, isMac = false, arch = "arm64").single()

        assertTrue("win64-gpl.zip" in x64.url)
        assertTrue("win32-gpl.zip" in x86.url)
        assertTrue("winarm64-gpl.zip" in arm64.url)
    }

    @Test
    fun `zip extraction supports root and nested tool entries`() {
        val root = createTempDirectory("seal-dependency-zip-test")
        val archive = root.resolve("tools.zip")
        val output = root.resolve("output")
        Files.createDirectories(output)
        try {
            ZipOutputStream(Files.newOutputStream(archive)).use { zip ->
                zip.putNextEntry(ZipEntry("ffmpeg"))
                zip.write("ffmpeg-test".toByteArray())
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("package/bin/ffprobe"))
                zip.write("ffprobe-test".toByteArray())
                zip.closeEntry()
            }

            DesktopAuxiliaryDownloader.extractZipAndMoveTools(
                zipFile = archive,
                targetDir = output,
                isWin = false,
                tools = setOf("ffmpeg", "ffprobe"),
            )

            assertTrue(output.resolve("ffmpeg").exists())
            assertTrue(output.resolve("ffprobe").exists())
        } finally {
            Files.walk(root).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach { it.deleteIfExists() }
            }
        }
    }

    @Test
    fun `zip extraction fails when a required tool is absent`() {
        val root = createTempDirectory("seal-dependency-zip-missing-test")
        val archive = root.resolve("tools.zip")
        try {
            ZipOutputStream(Files.newOutputStream(archive)).use { zip ->
                zip.putNextEntry(ZipEntry("ffmpeg"))
                zip.write("ffmpeg-test".toByteArray())
                zip.closeEntry()
            }

            assertFailsWith<IllegalStateException> {
                DesktopAuxiliaryDownloader.extractZipAndMoveTools(
                    zipFile = archive,
                    targetDir = root,
                    isWin = false,
                    tools = setOf("ffmpeg", "ffprobe"),
                )
            }
        } finally {
            Files.walk(root).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach { it.deleteIfExists() }
            }
        }
    }
}
