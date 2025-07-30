@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.modifier

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed

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
): Modifier {
    val widthModifier = when (val width = size.width) {
        is Fit -> Modifier.wrapContentWidth(align = horizontalAlignment ?: Alignment.CenterHorizontally)
        is Fill -> Modifier.fillMaxWidth()
        is Fixed -> Modifier.width(width.value.toInt().dp)
    }

    val heightModifier = when (val height = size.height) {
        is Fit -> Modifier.wrapContentHeight(align = verticalAlignment ?: Alignment.CenterVertically)
        is Fill -> Modifier.fillMaxHeight()
        is Fixed -> Modifier.height(height.value.toInt().dp)
    }

    return this then widthModifier then heightModifier
}

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
    Size_Preview(size = Size(width = Fit, height = Fit))
}

@Preview("FillFill")
@Composable
private fun Size_Preview_FillFill() {
    Size_Preview(size = Size(width = Fill, height = Fill))
}

@Preview("FillFit")
@Composable
private fun Size_Preview_FillFit() {
    Size_Preview(size = Size(width = Fill, height = Fit))
}

@Preview("FitFill")
@Composable
private fun Size_Preview_FitFill() {
    Size_Preview(size = Size(width = Fit, height = Fill))
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
                    size = Size(width = Fit, height = Fit),
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
                    size = Size(width = Fit, height = Fit),
                    verticalAlignment = Alignment.Bottom,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "Hello world!")
        }
    }
}
