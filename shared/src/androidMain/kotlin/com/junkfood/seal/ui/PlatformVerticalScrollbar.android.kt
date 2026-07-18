package com.junkfood.seal.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

actual val PlatformVerticalScrollbarGutter = 0.dp

@Composable
actual fun PlatformVerticalScrollbar(state: ScrollState, modifier: Modifier) = Unit

@Composable
actual fun PlatformVerticalScrollbar(state: LazyListState, modifier: Modifier) = Unit

@Composable
actual fun PlatformVerticalScrollbar(state: LazyGridState, modifier: Modifier) = Unit
