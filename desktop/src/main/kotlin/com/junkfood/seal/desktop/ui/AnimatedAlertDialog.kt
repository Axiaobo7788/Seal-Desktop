package com.junkfood.seal.desktop.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay

@Composable
fun AnimatedAlertDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    containerColor: Color = AlertDialogDefaults.containerColor,
) {
    var shouldRender by remember { mutableStateOf(false) }
    var animationVisible by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            shouldRender = true
            animationVisible = false
            withFrameNanos { }
            animationVisible = true
        } else {
            animationVisible = false
            delay(150)
            shouldRender = false
        }
    }

    if (!shouldRender) return

    val alphaAnim by animateFloatAsState(
        targetValue = if (animationVisible) 1f else 0f,
        animationSpec = tween(if (animationVisible) 230 else 130),
        label = "dialogAlpha",
    )
    val scale by animateFloatAsState(
        targetValue = if (animationVisible) 1f else 0.9f,
        animationSpec = tween(if (animationVisible) 230 else 130),
        label = "dialogScale",
    )

    Popup(
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f * alphaAnim))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = modifier
                    .scale(scale)
                    .alpha(alphaAnim)
                    .widthIn(min = 280.dp, max = 560.dp)
                    .padding(32.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} 
                    ),
                shape = AlertDialogDefaults.shape,
                color = containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    if (icon != null) {
                        Box(
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
                        ) {
                            CompositionLocalProvider(LocalContentColor provides AlertDialogDefaults.iconContentColor) {
                                icon()
                            }
                        }
                    }
                    if (title != null) {
                        CompositionLocalProvider(LocalContentColor provides AlertDialogDefaults.titleContentColor) {
                            ProvideTextStyle(MaterialTheme.typography.headlineSmall) {
                                Box(modifier = Modifier.padding(bottom = 16.dp)) {
                                    title()
                                }
                            }
                        }
                    }
                    if (text != null) {
                        CompositionLocalProvider(LocalContentColor provides AlertDialogDefaults.textContentColor) {
                            ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                                Box(modifier = Modifier.padding(bottom = 24.dp)) {
                                    text()
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (dismissButton != null) {
                            dismissButton()
                        }
                        confirmButton()
                    }
                }
            }
        }
    }
}
