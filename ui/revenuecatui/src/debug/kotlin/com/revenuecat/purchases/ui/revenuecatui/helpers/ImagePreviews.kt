package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.withSave
import coil.ImageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt

// The release source set contains no-op implementations of this code as a precaution. We should not be calling any of
// this code in release builds, but if we do, nothing (bad) will happen.

@JvmSynthetic
internal val LocalPreviewImageLoader: ProvidableCompositionLocal<ImageLoader?> = staticCompositionLocalOf { null }

@JvmSynthetic
@Composable
internal fun ProvidePreviewImageLoader(imageLoader: ImageLoader, content: @Composable () -> Unit): Unit =
    CompositionLocalProvider(
        LocalPreviewImageLoader provides imageLoader,
        content,
    )

/**
 * This is nullable, so the implementation in the release source set can return null.
 */
@JvmSynthetic
@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
internal fun ImageLoader.getPreviewPlaceholderBlocking(imageRequest: ImageRequest): Painter? =
    when (val result = runBlocking { execute(imageRequest) }) {
        is SuccessResult -> DrawablePainter(result.drawable)
        is ErrorResult -> throw result.throwable
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
