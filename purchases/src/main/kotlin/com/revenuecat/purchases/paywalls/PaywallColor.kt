package com.revenuecat.purchases.paywalls

import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Represents a color to be used by `RevenueCatUI`.
 */
data class PaywallColor(
    /**
     * The original Hex representation for this color.
     */
    val stringRepresentation: String,
    /**
     * The underlying `Color`.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    val underlyingColor: Color?,
) {
    /**
     * Creates a color from a Hex string: `#RRGGBB` or `#RRGGBBAA`.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    constructor(stringRepresentation: String) : this(
        stringRepresentation,
        Color.valueOf(Color.parseColor(stringRepresentation)),
    )
}

/**
 * Represents a color scheme.
 */
enum class ColorScheme {
    LIGHT, DARK
}
