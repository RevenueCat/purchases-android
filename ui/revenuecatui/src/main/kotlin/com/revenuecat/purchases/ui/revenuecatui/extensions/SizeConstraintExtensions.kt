@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.extensions

import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint

@JvmSynthetic
internal fun SizeConstraint.dpOrNull() = when (this) {
    is SizeConstraint.Fixed -> value.toInt().dp
    is SizeConstraint.Fill -> null
    is SizeConstraint.Fit -> null
}
