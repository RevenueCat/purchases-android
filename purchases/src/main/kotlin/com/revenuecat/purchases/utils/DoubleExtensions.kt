package com.revenuecat.purchases.utils

import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.round

private const val DECIMAL_BASE = 10.0

/**
 * Rounds down (truncates) a Double to the specified number of decimal places.
 * Uses floor to ensure we never round up, which is important for derived price calculations
 * like pricePerMonth and pricePerYear where we don't want to round up.
 */
internal fun Double.truncateToDecimalPlaces(decimals: Int): Double {
    val divisor = DECIMAL_BASE.pow(decimals.toDouble())
    return floor(this * divisor) / divisor
}

/**
 * Rounds a Double to the specified number of decimal places using half-up rounding.
 * This is the standard rounding behavior where 0.5 rounds up (e.g., 19.995 -> 20.00).
 * Use this for exact price values where floating-point imprecision should be corrected.
 */
internal fun Double.roundToDecimalPlaces(decimals: Int): Double {
    val divisor = DECIMAL_BASE.pow(decimals.toDouble())
    return round(this * divisor) / divisor
}
