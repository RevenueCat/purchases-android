package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.InternalRevenueCatAPI
import kotlinx.serialization.Serializable

/**
 * Contains padding values for 4 axes, in dp.
 */
@InternalRevenueCatAPI
@Serializable
internal data class Padding(
    /**
     * The top padding, in dp.
     */
    val top: Double,
    /**
     * The bottom padding, in dp.
     */
    val bottom: Double,
    /**
     * The leading, or start, padding, in dp.
     */
    val leading: Double,
    /**
     * The trailing, or end, padding, in dp.
     */
    val trailing: Double,
) {
    companion object {
        val zero = Padding(0.0, 0.0, 0.0, 0.0)
        val default = Padding(10.0, 10.0, 20.0, 20.0)
    }
}
