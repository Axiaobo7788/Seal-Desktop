package com.junkfood.seal.ui.download.queue

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun Modifier.desktopHorizontalScrollGesture(state: LazyListState): Modifier {
    val coroutineScope = rememberCoroutineScope()
    return this.onPointerEvent(PointerEventType.Scroll) { event ->
        val change = event.changes.firstOrNull()
        val scrollDelta = change?.scrollDelta
        val horizontalDelta = scrollDelta?.x ?: 0f
        val fallbackDelta = if (event.keyboardModifiers.isShiftPressed) scrollDelta?.y ?: 0f else 0f
        val effectiveDelta = if (horizontalDelta != 0f) horizontalDelta else fallbackDelta

        if (effectiveDelta != 0f) {
            coroutineScope.launch {
                state.scrollBy(-effectiveDelta)
            }
            event.changes.forEach { it.consume() }
        }
    }
}
