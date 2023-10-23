package com.revenuecat.purchases.ui.revenuecatui.composables

import android.content.Context
import androidx.annotation.DrawableRes
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

@SuppressWarnings("LongParameterList")
@Composable
internal fun LocalImage(
    @DrawableRes resource: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null,
    transformation: Transformation? = null,
    alpha: Float = 1f,
) {
    Image(
        source = ImageSource.Local(resource),
        modifier = modifier,
        contentScale = contentScale,
        contentDescription = contentDescription,
        transformation = transformation,
        alpha = alpha,
    )
}

@SuppressWarnings("LongParameterList")
@Composable
internal fun RemoteImage(
    urlString: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null,
    transformation: Transformation? = null,
    alpha: Float = 1f,
) {
    Image(
        source = ImageSource.Remote(urlString),
        modifier = modifier,
        contentScale = contentScale,
        contentDescription = contentDescription,
        transformation = transformation,
        alpha = alpha,
    )
}

private sealed class ImageSource {
    data class Local(@DrawableRes val resource: Int) : ImageSource() {
        override val data: Any = resource
    }
    data class Remote(val urlString: String) : ImageSource() {
        override val data: Any = urlString
    }

    abstract val data: Any
}

@SuppressWarnings("LongParameterList")
@Composable
private fun Image(
    source: ImageSource,
    modifier: Modifier = Modifier,
    contentScale: ContentScale,
    contentDescription: String?,
    transformation: Transformation?,
    alpha: Float,
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(source.data)
            .crossfade(durationMillis = UIConstant.defaultAnimationDurationMillis)
            .transformations(listOfNotNull(transformation))
            .build(),
        contentDescription = contentDescription,
        imageLoader = LocalContext.current.getRevenueCatUIImageLoader(),
        modifier = modifier,
        contentScale = contentScale,
        alpha = alpha,
        onState = {
            when (it) {
                is AsyncImagePainter.State.Error -> {
                    val error = when (source) {
                        is ImageSource.Local -> "Error loading local image: '${source.resource}'"
                        is ImageSource.Remote -> "Error loading image from '${source.urlString}'"
                    }

                    Logger.e("$error: ${it.result}")
                }
                else -> {}
            }
        },
    )
}

// Note: these values have to match those in CoilImageDownloader
private const val MAX_CACHE_SIZE_BYTES = 25 * 1024 * 1024L // 25 MB
private const val PAYWALL_IMAGE_CACHE_FOLDER = "revenuecatui_cache"

/**
 * This downloads paywall images in a specific cache for RevenueCat.
 * If you update this, make sure the version in the [CoilImageDownloader] class is also updated.
 */
@Composable
@ReadOnlyComposable
private fun Context.getRevenueCatUIImageLoader(): ImageLoader {
    return ImageLoader.Builder(this)
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve(PAYWALL_IMAGE_CACHE_FOLDER))
                .maxSizeBytes(MAX_CACHE_SIZE_BYTES)
                .build()
        }
        .build()
}
