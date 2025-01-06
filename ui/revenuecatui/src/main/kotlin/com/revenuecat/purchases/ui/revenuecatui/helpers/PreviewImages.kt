package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.graphics.drawable.ColorDrawable
import androidx.annotation.DrawableRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * **For Compose Previews only.**
 *
 * Replaces all images in the [content] with the provided [color].
 */
@InternalRevenueCatAPI
@ExperimentalCoilApi
@Composable
internal fun PreviewImagesAsColor(color: Color, content: @Composable () -> Unit) {
    val previewHandler = AsyncImagePreviewHandler { ColorDrawable(color.toArgb()).asImage() }
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewHandler, content)
}

/**
 * **For Compose Previews only.**
 *
 * Replaces all images in the [content] with the current Material 3 theme's primary color.
 */
@InternalRevenueCatAPI
@ExperimentalCoilApi
@Composable
internal fun PreviewImagesAsPrimaryColor(content: @Composable () -> Unit) =
    PreviewImagesAsColor(MaterialTheme.colorScheme.primary, content)

/**
 * **For Compose Previews only.**
 *
 * Replaces all images in the [content] with the provided Drawable [resource].
 */
@InternalRevenueCatAPI
@ExperimentalCoilApi
@Composable
internal fun PreviewImagesAsDrawableResource(@DrawableRes resource: Int, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val previewHandler = AsyncImagePreviewHandler { context.getDrawable(resource)?.asImage() }
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewHandler, content)
}
