@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.junkfood.seal.desktop.ui.page.downloadv2.configure

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Density
import com.junkfood.seal.desktop.i18n.DesktopResourceEnvironmentController
import com.junkfood.seal.desktop.i18n.ReflectionResourceEnvironmentBridge
import com.junkfood.seal.util.*
import java.io.File
import kotlin.test.*

class RcUiLayoutTest {
    @Test fun `format clip layout renders across widths scales themes and locale fallback`() {
        val widths = listOf(640, 720, 800, 1024, 1199, 1200, 1300, 1600)
        val locales = listOf("en", "zh-Hans", "zh-Hant", "de", "zz")
        widths.forEachIndexed { index, width ->
            listOf(1f, 1.25f, 1.5f).forEach { scale ->
                val resources = DesktopResourceEnvironmentController(ReflectionResourceEnvironmentBridge).apply { setLanguageOverride(locales[index % locales.size]) }
                val format = Format(formatId = "137", format = "137 - 1920x1080 (1080p60)", vcodec = "av01.0.08M.08", acodec = "none", ext = "webm", width = 1920.0, height = 1080.0, fps = 60.0)
                val info = VideoInfo(id = "fixture", title = "Offline fixture / A long video title for responsive layouts", duration = 600.0,
                    formats = List(14) { format.copy(formatId = "$it") }, requestedFormats = listOf(format))
                runDesktopComposeUiTest(width = (width * scale).toInt(), height = (900 * scale).toInt()) {
                    setContent {
                        CompositionLocalProvider(LocalDensity provides Density(scale)) {
                            resources.Provide {
                                MaterialTheme(colorScheme = if (index % 2 == 0) lightColorScheme() else darkColorScheme()) {
                                    Surface(Modifier.fillMaxSize()) {
                                        FormatPageImpl(info, DownloadPreferences.EMPTY.copy(videoClips = listOf(VideoClip(42, 497))), false, false, {}, {}, true)
                                    }
                                }
                            }
                        }
                    }
                    val fields = onAllNodes(hasSetTextAction()).fetchSemanticsNodes()
                    assertEquals(3, fields.size, "width=$width scale=$scale")
                    fields.forEach { node ->
                        assertTrue(node.boundsInRoot.width >= 96 * scale)
                        assertTrue(node.boundsInRoot.left >= 0 && node.boundsInRoot.right <= width * scale)
                    }
                    onNode(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true)).assertExists()
                    if ((width == 640 || width == 1300) && scale == 1f) {
                        val output = File("build/reports/rc-ui/format-clip-$width.png")
                        output.parentFile.mkdirs()
                        val bytes = org.jetbrains.skia.Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).encodeToData()!!.bytes
                        output.writeBytes(bytes)
                    }
                }
            }
        }
    }
}
