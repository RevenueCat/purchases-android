package com.revenuecat.purchases.utils

import kotlin.math.floor
import kotlin.math.pow

private const val DECIMAL_BASE = 10.0

/**
 * Rounds down (truncates) a Double to the specified number of decimal places.
 * Uses floor to ensure we never round up, which is important for price display.
 */
internal fun Double.roundToDecimalPlaces(decimals: Int): Double {
    val divisor = DECIMAL_BASE.pow(decimals.toDouble())
    return floor(this * divisor) / divisor
}
