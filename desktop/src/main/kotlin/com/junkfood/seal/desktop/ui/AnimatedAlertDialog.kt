package com.junkfood.seal.desktop.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier.alpha(alphaAnim).scale(scale),
        icon = icon,
        title = title,
        text = text,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        containerColor = containerColor,
    )
}
