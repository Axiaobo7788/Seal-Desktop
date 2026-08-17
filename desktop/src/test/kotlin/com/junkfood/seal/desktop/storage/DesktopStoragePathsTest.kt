package com.junkfood.seal.desktop.storage

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopStoragePathsTest {
    @Test
    fun `system property takes precedence over environment state directory`() {
        val resolved = resolveDesktopStateBaseDir(
            propertyStateDir = " /property/state ",
            environmentStateDir = "/environment/state",
            xdgStateHome = "/xdg/state",
            userHome = "/home/test",
        )

        assertEquals(Path.of("/property/state"), resolved)
    }

    @Test
    fun `environment state directory supports packaged smoke tests`() {
        val resolved = resolveDesktopStateBaseDir(
            propertyStateDir = " ",
            environmentStateDir = " /isolated/smoke-state ",
            xdgStateHome = "/xdg/state",
            userHome = "/home/test",
        )

        assertEquals(Path.of("/isolated/smoke-state"), resolved)
    }

    @Test
    fun `xdg and home remain the normal fallback chain`() {
        assertEquals(
            Path.of("/xdg/state"),
            resolveDesktopStateBaseDir(null, null, " /xdg/state ", "/home/test"),
        )
        assertEquals(
            Path.of("/home/test", ".local", "state"),
            resolveDesktopStateBaseDir(null, null, " ", "/home/test"),
        )
    }
}
