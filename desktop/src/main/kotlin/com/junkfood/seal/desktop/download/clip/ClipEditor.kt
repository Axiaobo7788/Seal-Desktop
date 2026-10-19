@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.junkfood.seal.desktop.download.clip

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import com.junkfood.seal.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
internal fun ClipEditor(state: ClipEditorState, onChange: (ClipEditorState) -> Unit) {
    val focusManager = LocalFocusManager.current
    var startTouched by remember { mutableStateOf(false) }
    var endTouched by remember { mutableStateOf(false) }
    val invalid = state.validDraft == null
    val startLabel = stringResource(Res.string.clip_start)
    val endLabel = stringResource(Res.string.clip_end)
    val clipLabel = stringResource(Res.string.clip_video)
    val startInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val endInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val startFocused by startInteraction.collectIsFocusedAsState()
    val endFocused by endInteraction.collectIsFocusedAsState()
    val horizontalDirection = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1

    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(clipLabel, style = MaterialTheme.typography.titleSmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatClipTimestamp(state.range.start))
                Text(formatClipTimestamp(state.range.end))
            }
            RangeSlider(
                value = state.range.start.toFloat()..state.range.end.toFloat(),
                onValueChange = { range -> onChange(state.move(range.start.roundToInt(), range.endInclusive.roundToInt())) },
                valueRange = 0f..state.duration.toFloat(),
                startInteractionSource = startInteraction,
                endInteractionSource = endInteraction,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = clipLabel }
                    // This Compose version exposes thumb focus but no keyboard range adjustment.
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown || (!startFocused && !endFocused)) false
                        else {
                            val minimum = if (startFocused) 0 else state.range.start + 1
                            val maximum = if (startFocused) state.range.end - 1 else state.duration
                            val current = if (startFocused) state.range.start else state.range.end
                            val target = when (event.key) {
                                Key.DirectionRight -> current.toLong() + horizontalDirection
                                Key.DirectionLeft -> current.toLong() - horizontalDirection
                                Key.DirectionUp -> current.toLong() + 1
                                Key.DirectionDown -> current.toLong() - 1
                                Key.MoveHome -> minimum.toLong()
                                Key.MoveEnd -> maximum.toLong()
                                else -> null
                            }
                            if (target == null) false
                            else {
                                val seconds = target.coerceIn(minimum.toLong(), maximum.toLong()).toInt()
                                onChange(if (startFocused) state.move(seconds, state.range.end) else state.move(state.range.start, seconds))
                                true
                            }
                        }
                    },
                startThumb = { _ ->
                    SliderDefaults.Thumb(
                        interactionSource = startInteraction,
                        modifier = Modifier.semantics { contentDescription = startLabel },
                    )
                },
                endThumb = { _ ->
                    SliderDefaults.Thumb(
                        interactionSource = endInteraction,
                        modifier = Modifier.semantics { contentDescription = endLabel },
                    )
                },
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatClipTimestamp(0), style = MaterialTheme.typography.labelSmall)
                Text(formatClipTimestamp(state.duration), style = MaterialTheme.typography.labelSmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.startDraft,
                    onValueChange = { onChange(state.edit(start = it)) },
                    label = { Text(startLabel) },
                    singleLine = true,
                    isError = invalid && startTouched,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { startTouched = true; focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) }),
                    modifier = Modifier.weight(1f).onFocusChanged { if (!it.isFocused) startTouched = true },
                )
                OutlinedTextField(
                    value = state.endDraft,
                    onValueChange = { onChange(state.edit(end = it)) },
                    label = { Text(endLabel) },
                    singleLine = true,
                    isError = invalid && endTouched,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { endTouched = true; focusManager.clearFocus() }),
                    modifier = Modifier.weight(1f).onFocusChanged { if (!it.isFocused) endTouched = true },
                )
            }
            if (invalid) {
                Text(stringResource(Res.string.clip_range_invalid), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.clip_duration, formatClipTimestamp(state.range.end - state.range.start)),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = { onChange(state.reset()); startTouched = false; endTouched = false }) {
                    Text(stringResource(Res.string.reset))
                }
            }
        }
    }
}
