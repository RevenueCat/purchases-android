@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.modifier

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import kotlin.math.roundToInt

/**
 * @param horizontalAlignment Alignment to apply when the provided [size]'s width is [Fit], and the component is
 * forced to be wider than its contents, e.g. using [widthIn] or [requiredWidth].
 * @param verticalAlignment Alignment to apply when the provided [size]'s height is [Fit], and the component is
 * forced to be taller than its contents, e.g. using [heightIn] or [requiredHeight].
 */
@JvmSynthetic
@Stable
internal fun Modifier.size(
    size: Size,
    horizontalAlignment: Alignment.Horizontal? = null,
    verticalAlignment: Alignment.Vertical? = null,
): Modifier = this.layout { measurable, incomingConstraints ->
    val widthConstraints = size.width.measurementConstraints(
        incomingMin = incomingConstraints.minWidth,
        incomingMax = incomingConstraints.maxWidth,
        density = this,
    )
    val heightConstraints = size.height.measurementConstraints(
        incomingMin = incomingConstraints.minHeight,
        incomingMax = incomingConstraints.maxHeight,
        density = this,
    )
    val placeable = measurable.measure(
        Constraints(
            minWidth = widthConstraints.min,
            maxWidth = widthConstraints.max,
            minHeight = heightConstraints.min,
            maxHeight = heightConstraints.max,
        ),
    )

    val layoutWidth = placeable.width.coerceIn(incomingConstraints.minWidth, incomingConstraints.maxWidth)
    val layoutHeight = placeable.height.coerceIn(incomingConstraints.minHeight, incomingConstraints.maxHeight)
    val x = (horizontalAlignment ?: Alignment.CenterHorizontally).align(
        size = placeable.width,
        space = layoutWidth,
        layoutDirection = layoutDirection,
    )
    val y = (verticalAlignment ?: Alignment.CenterVertically).align(
        size = placeable.height,
        space = layoutHeight,
    )

    layout(layoutWidth, layoutHeight) {
        placeable.placeRelative(x, y)
    }
}

private data class AxisConstraints(
    val min: Int,
    val max: Int,
)

private fun SizeConstraint.measurementConstraints(
    incomingMin: Int,
    incomingMax: Int,
    density: Density,
): AxisConstraints = when (this) {
    is Fit -> {
        val limits = limitsInPx(density)
        val maximum = maxOf(limits.min, minOf(incomingMax, limits.max))
        AxisConstraints(min = limits.min, max = maximum)
    }
    is Fill -> {
        val limits = limitsInPx(density)
        if (incomingMax != Constraints.Infinity) {
            val resolved = limits.clamp(incomingMax)
            AxisConstraints(min = resolved, max = resolved)
        } else {
            val minimum = limits.clamp(incomingMin)
            AxisConstraints(min = minimum, max = maxOf(minimum, limits.max))
        }
    }
    is Fixed -> {
        val resolved = this.value.toPx(density).coerceIn(incomingMin, incomingMax)
        AxisConstraints(min = resolved, max = resolved)
    }
}

private data class Limits(
    val min: Int,
    val max: Int,
) {
    fun clamp(value: Int): Int = value.coerceIn(min, max)
}

private fun SizeConstraint.limitsInPx(density: Density): Limits {
    val minimumPx = minimum?.toPx(density) ?: 0
    val maximumPx = effectiveMaximum?.toPx(density) ?: Constraints.Infinity
    return Limits(min = minimumPx, max = maximumPx)
}

private fun UInt.toPx(density: Density): Int =
    (toDouble() * density.density).roundToInt().coerceAtLeast(0)

@Composable
private fun Size_Preview(size: Size) {
    Box(
        modifier = Modifier.requiredSize(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .background(Color.Red)
                .size(
                    size = size,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "Hello world!")
        }
    }
}

@Preview("FitFit")
@Composable
private fun Size_Preview_FitFit() {
    Size_Preview(size = Size(width = Fit(), height = Fit()))
}

@Preview("FillFill")
@Composable
private fun Size_Preview_FillFill() {
    Size_Preview(size = Size(width = Fill(), height = Fill()))
}

@Preview("FillFit")
@Composable
private fun Size_Preview_FillFit() {
    Size_Preview(size = Size(width = Fill(), height = Fit()))
}

@Preview("FitFill")
@Composable
private fun Size_Preview_FitFill() {
    Size_Preview(size = Size(width = Fit(), height = Fill()))
}

@Preview("FixedFixed")
@Composable
private fun Size_Preview_FixedFixed() {
    Size_Preview(size = Size(width = Fixed(50.toUInt()), height = Fixed(50.toUInt())))
}

@Preview
@Composable
private fun Size_Preview_HorizontalAlignment() {
    Box(
        modifier = Modifier.requiredSize(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .background(Color.Red)
                // With requiredWidth + Fit, the horizontalAlignment applies.
                .requiredWidth(150.dp)
                .size(
                    size = Size(width = Fit(), height = Fit()),
                    horizontalAlignment = Alignment.End,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "Hello world!")
        }
    }
}

@Preview
@Composable
private fun Size_Preview_VerticalAlignment() {
    Box(
        modifier = Modifier.requiredSize(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .background(Color.Red)
                // With requiredHeight + Fit, the verticalAlignment applies.
                .requiredHeight(150.dp)
                .size(
                    size = Size(width = Fit(), height = Fit()),
                    verticalAlignment = Alignment.Bottom,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "Hello world!")
        }
    }
}
