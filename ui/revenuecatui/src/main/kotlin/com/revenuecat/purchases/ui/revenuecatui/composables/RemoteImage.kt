package com.revenuecat.purchases.ui.revenuecatui.composables

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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.transform.Transformation
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ui.revenuecatui.UIConstant
import com.revenuecat.purchases.ui.revenuecatui.extensions.getImageLoaderTyped
import com.revenuecat.purchases.ui.revenuecatui.helpers.LocalPreviewImageLoader
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.getPreviewPlaceholderBlocking
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
) {
    // Previews don't support images
    val previewImageLoader = LocalPreviewImageLoader.current
    val isInPreviewMode = isInPreviewMode()
    if (isInPreviewMode && previewImageLoader == null) {
        return ImageForPreviews(modifier)
    }

    var cachePolicy by remember { mutableStateOf(CachePolicy.ENABLED) }
    val applicationContext = LocalContext.current.applicationContext
    val imageLoader = previewImageLoader.takeIf { isInPreviewMode } ?: remember(applicationContext) {
        Purchases.getImageLoaderTyped(applicationContext)
    }

    val imageRequest = ImageRequest.Builder(LocalContext.current)
        .data(source.data)
        .crossfade(durationMillis = UIConstant.defaultAnimationDurationMillis)
        .transformations(listOfNotNull(transformation))
        .diskCachePolicy(cachePolicy)
        .memoryCachePolicy(cachePolicy)
        .build()

    if (cachePolicy == CachePolicy.ENABLED) {
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
                cachePolicy = CachePolicy.WRITE_ONLY
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
                placeholder = if (isInPreviewMode()) imageLoader.getPreviewPlaceholderBlocking(imageRequest) else null,
                imageLoader = imageLoader,
                contentScale = contentScale,
                onError = { errorState ->
                    Logger.e("Error loading placeholder image", errorState.result.throwable)
                },
            )
        } ?: if (isInPreviewMode()) imageLoader.getPreviewPlaceholderBlocking(imageRequest) else null,
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
