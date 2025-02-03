@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.extensions

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint

@get:JvmSynthetic
internal val SizeConstraint.dp: Dp?
    get() = when (this) {
        is SizeConstraint.Fixed -> value.toInt().dp
        is SizeConstraint.Fill -> null
        is SizeConstraint.Fit -> null
    }
