@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.modifier

import androidx.compose.ui.unit.Density
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed

/**
 * Resolves intrinsic media dimensions while preserving the media's aspect ratio when both axes are [Fit].
 */
internal fun Size.adjustForMedia(
    widthPx: UInt,
    heightPx: UInt,
    density: Density,
): Size {
    val fitWidth = width as? Fit
    val fitHeight = height as? Fit
    if (fitWidth != null && fitHeight != null) {
        return adjustFitDimensions(
            width = fitWidth,
            height = fitHeight,
            widthPx = widthPx,
            heightPx = heightPx,
            density = density,
        )
    }

    return Size(
        width = width.adjustDimension(
            other = height,
            thisDimensionPx = widthPx,
            otherDimensionPx = heightPx,
            density = density,
        ),
        height = height.adjustDimension(
            other = width,
            thisDimensionPx = heightPx,
            otherDimensionPx = widthPx,
            density = density,
        ),
    )
}

private fun adjustFitDimensions(
    width: Fit,
    height: Fit,
    widthPx: UInt,
    heightPx: UInt,
    density: Density,
): Size {
    val intrinsicWidth = with(density) { widthPx.toInt().toDp().value }
    val intrinsicHeight = with(density) { heightPx.toInt().toDp().value }
    if (intrinsicWidth <= 0f || intrinsicHeight <= 0f) {
        return Size(
            width = Fixed(width.clamp(0u)),
            height = Fixed(height.clamp(0u)),
        )
    }

    val minimumScale = maxOf(
        width.minimum.scaleFor(intrinsicWidth, fallback = 0f),
        height.minimum.scaleFor(intrinsicHeight, fallback = 0f),
    )
    val maximumScale = minOf(
        width.effectiveMaximum.scaleFor(intrinsicWidth, fallback = Float.POSITIVE_INFINITY),
        height.effectiveMaximum.scaleFor(intrinsicHeight, fallback = Float.POSITIVE_INFINITY),
    )
    val scale = if (minimumScale <= maximumScale) {
        1f.coerceIn(minimumScale, maximumScale)
    } else {
        // The two axes cannot both satisfy their limits without distortion. Match the existing size behavior by
        // prioritizing minimums over maximums.
        minimumScale
    }

    return Size(
        width = Fixed((intrinsicWidth * scale).toUInt()),
        height = Fixed((intrinsicHeight * scale).toUInt()),
    )
}

private fun UInt?.scaleFor(intrinsicDimension: Float, fallback: Float): Float =
    this?.toFloat()?.div(intrinsicDimension) ?: fallback

private fun SizeConstraint.adjustDimension(
    other: SizeConstraint,
    thisDimensionPx: UInt,
    otherDimensionPx: UInt,
    density: Density,
): SizeConstraint = when (this) {
    is Fit -> {
        when (other) {
            is Fit -> error("Fit dimensions must be adjusted together.")
            is Fill -> this

            is Fixed -> {
                // If the other dimension is Fixed, we'll have to scale this one by the same factor.
                val otherDimensionDp = with(density) { otherDimensionPx.toInt().toDp() }
                val scaleFactor = other.value.toFloat() / otherDimensionDp.value
                val scaledSize = with(density) {
                    (scaleFactor * thisDimensionPx.toInt()).toDp().value.toUInt()
                }
                Fixed(clamp(scaledSize))
            }
        }
    }

    is Fill,
    is Fixed,
    -> this
}
