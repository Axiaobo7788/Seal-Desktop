@file:Suppress("DEPRECATION")

package com.junkfood.seal.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarAdapter
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

actual val PlatformVerticalScrollbarGutter = 12.dp

private val DesktopScrollbarLaneWidth = 14.dp
private val DesktopScrollbarThickness = 8.dp

@Composable
actual fun PlatformVerticalScrollbar(state: ScrollState, modifier: Modifier) {
    PlatformVerticalScrollbar(
        visible = state.maxValue > 0,
        adapter = rememberScrollbarAdapter(state),
        modifier = modifier,
    )
}

@Composable
actual fun PlatformVerticalScrollbar(state: LazyListState, modifier: Modifier) {
    PlatformVerticalScrollbar(
        visible = state.canScrollBackward || state.canScrollForward,
        adapter = rememberScrollbarAdapter(state),
        modifier = modifier,
    )
}

@Composable
actual fun PlatformVerticalScrollbar(state: LazyGridState, modifier: Modifier) {
    PlatformVerticalScrollbar(
        visible = state.canScrollBackward || state.canScrollForward,
        adapter = rememberScrollbarAdapter(state),
        modifier = modifier,
    )
}

@Composable
private fun PlatformVerticalScrollbar(
    visible: Boolean,
    adapter: ScrollbarAdapter,
    modifier: Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(durationMillis = 120)),
        exit = fadeOut(tween(durationMillis = 120)),
    ) {
        DesktopScrollbarLane {
            VerticalScrollbar(
                adapter = adapter,
                modifier = Modifier.fillMaxHeight().width(DesktopScrollbarThickness),
                style = desktopScrollbarStyle(),
            )
        }
    }
}

@Composable
private fun PlatformVerticalScrollbar(
    visible: Boolean,
    adapter: androidx.compose.foundation.v2.ScrollbarAdapter,
    modifier: Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(durationMillis = 120)),
        exit = fadeOut(tween(durationMillis = 120)),
    ) {
        DesktopScrollbarLane {
            VerticalScrollbar(
                adapter = adapter,
                modifier = Modifier.fillMaxHeight().width(DesktopScrollbarThickness),
                style = desktopScrollbarStyle(),
            )
        }
    }
}

@Composable
private fun DesktopScrollbarLane(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxHeight().width(DesktopScrollbarLaneWidth),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun desktopScrollbarStyle() =
    ScrollbarStyle(
        minimalHeight = 30.dp,
        thickness = DesktopScrollbarThickness,
        shape = CircleShape,
        hoverDurationMillis = 180,
        unhoverColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.20f),
        hoverColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
    )
