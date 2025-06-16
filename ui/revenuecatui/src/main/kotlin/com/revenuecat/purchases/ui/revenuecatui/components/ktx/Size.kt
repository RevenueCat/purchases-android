@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.ktx

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.LayoutDirection
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.ui.revenuecatui.extensions.calculateHorizontalPadding
import com.revenuecat.purchases.ui.revenuecatui.extensions.calculateVerticalPadding

@JvmSynthetic
internal fun Size.adjustForMargin(margin: PaddingValues, layoutDirection: LayoutDirection): Size =
    Size(
        width = width.adjustDimensionForMargin(margin.calculateHorizontalPadding(layoutDirection).value.toUInt()),
        height = height.adjustDimensionForMargin(margin.calculateVerticalPadding().value.toUInt()),
    )

private fun SizeConstraint.adjustDimensionForMargin(
    margin: UInt,
): SizeConstraint = when (this) {
    is Fixed -> Fixed(value + margin)
    is Fill,
    is Fit,
    -> this
}
