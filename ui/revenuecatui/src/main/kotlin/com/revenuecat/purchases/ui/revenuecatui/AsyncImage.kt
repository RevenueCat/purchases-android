package com.revenuecat.purchases.ui.revenuecatui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch
import java.net.URL

@Composable
internal fun AsyncImage(
    url: URL,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val coroutineScope = rememberCoroutineScope()
    val bitmapToDisplay = remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(url) {
        coroutineScope.launch {
            val result = url.toImageBitmap()
            result.getOrNull()?.let { bitmap -> bitmapToDisplay.value = bitmap }
        }
    }

    bitmapToDisplay.value?.let {
        Image(
            it.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
        )
    } ?: Box(modifier = modifier.background(Color.Gray.copy(alpha = 0.5f)))
}
