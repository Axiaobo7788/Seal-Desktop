package com.junkfood.seal.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

expect val PlatformVerticalScrollbarGutter: Dp

@Composable
expect fun PlatformVerticalScrollbar(
    state: ScrollState,
    modifier: Modifier = Modifier,
)

@Composable
expect fun PlatformVerticalScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
)

@Composable
expect fun PlatformVerticalScrollbar(
    state: LazyGridState,
    modifier: Modifier = Modifier,
)
