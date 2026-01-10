package com.junkfood.seal.ui.download.queue

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.material3.MaterialTheme
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image

@Composable
actual fun DownloadThumbnail(
    url: String?,
    modifier: Modifier,
    contentScale: ContentScale,
) {
    val isPreview = LocalInspectionMode.current
    var bitmapPainter by remember { mutableStateOf<BitmapPainter?>(null) }

    LaunchedEffect(url) {
        if (url.isNullOrBlank() || isPreview) {
            bitmapPainter = null
            return@LaunchedEffect
        }
        val painter = withContext(Dispatchers.IO) {
            runCatching {
                val bytes = URL(url).readBytes()
                val image = Image.makeFromEncoded(bytes)
                BitmapPainter(image.asImageBitmap())
            }.getOrNull()
        }
        bitmapPainter = painter
    }

    val painter = bitmapPainter
    if (painter != null) {
        Image(painter = painter, contentDescription = null, modifier = modifier, contentScale = contentScale)
    } else {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest))
    }
}
