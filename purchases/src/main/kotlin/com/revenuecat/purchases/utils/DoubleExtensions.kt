package com.revenuecat.purchases.utils

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Rounds down (truncates) a Double to the specified number of decimal places.
 * Uses floor to ensure we never round up, which is important for price display.
 */
internal fun Double.roundToDecimalPlaces(decimals: Int): Double {
    return BigDecimal.valueOf(this)
        .setScale(decimals, RoundingMode.FLOOR)
        .toDouble()
}
