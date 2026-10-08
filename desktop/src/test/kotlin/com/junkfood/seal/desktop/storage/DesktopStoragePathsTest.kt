package com.junkfood.seal.desktop.storage

import com.junkfood.seal.desktop.paths.DesktopPathEnvironment
import com.junkfood.seal.desktop.paths.resolveActiveStateDirectory
import com.junkfood.seal.desktop.paths.resolveDesktopAppPathLayout
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopStoragePathsTest {
    @Test
    fun `system property takes precedence over environment state directory`() {
        val layout = resolveDesktopAppPathLayout(
            environment(
                stateDirectoryProperty = " /property/state ",
                stateDirectoryEnvironment = "/environment/state",
                xdgStateHome = "/xdg/state",
            )
        )

        assertEquals(Path.of("/property/state/seal"), layout.stateDirectory)
        assertTrue(layout.stateOverrideUsed)
    }

    @Test
    fun `environment state directory supports packaged smoke tests`() {
        val layout = resolveDesktopAppPathLayout(
            environment(
                stateDirectoryProperty = " ",
                stateDirectoryEnvironment = " /isolated/smoke-state ",
                xdgStateHome = "/xdg/state",
            )
        )

        assertEquals(Path.of("/isolated/smoke-state/seal"), layout.stateDirectory)
        assertTrue(layout.stateOverrideUsed)
    }

    @Test
    fun `auxiliary property takes precedence over environment directory`() {
        val layout =
            resolveDesktopAppPathLayout(
                environment(
                    auxiliaryDirectoryProperty = " /property/bin ",
                    auxiliaryDirectoryEnvironment = "/environment/bin",
                )
            )

        assertEquals(Path.of("/property/bin"), layout.auxiliaryBinariesDirectory)
    }

    @Test
    fun `linux uses XDG state cache and data roots`() {
        val layout = resolveDesktopAppPathLayout(
            environment(
                xdgStateHome = " /xdg/state ",
                xdgCacheHome = "/xdg/cache",
                xdgDataHome = "/xdg/data",
            )
        )

        assertEquals(Path.of("/xdg/state/seal"), layout.stateDirectory)
        assertEquals(Path.of("/xdg/cache/seal"), layout.cacheDirectory)
        assertEquals(Path.of("/xdg/data/Seal"), layout.dataDirectory)
        assertEquals(Path.of("/xdg/data/Seal/bin"), layout.auxiliaryBinariesDirectory)
    }

    @Test
    fun `relative XDG paths fall back to home directories`() {
        val layout =
            resolveDesktopAppPathLayout(
                environment(
                    xdgStateHome = "relative-state",
                    xdgCacheHome = "relative-cache",
                    xdgDataHome = "relative-data",
                )
            )

        assertEquals(Path.of("/home/test/.local/state/seal"), layout.stateDirectory)
        assertEquals(Path.of("/home/test/.cache/seal"), layout.cacheDirectory)
        assertEquals(Path.of("/home/test/.local/share/Seal"), layout.dataDirectory)
    }

    @Test
    fun `windows uses local app data for writable application files`() {
        val layout = resolveDesktopAppPathLayout(
            environment(
                osName = "Windows 11",
                userHome = "C:\\Users\\Tester",
                localAppData = "C:\\Users\\Tester\\AppData\\Local",
            )
        )

        val root = Path.of("C:\\Users\\Tester\\AppData\\Local").resolve("Seal")
        assertEquals(root, layout.stateDirectory)
        assertEquals(root.resolve("cache"), layout.cacheDirectory)
        assertEquals(root.resolve("bin"), layout.auxiliaryBinariesDirectory)
    }

    @Test
    fun `macOS uses application support and library caches`() {
        val layout = resolveDesktopAppPathLayout(
            environment(osName = "Mac OS X", userHome = "/Users/tester")
        )

        assertEquals(
            Path.of("/Users/tester/Library/Application Support/Seal"),
            layout.stateDirectory,
        )
        assertEquals(Path.of("/Users/tester/Library/Caches/Seal"), layout.cacheDirectory)
        assertEquals(
            Path.of("/Users/tester/Library/Application Support/Seal/bin"),
            layout.auxiliaryBinariesDirectory,
        )
    }

    @Test
    fun `legacy state is copied to native path while source is retained`() {
        val home = createTempDirectory("seal-path-migration-home")
        val localAppData = home.resolve("native")
        val legacy = home.resolve(".local/state/seal")
        legacy.createDirectories()
        legacy.resolve("settings.json").writeText("legacy-settings")
        legacy.resolve("yt-dlp").createDirectories()
        val legacyCookies = legacy.resolve("yt-dlp/cookies.txt")
        legacyCookies.writeText("legacy-cookies")
        val posixSupported =
            Files.getFileAttributeView(legacyCookies, PosixFileAttributeView::class.java) != null
        if (posixSupported) {
            Files.setPosixFilePermissions(legacyCookies, PosixFilePermissions.fromString("rw-------"))
        }
        val layout =
            resolveDesktopAppPathLayout(
                environment(
                    osName = "Windows 11",
                    userHome = home.toString(),
                    localAppData = localAppData.toString(),
                )
            )

        val active = resolveActiveStateDirectory(layout)

        assertEquals(localAppData.resolve("Seal"), active)
        assertEquals("legacy-settings", active.resolve("settings.json").readText())
        assertEquals("legacy-cookies", active.resolve("yt-dlp/cookies.txt").readText())
        assertTrue(legacy.resolve("settings.json").exists())
        assertTrue(active.resolve(".legacy-state-migrated").exists())
        if (posixSupported) {
            assertEquals(
                PosixFilePermissions.fromString("rw-------"),
                Files.getPosixFilePermissions(active.resolve("yt-dlp/cookies.txt")),
            )
        }
    }

    @Test
    fun `native state wins without touching stale legacy data`() {
        val home = createTempDirectory("seal-path-native-home")
        val localAppData = home.resolve("native")
        val native = localAppData.resolve("Seal")
        val legacy = home.resolve(".local/state/seal")
        native.createDirectories()
        legacy.createDirectories()
        native.resolve("settings.json").writeText("native")
        legacy.resolve("settings.json").writeText("legacy")
        var migrationCalled = false
        val layout =
            resolveDesktopAppPathLayout(
                environment(
                    osName = "Windows 11",
                    userHome = home.toString(),
                    localAppData = localAppData.toString(),
                )
            )

        val active = resolveActiveStateDirectory(layout) { _, _ ->
            migrationCalled = true
            true
        }

        assertEquals(native, active)
        assertFalse(migrationCalled)
        assertEquals("native", native.resolve("settings.json").readText())
    }

    @Test
    fun `failed migration keeps legacy directory active`() {
        val home = createTempDirectory("seal-path-fallback-home")
        val legacy = home.resolve(".local/state/seal")
        legacy.createDirectories()
        legacy.resolve("settings.json").writeText("legacy")
        val layout =
            resolveDesktopAppPathLayout(
                environment(
                    osName = "Mac OS X",
                    userHome = home.toString(),
                )
            )

        val active = resolveActiveStateDirectory(layout) { _, _ -> false }

        assertEquals(legacy, active)
    }

    private fun environment(
        osName: String = "Linux",
        userHome: String = "/home/test",
        xdgStateHome: String? = null,
        xdgCacheHome: String? = null,
        xdgDataHome: String? = null,
        localAppData: String? = null,
        stateDirectoryProperty: String? = null,
        stateDirectoryEnvironment: String? = null,
        auxiliaryDirectoryProperty: String? = null,
        auxiliaryDirectoryEnvironment: String? = null,
    ): DesktopPathEnvironment =
        DesktopPathEnvironment(
            osName = osName,
            userHome = userHome,
            xdgStateHome = xdgStateHome,
            xdgCacheHome = xdgCacheHome,
            xdgDataHome = xdgDataHome,
            localAppData = localAppData,
            stateDirectoryProperty = stateDirectoryProperty,
            stateDirectoryEnvironment = stateDirectoryEnvironment,
            auxiliaryDirectoryProperty = auxiliaryDirectoryProperty,
            auxiliaryDirectoryEnvironment = auxiliaryDirectoryEnvironment,
            temporaryDirectory = "/tmp",
        )
}
