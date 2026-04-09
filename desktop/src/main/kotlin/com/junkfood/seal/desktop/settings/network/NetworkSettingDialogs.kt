package com.junkfood.seal.desktop.settings.network

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OfflineBolt
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SignalWifi4Bar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.junkfood.seal.desktop.ui.AnimatedAlertDialog
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.cancel
import com.junkfood.seal.shared.generated.resources.concurrent_download
import com.junkfood.seal.shared.generated.resources.concurrent_download_num
import com.junkfood.seal.shared.generated.resources.confirm
import com.junkfood.seal.shared.generated.resources.invalid_input
import com.junkfood.seal.shared.generated.resources.max_rate
import com.junkfood.seal.shared.generated.resources.proxy
import com.junkfood.seal.shared.generated.resources.proxy_desc
import com.junkfood.seal.shared.generated.resources.rate_limit
import com.junkfood.seal.shared.generated.resources.rate_limit_desc
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
internal fun RateLimitDialog(
    visible: Boolean,
    initialRate: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var isError by remember(visible) { mutableStateOf(false) }
    var maxRate by remember(visible) { mutableStateOf(initialRate) }

    AnimatedAlertDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Rounded.Speed, null) },
        title = { Text(stringResource(Res.string.rate_limit)) },
        text = {
            Column {
                Text(
                    stringResource(Res.string.rate_limit_desc),
                    style = MaterialTheme.typography.bodyLarge,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp),
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text(
                                text = stringResource(Res.string.invalid_input),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    value = maxRate,
                    label = { Text(stringResource(Res.string.max_rate)) },
                    onValueChange = {
                        if (it.all { ch -> ch.isDigit() }) maxRate = it
                        isError = false
                    },
                    trailingIcon = { Text("K") },
                    singleLine = true
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(Res.string.cancel)) }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val rate = maxRate.toIntOrNull()
                    if (rate != null && rate in 1..1000000) {
                        onConfirm(maxRate)
                        onDismissRequest()
                    } else {
                        isError = true
                    }
                }
            ) {
                Text(stringResource(Res.string.confirm))
            }
        },
    )
}

@Composable
internal fun ConcurrentDownloadDialog(
    visible: Boolean,
    initialFragments: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var concurrentFragments by remember(visible) {
        val mapped = if (initialFragments <= 1) 0f else (initialFragments / 8f) / 3f
        mutableFloatStateOf(mapped.coerceIn(0f, 1f))
    }
    val count by remember {
        derivedStateOf {
            if (concurrentFragments <= 0.125f) 1 else ((concurrentFragments * 3f).roundToInt()) * 8
        }
    }
    
    AnimatedAlertDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Outlined.OfflineBolt, null) },
        title = { Text(stringResource(Res.string.concurrent_download)) },
        text = {
            Column {
                Text(text = stringResource(Res.string.concurrent_download_num, count))

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = concurrentFragments,
                    onValueChange = { concurrentFragments = it },
                    steps = 2,
                    valueRange = 0f..1f,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(Res.string.cancel)) }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(count)
                    onDismissRequest()
                }
            ) {
                Text(stringResource(Res.string.confirm))
            }
        },
    )
}

@Composable
internal fun ProxyConfigurationDialog(
    visible: Boolean,
    initialProxy: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var proxyUrl by remember(visible) { mutableStateOf(initialProxy) }
    AnimatedAlertDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Rounded.SignalWifi4Bar, null) },
        title = { Text(stringResource(Res.string.proxy)) },
        text = {
            Column {
                Text(
                    stringResource(Res.string.proxy_desc),
                    style = MaterialTheme.typography.bodyLarge,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp),
                    value = proxyUrl,
                    label = { Text(stringResource(Res.string.proxy)) },
                    placeholder = { Text("http://127.0.0.1:7890") },
                    onValueChange = { proxyUrl = it },
                    singleLine = true,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(Res.string.cancel)) }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(proxyUrl)
                    onDismissRequest()
                }
            ) {
                Text(stringResource(Res.string.confirm))
            }
        },
    )
}