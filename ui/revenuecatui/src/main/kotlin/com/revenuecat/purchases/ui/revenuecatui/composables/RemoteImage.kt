package com.revenuecat.purchases.ui.revenuecatui.composables

import android.content.Context
import android.graphics.drawable.Drawable
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.withSave
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.transform.Transformation
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.UIConstant
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.isInPreviewMode
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt

@SuppressWarnings("LongParameterList")
@Composable
internal fun LocalImage(
    @DrawableRes resource: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null,
    transformation: Transformation? = null,
    alpha: Float = 1f,
    colorFilter: ColorFilter? = null,
) {
    Image(
        source = ImageSource.Local(resource),
        placeholderSource = null,
        modifier = modifier,
        contentScale = contentScale,
        contentDescription = contentDescription,
        transformation = transformation,
        alpha = alpha,
        colorFilter = colorFilter,
        previewImageLoader = null,
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
    colorFilter: ColorFilter? = null,
    previewImageLoader: ImageLoader? = null,
) {
    Image(
        source = ImageSource.Remote(urlString),
        placeholderSource = placeholderUrlString?.let { ImageSource.Remote(it) },
        modifier = modifier,
        contentScale = contentScale,
        contentDescription = contentDescription,
        transformation = transformation,
        alpha = alpha,
        colorFilter = colorFilter,
        previewImageLoader = previewImageLoader,
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
    colorFilter: ColorFilter?,
    previewImageLoader: ImageLoader?,
) {
    // Previews don't support images
    val isInPreviewMode = isInPreviewMode()
    if (isInPreviewMode && previewImageLoader == null) {
        return ImageForPreviews(modifier)
    }

    var useCache by remember { mutableStateOf(true) }
    val applicationContext = LocalContext.current.applicationContext
    val imageLoader = previewImageLoader.takeIf { isInPreviewMode } ?: remember(useCache) {
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
            colorFilter = colorFilter,
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
            colorFilter = colorFilter,
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
    colorFilter: ColorFilter? = null,
    onError: ((AsyncImagePainter.State.Error) -> Unit)? = null,
) {
    AsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        placeholder = placeholderSource?.let {
            rememberAsyncImagePainter(
                model = it.data,
                placeholder = if (isInPreviewMode()) imageLoader.getPreviewPlaceholder(imageRequest) else null,
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
        colorFilter = colorFilter,
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

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
private fun ImageLoader.getPreviewPlaceholder(imageRequest: ImageRequest): Painter =
    when (val result = runBlocking { execute(imageRequest) }) {
        is SuccessResult -> DrawablePainter(result.drawable)
        is ErrorResult -> throw result.throwable
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
            MemoryCache.Builder(this)
                .build(),
        )
        .diskCachePolicy(cachePolicy)
        .memoryCachePolicy(cachePolicy)
        .build()
}

/**
 * This is loosely based on [Accompanist's Drawable Painter](https://google.github.io/accompanist/drawablepainter/).
 * This is not production-quality code and should only be used for Previews. If we ever have a need for this, it's
 * better to use the Accompanist Drawable Painter library directly.
 *
 * It's annotated with [ExperimentalPreviewRevenueCatUIPurchasesAPI] to discourage usage in production and as a nudge
 * to read this documentation.
 */
@ExperimentalPreviewRevenueCatUIPurchasesAPI
private class DrawablePainter(
    private val drawable: Drawable,
) : Painter() {

    override fun DrawScope.onDraw() {
        drawIntoCanvas { canvas ->
            // Update the Drawable's bounds
            drawable.setBounds(0, 0, size.width.roundToInt(), size.height.roundToInt())

            canvas.withSave {
                drawable.draw(canvas.nativeCanvas)
            }
        }
    }

    override val intrinsicSize: Size = drawable.intrinsicSize

    private val Drawable.intrinsicSize: Size
        get() = when {
            // Only return a finite size if the drawable has an intrinsic size
            intrinsicWidth >= 0 && intrinsicHeight >= 0 -> {
                Size(width = intrinsicWidth.toFloat(), height = intrinsicHeight.toFloat())
            }
            else -> Size.Unspecified
        }
}
