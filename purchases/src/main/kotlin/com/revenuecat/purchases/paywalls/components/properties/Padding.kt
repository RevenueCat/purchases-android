package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

/**
 * Contains padding values for 4 axes, in dp.
 */
@InternalRevenueCatAPI
@Poko
@Serializable
class Padding(
    /**
     * The top padding, in dp.
     */
    @get:JvmSynthetic val top: Double = 0.0,
    /**
     * The bottom padding, in dp.
     */
    @get:JvmSynthetic val bottom: Double = 0.0,
    /**
     * The leading, or start, padding, in dp.
     */
    @get:JvmSynthetic val leading: Double = 0.0,
    /**
     * The trailing, or end, padding, in dp.
     */
    @get:JvmSynthetic val trailing: Double = 0.0,
) {
    companion object {
        @get:JvmSynthetic
        val zero = Padding(0.0, 0.0, 0.0, 0.0)

        @get:JvmSynthetic
        val default = Padding(10.0, 10.0, 20.0, 20.0)
    }
}
