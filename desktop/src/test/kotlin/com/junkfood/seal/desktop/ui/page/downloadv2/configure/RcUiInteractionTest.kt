@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.junkfood.seal.desktop.ui.page.downloadv2.configure

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import com.junkfood.seal.desktop.download.DesktopDownloadType
import com.junkfood.seal.desktop.download.clip.*
import com.junkfood.seal.desktop.download.configure.DownloadOptionsSheet
import com.junkfood.seal.desktop.i18n.DesktopResourceEnvironmentController
import com.junkfood.seal.desktop.i18n.ReflectionResourceEnvironmentBridge
import com.junkfood.seal.util.*
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.*

class RcUiInteractionTest {
    private val resources = DesktopResourceEnvironmentController(ReflectionResourceEnvironmentBridge).apply { setLanguageOverride("en") }
    private val formats = (0..20).map { Format(formatId = "audio-$it", format = "audio-$it", vcodec = "none", acodec = "opus", ext = "webm") }
    private val info = VideoInfo(id = "fixture", title = "Offline fixture", duration = 600.0, formats = formats)

    @Composable private fun Content(content: @Composable () -> Unit) {
        resources.Provide { MaterialTheme { Surface(Modifier.fillMaxSize()) { content() } } }
    }

    @Test fun `clip field edits slider semantics invalid drafts focus validation and reset`() = runDesktopComposeUiTest {
        var state by mutableStateOf(ClipEditorState.create(600))
        setContent { Content { Column(Modifier.width(600.dp)) { ClipEditor(state) { state = it } } } }
        val start = onNode(hasSetTextAction() and hasText("Start"))
        val end = onNode(hasSetTextAction() and hasText("End"))
        start.performTextReplacement("0:42")
        end.performTextReplacement("8:17")
        runOnIdle { assertEquals(VideoClip(42, 497), state.validDraft) }
        start.performTextReplacement("1:")
        end.performClick()
        onNodeWithText("Enter valid times", substring = true).assertExists()
        runOnIdle { assertEquals(VideoClip(42, 497), state.range); assertNull(state.validDraft) }
        start.performTextReplacement("1:23")
        onNodeWithContentDescription("Start").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(90f) }
        start.assertTextContains("1:30")
        runOnIdle { assertEquals(VideoClip(90, 497), state.validDraft) }
        onNodeWithContentDescription("Start").performMouseInput {
            moveTo(center)
            press()
            moveBy(Offset(4f, 0f), delayMillis = 16)
            moveBy(Offset(40f, 0f), delayMillis = 32)
            release()
        }
        runOnIdle { assertTrue(state.range.start > 90); assertEquals(state.range, state.validDraft) }
        val beforeKeyboard = runOnIdle { state.range.start }
        onNodeWithContentDescription("Start").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus) { it() }
        onNodeWithContentDescription("Start").performKeyInput { pressKey(Key.DirectionRight) }
        runOnIdle { assertEquals(beforeKeyboard + 1, state.range.start); assertEquals(state.range, state.validDraft) }
        onNodeWithContentDescription("End").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus) { it() }
        onNodeWithContentDescription("End").performKeyInput { pressKey(Key.DirectionLeft) }
        runOnIdle { assertEquals(496, state.range.end); assertEquals(state.range, state.validDraft) }
        onNodeWithContentDescription("End").performKeyInput { pressKey(Key.MoveHome) }
        runOnIdle { assertEquals(state.range.start + 1, state.range.end) }
        onNodeWithContentDescription("End").performKeyInput { pressKey(Key.MoveEnd) }
        runOnIdle { assertEquals(600, state.range.end) }
        onNodeWithText("Reset").performClick()
        start.assertTextContains("0:00")
        end.assertTextContains("10:00")
    }

    @Test fun `filtered format selects original index and selection survives filtering and clear`() = runDesktopComposeUiTest {
        var selected: FormatConfig? = null
        setContent { Content {
            FormatPageImpl(info, DownloadPreferences.EMPTY, true, false, {}, { selected = it })
        } }
        val search = onNode(hasSetTextAction() and hasText("Search formats"))
        val card = hasText("audio-7") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)
        search.performTextReplacement("audio-7")
        onNode(card).performClick().assertIsSelected()
        search.performTextReplacement("not-a-codec")
        onNodeWithText("No matching formats").assertExists()
        onNodeWithContentDescription("Download").performClick()
        runOnIdle { assertEquals(listOf(formats[7]), selected?.formatList) }
        onNodeWithContentDescription("Clear").performClick()
        search.assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        search.performTextReplacement("audio-7")
        onNode(card).assertIsSelected()
        onNode(card).performClick().performKeyInput { pressKey(Key.Spacebar) }
        onNode(card).assertIsSelected()
        onNode(card).performKeyInput { pressKey(Key.Enter) }
        onNode(card).assertIsNotSelected()
    }

    @Test fun `clip keyboard respects RTL direction and never crosses short range boundaries`() = runDesktopComposeUiTest {
        var state by mutableStateOf(ClipEditorState.create(600, VideoClip(100, 200)))
        setContent { Content {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { ClipEditor(state) { state = it } }
        } }
        val start = onNodeWithContentDescription("Start")
        val end = onNodeWithContentDescription("End")
        start.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus) { it() }
        start.performKeyInput { pressKey(Key.DirectionRight) }
        runOnIdle { assertEquals(VideoClip(99, 200), state.range) }
        start.performKeyInput { pressKey(Key.DirectionLeft) }
        runOnIdle { assertEquals(VideoClip(100, 200), state.range) }
        start.performKeyInput { pressKey(Key.MoveEnd); pressKey(Key.DirectionUp) }
        runOnIdle { assertEquals(VideoClip(199, 200), state.range) }
        end.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus) { it() }
        end.performKeyInput { pressKey(Key.MoveHome); pressKey(Key.DirectionDown) }
        runOnIdle { assertEquals(VideoClip(199, 200), state.range); state = ClipEditorState.create(1) }
        end.performKeyInput { pressKey(Key.DirectionDown); pressKey(Key.MoveHome) }
        runOnIdle { assertEquals(VideoClip(0, 1), state.validDraft) }
    }

    @Test fun `error without diagnostic retry empty retry and loading preserve request context`() = runDesktopComposeUiTest {
        val preferences = DownloadPreferences.EMPTY.copy(newTitle = "Frozen title", cookiesBrowser = "firefox")
        val pending = CompletableDeferred<Result<VideoInfo>>()
        val requests = mutableListOf<Pair<String, DownloadPreferences>>()
        var backCount = 0
        setContent { Content {
            CustomFormatSelectionContent("https://example.com/video", DesktopDownloadType.Audio, preferences, false,
                fetchVideoInfo = { url, prefs ->
                    requests += url to prefs
                    when (requests.size) {
                        1 -> Result.failure(Exception())
                        2 -> Result.success(info.copy(formats = emptyList()))
                        else -> pending.await()
                    }
                }, onBack = { backCount++ }, onDownload = { _, _ -> })
        } }
        onNodeWithText("Retry").assertExists().performClick()
        onNodeWithText("No available formats").assertExists()
        onNodeWithText("Retry").performClick()
        onNodeWithText("Fetching video info…").assertExists()
        runOnIdle { pending.complete(Result.success(info)) }
        onNodeWithText("Search formats").assertExists()
        onNodeWithContentDescription("Back").performClick()
        runOnIdle {
            assertEquals(1, backCount)
            assertEquals(List(3) { "https://example.com/video" to preferences }, requests)
        }
    }

    @Test fun `changing URL cancels previous metadata request and rejects stale completion`() = runDesktopComposeUiTest {
        val pending = CompletableDeferred<Result<VideoInfo>>()
        var url by mutableStateOf("https://example.com/first")
        var canceled = false
        setContent { Content {
            CustomFormatSelectionContent(url, DesktopDownloadType.Video, DownloadPreferences.EMPTY, false,
                fetchVideoInfo = { input, _ ->
                    if (input.endsWith("first")) try { pending.await() } finally { canceled = true }
                    else Result.success(info.copy(title = "Second request"))
                }, onBack = {}, onDownload = { _, _ -> })
        } }
        onNodeWithText("Fetching video info…").assertExists()
        runOnIdle { url = "https://example.com/second" }
        onNodeWithText("Second request").assertExists()
        runOnIdle { assertTrue(canceled); pending.complete(Result.success(info.copy(title = "Stale first request"))) }
        onNodeWithText("Stale first request").assertDoesNotExist()
    }

    @Test fun `collapsed additional settings summary follows updated preferences`() = runDesktopComposeUiTest(width = 640, height = 800) {
        var preferences by mutableStateOf(DownloadPreferences.EMPTY.copy(cookies = true, downloadSubtitle = true, embedThumbnail = false, embedMetadata = true, sponsorBlock = false))
        setContent { Content {
            DownloadOptionsSheet("https://example.com", DesktopDownloadType.Video, {}, preferences, { preferences = it }, false, null, {}, {}, {})
        } }
        onNodeWithText("Cookies · Download subtitles · Embed metadata").assertExists()
        onNode(hasClickAction() and hasText("Additional settings", substring = true)).performClick()
        onNodeWithText("Cookies · Download subtitles · Embed metadata").assertDoesNotExist()
        onNode(hasClickAction() and hasText("SponsorBlock")).performClick()
        onNode(hasClickAction() and hasText("Additional settings", substring = true)).performClick()
        onNodeWithText("Cookies · Download subtitles · Embed metadata · SponsorBlock").assertExists()
    }

    @Test fun `format selection preference controls the executable bottom action`() = runDesktopComposeUiTest(width = 640, height = 800) {
        var presetDownloads = 0
        var customSelections = 0
        setContent { Content {
            DownloadOptionsSheet(
                urlPreview = "https://example.com",
                downloadType = DesktopDownloadType.Video,
                onTypeChange = {},
                preferences = DownloadPreferences.EMPTY,
                onPreferencesChange = {},
                customCommandEnabled = false,
                customCommandTemplate = null,
                onDismissRequest = {},
                onDownload = { presetDownloads++ },
                onCustomFormatClick = { customSelections++ },
                preferCustomFormat = true,
            )
        } }

        onNodeWithText("Continue").performClick()
        runOnIdle { assertEquals(1, customSelections); assertEquals(0, presetDownloads) }

        onNodeWithText("Preset").performClick()
        onNode(hasClickAction() and hasText("Download")).performClick()
        runOnIdle { assertEquals(1, customSelections); assertEquals(1, presetDownloads) }
    }

    @Test fun `clip and split menu actions produce exclusive download intents and invalid drafts block submission`() = runDesktopComposeUiTest(height = 1000) {
        var selected: FormatConfig? = null
        setContent { Content {
            FormatPageImpl(
                info.copy(requestedFormats = listOf(formats[0]), chapters = listOf(Chapter("Part", 0.0, 600.0))),
                DownloadPreferences.EMPTY.copy(videoClips = listOf(VideoClip(42, 497)), splitByChapter = true),
                false, false, {}, { selected = it }, isVideoClipEnabled = true,
            )
        } }
        val start = onNode(hasSetTextAction() and hasText("Start"))
        start.assertTextContains("0:42")
        start.performTextReplacement("1:")
        onNodeWithContentDescription("Download").assertDoesNotExist()
        start.performTextReplacement("0:42")
        onNodeWithContentDescription("Download").performClick()
        runOnIdle { assertEquals(listOf(VideoClip(42, 497)), selected?.videoClips); assertFalse(selected!!.splitByChapter) }
        onNodeWithContentDescription("Show more actions").performClick()
        onNodeWithText("Split video").performClick()
        onNodeWithText("Video will be split", substring = true).assertExists()
        onNodeWithContentDescription("Download").performClick()
        runOnIdle { assertTrue(selected!!.videoClips.isEmpty()); assertTrue(selected!!.splitByChapter) }
        onNodeWithContentDescription("Show more actions").performClick()
        onNodeWithText("Clip video").performClick()
        start.assertTextContains("0:42")
        onNodeWithContentDescription("Download").performClick()
        runOnIdle { assertEquals(listOf(VideoClip(42, 497)), selected?.videoClips); assertFalse(selected!!.splitByChapter) }
    }

    @Test fun `unknown live and sub-second duration hide clip action and never create zero slider`() = runDesktopComposeUiTest {
        var duration by mutableStateOf<Double?>(null)
        var live by mutableStateOf<String?>(null)
        setContent { Content {
            key(duration, live) { FormatPageImpl(info.copy(duration = duration, liveStatus = live), DownloadPreferences.EMPTY, false, false, {}, {}, true) }
        } }
        listOf(null to null, 0.5 to null, 600.0 to "is_live").forEach { (time, status) ->
            runOnIdle { duration = time; live = status }
            onNodeWithContentDescription("Show more actions").performClick()
            onNodeWithText("Clip video").assertDoesNotExist()
            onNodeWithText("Rename").performKeyInput { pressKey(Key.Escape) }
        }
    }

    @Test fun `thrown metadata error keeps retry and back visible with long diagnostics`() = runDesktopComposeUiTest(width = 640, height = 640) {
        var backCount = 0
        setContent { Content {
            CustomFormatSelectionContent("https://example.com/video", DesktopDownloadType.Video, DownloadPreferences.EMPTY, false,
                fetchVideoInfo = { _, _ -> throw IllegalStateException("Diagnostic line\n".repeat(1000)) },
                onBack = { backCount++ }, onDownload = { _, _ -> })
        } }
        onNodeWithText("Retry").assertIsDisplayed().performClick().assertIsDisplayed()
        onNodeWithText("Back").assertIsDisplayed().performClick()
        runOnIdle { assertEquals(1, backCount) }
    }

    @Test fun `blank URL is explicit and never starts metadata`() = runDesktopComposeUiTest {
        var calls = 0
        setContent { Content {
            CustomFormatSelectionContent(" ", DesktopDownloadType.Video, DownloadPreferences.EMPTY, false,
                fetchVideoInfo = { _, _ -> calls++; Result.success(info) }, onBack = {}, onDownload = { _, _ -> })
        } }
        onNodeWithText("Back").assertIsDisplayed()
        onNodeWithText("Retry").assertDoesNotExist()
        runOnIdle { assertEquals(0, calls) }
    }

    @Test fun `fold animation keeps header height and collapse moves actions monotonically`() = runDesktopComposeUiTest(width = 640, height = 900) {
        val prefs = DownloadPreferences.EMPTY.copy(cookies = true, downloadSubtitle = true, embedThumbnail = true, embedMetadata = true, sponsorBlock = true)
        setContent { Content { DownloadOptionsSheet("https://example.com", DesktopDownloadType.Video, {}, prefs, {}, false, null, {}, {}, {}) } }
        val header = onNode(hasClickAction() and hasText("Additional settings", substring = true))
        val action = onNode(hasClickAction() and hasText("Download"))
        val originalHeight = header.fetchSemanticsNode().boundsInRoot.height
        header.performClick()
        assertEquals(originalHeight, header.fetchSemanticsNode().boundsInRoot.height)
        mainClock.autoAdvance = false
        header.performClick()
        var previousTop = action.fetchSemanticsNode().boundsInRoot.top
        repeat(7) {
            mainClock.advanceTimeBy(32)
            waitForIdle()
            val nextTop = action.fetchSemanticsNode().boundsInRoot.top
            assertTrue(nextTop <= previousTop + 1f, "collapse moved actions downward: $previousTop -> $nextTop")
            assertEquals(originalHeight, header.fetchSemanticsNode().boundsInRoot.height)
            previousTop = nextTop
        }
        mainClock.autoAdvance = true
    }
}
