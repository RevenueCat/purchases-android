package com.revenuecat.purchases.ui.revenuecatui.composables

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.transformations
import coil3.transform.Transformation
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.UIConstant
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.isInPreviewMode

@SuppressWarnings("LongParameterList")
@Composable
internal fun LocalImage(
    @DrawableRes resource: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null,
    transformation: Transformation? = null,
    alpha: Float = 1f,
    @DrawableRes imagePreview: Int? = null,
) {
    Image(
        source = ImageSource.Local(resource),
        placeholderSource = null,
        modifier = modifier,
        contentScale = contentScale,
        contentDescription = contentDescription,
        transformation = transformation,
        alpha = alpha,
        imagePreview = imagePreview,
    )
}

/**
 * @param supportImagePreview: set to false to not show any image in previews and just a colored box. This is to avoid
 * modifying Paywalls V1 previews.
 */
@SuppressWarnings("LongParameterList")
@Composable
internal fun RemoteImage(
    urlString: String,
    modifier: Modifier = Modifier,
    placeholderUrlString: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null,
    transformation: Transformation? = null,
    alpha: Float = 1f,
    @DrawableRes imagePreview: Int? = null,
) {
    Image(
        source = ImageSource.Remote(urlString),
        placeholderSource = placeholderUrlString?.let { ImageSource.Remote(it) },
        modifier = modifier,
        contentScale = contentScale,
        contentDescription = contentDescription,
        transformation = transformation,
        alpha = alpha,
        imagePreview = imagePreview,
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
    placeholderSource: ImageSource?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale,
    contentDescription: String?,
    transformation: Transformation?,
    alpha: Float,
    @DrawableRes imagePreview: Int?,
) {
    // Previews don't support images
    if (isInPreviewMode() && imagePreview == null) {
        return ImageForPreviews(modifier)
    }

    var useCache by remember { mutableStateOf(true) }
    val applicationContext = LocalContext.current.applicationContext
    val imageLoader = remember(useCache) {
        applicationContext.getRevenueCatUIImageLoader(readCache = useCache)
    }

    val imageRequest = ImageRequest.Builder(LocalContext.current)
        .data(source.data)
        .crossfade(durationMillis = UIConstant.defaultAnimationDurationMillis)
        .transformations(listOfNotNull(transformation))
        .build()

    if (useCache) {
        AsyncImage(
            source = source,
            placeholderSource = placeholderSource,
            imageRequest = imageRequest,
            contentDescription = contentDescription,
            imageLoader = imageLoader,
            modifier = modifier,
            contentScale = contentScale,
            alpha = alpha,
            imagePreview = imagePreview,
            onError = {
                Logger.w("Image failed to load. Will try again disabling cache")
                useCache = false
            },
        )
    } else {
        AsyncImage(
            source = source,
            placeholderSource = placeholderSource,
            imageRequest = imageRequest,
            contentDescription = contentDescription,
            imageLoader = imageLoader,
            modifier = modifier,
            contentScale = contentScale,
            alpha = alpha,
            imagePreview = imagePreview,
        )
    }
}

@SuppressWarnings("LongParameterList")
@Composable
private fun AsyncImage(
    source: ImageSource,
    placeholderSource: ImageSource?,
    imageRequest: ImageRequest,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    contentScale: ContentScale,
    contentDescription: String?,
    alpha: Float,
    @DrawableRes imagePreview: Int? = null,
    onError: ((AsyncImagePainter.State.Error) -> Unit)? = null,
) {
    AsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        placeholder = placeholderSource?.let {
            rememberAsyncImagePainter(
                model = it.data,
                placeholder = if (isInPreviewMode() && imagePreview != null) painterResource(imagePreview) else null,
                imageLoader = imageLoader,
                contentScale = contentScale,
                onError = { errorState ->
                    Logger.e("Error loading placeholder image", errorState.result.throwable)
                },
            )
        } ?: if (isInPreviewMode()) painterResource(R.drawable.android) else null,
        imageLoader = imageLoader,
        modifier = modifier,
        contentScale = contentScale,
        alpha = alpha,
        onError = {
            val error = when (source) {
                is ImageSource.Local -> "Error loading local image: '${source.resource}'"
                is ImageSource.Remote -> "Error loading image from '${source.urlString}'"
            }

            Logger.e(error, it.result.throwable)
            onError?.invoke(it)
        },
    )
}

@Composable
private fun ImageForPreviews(modifier: Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.primary),
    )
}

// Note: these values have to match those in CoilImageDownloader
private const val MAX_CACHE_SIZE_BYTES = 25 * 1024 * 1024L // 25 MB
private const val PAYWALL_IMAGE_CACHE_FOLDER = "revenuecatui_cache"

/**
 * This downloads paywall images in a specific cache for RevenueCat.
 * If you update this, make sure the version in the [CoilImageDownloader] class is also updated.
 *
 * @param readCache: set to false to ignore cache for reading, but allow overwriting with updated image.
 */
private fun Context.getRevenueCatUIImageLoader(readCache: Boolean): ImageLoader {
    val cachePolicy = if (readCache) CachePolicy.ENABLED else CachePolicy.WRITE_ONLY

    return ImageLoader.Builder(this)
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve(PAYWALL_IMAGE_CACHE_FOLDER))
                .maxSizeBytes(MAX_CACHE_SIZE_BYTES)
                .build()
        }
        .memoryCache(
            MemoryCache.Builder()
                .maxSizePercent(this)
                .build(),
        )
        .diskCachePolicy(cachePolicy)
        .memoryCachePolicy(cachePolicy)
        .build()
}
