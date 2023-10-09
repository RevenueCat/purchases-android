package com.revenuecat.purchases.ui.revenuecatui.composables

import BlurTransformation
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.disk.DiskCache
import coil.request.ImageRequest
import coil.transform.Transformation
import com.revenuecat.purchases.ui.revenuecatui.UIConstant
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger

@Composable
internal fun RemoteImage(
    urlString: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null,
    transformation: Transformation? = null,
    alpha: Float = 1f
) {

    val requestBuilder = ImageRequest.Builder(LocalContext.current)
        .data(urlString)
        .crossfade(durationMillis = UIConstant.defaultAnimationDurationMillis)

    transformation?.let {
        requestBuilder.transformations(listOf(it))
    }

    return AsyncImage(
        model = requestBuilder.build(),
        contentDescription = contentDescription,
        imageLoader = LocalContext.current.getRevenueCatUIImageLoader(),
        modifier = modifier,
        contentScale = contentScale,
        alpha = alpha,
        onState = {
            when (it) {
                is AsyncImagePainter.State.Error -> {
                    Logger.e("Error loading image from '$urlString': ${it.result}")
                }
                else -> {}
            }
        },
    )
}


private const val MAX_CACHE_SIZE_BYTES = 25 * 1024 * 1024L // 25 MB

@Composable
@ReadOnlyComposable
private fun Context.getRevenueCatUIImageLoader(): ImageLoader {
    return ImageLoader.Builder(this)
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("revenuecatui_cache"))
                .maxSizeBytes(MAX_CACHE_SIZE_BYTES)
                .build()
        }
        .build()
}
