package com.junkfood.seal.ui.download.queue

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter

@Composable
actual fun DownloadThumbnail(
    url: String?,
    modifier: Modifier,
    contentScale: ContentScale,
) {
    val painter = rememberAsyncImagePainter(model = url)
    Image(
        painter = painter,
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale,
    )
}
