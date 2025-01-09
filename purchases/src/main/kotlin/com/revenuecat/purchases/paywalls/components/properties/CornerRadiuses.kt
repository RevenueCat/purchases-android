package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Contains radius values for 4 corners, in dp.
 */
@InternalRevenueCatAPI
@Poko
@Serializable
class CornerRadiuses(
    /**
     * The top-leading, or top-start, corner radius, in dp.
     */
    @get:JvmSynthetic
    @SerialName("top_leading")
    val topLeading: Double,
    /**
     * The top-trailing, or top-end, corner radius, in dp.
     */
    @get:JvmSynthetic
    @SerialName("top_trailing")
    val topTrailing: Double,
    /**
     * The bottom-leading, or bottom-start, corner radius, in dp.
     */
    @get:JvmSynthetic
    @SerialName("bottom_leading")
    val bottomLeading: Double,
    /**
     * The bottom-trailing, or bottom-end, corner radius, in dp.
     */
    @get:JvmSynthetic
    @SerialName("bottom_trailing")
    val bottomTrailing: Double,
) {
    companion object {
        @get:JvmSynthetic val zero = CornerRadiuses(0.0, 0.0, 0.0, 0.0)

        @get:JvmSynthetic val default = zero
    }

    fun copy(
        topLeading: Double = this.topLeading,
        topTrailing: Double = this.topTrailing,
        bottomLeading: Double = this.bottomLeading,
        bottomTrailing: Double = this.bottomTrailing,
    ): CornerRadiuses {
        return CornerRadiuses(topLeading, topTrailing, bottomLeading, bottomTrailing)
    }
}
