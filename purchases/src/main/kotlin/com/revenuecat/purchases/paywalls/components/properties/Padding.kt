package com.revenuecat.purchases.paywalls.components.properties

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

/**
 * Contains padding values for 4 axes, in dp.
 */
@InternalRevenueCatAPI
@Poko
@Immutable
@Serializable
public class Padding(
    /**
     * The top padding, in dp.
     */
    public @get:JvmSynthetic val top: Double = 0.0,
    /**
     * The bottom padding, in dp.
     */
    public @get:JvmSynthetic val bottom: Double = 0.0,
    /**
     * The leading, or start, padding, in dp.
     */
    public @get:JvmSynthetic val leading: Double = 0.0,
    /**
     * The trailing, or end, padding, in dp.
     */
    public @get:JvmSynthetic val trailing: Double = 0.0,
) {
    public companion object {
        @get:JvmSynthetic
        public val zero: Padding = Padding(0.0, 0.0, 0.0, 0.0)

        @get:JvmSynthetic
        public val default: Padding = Padding(10.0, 10.0, 20.0, 20.0)
    }
}
