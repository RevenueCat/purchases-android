@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.modifier

import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed

internal val SizeConstraint.minimum: UInt?
    get() = when (this) {
        is Fit -> min
        is Fill -> min
        is Fixed -> null
    }

internal val SizeConstraint.effectiveMaximum: UInt?
    get() {
        val declaredMaximum = when (this) {
            is Fit -> max
            is Fill -> max
            is Fixed -> null
        }
        return declaredMaximum?.let { maxOf(it, minimum ?: 0u) }
    }

internal fun SizeConstraint.clamp(value: UInt): UInt {
    val minimum = minimum ?: 0u
    val maximum = effectiveMaximum ?: UInt.MAX_VALUE
    return value.coerceIn(minimum, maximum)
}
