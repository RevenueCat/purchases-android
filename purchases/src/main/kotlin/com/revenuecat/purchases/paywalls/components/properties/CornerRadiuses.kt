package com.revenuecat.purchases.paywalls.components.properties

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Contains radius values for 4 corners, in dp.
 */
@Serializable
internal data class CornerRadiuses(
    /**
     * The top-leading, or top-start, corner radius, in dp.
     */
    @SerialName("top_leading")
    val topLeading: Double,
    /**
     * The top-trailing, or top-end, corner radius, in dp.
     */
    @SerialName("top_trailing")
    val topTrailing: Double,
    /**
     * The bottom-leading, or bottom-start, corner radius, in dp.
     */
    @SerialName("bottom_leading")
    val bottomLeading: Double,
    /**
     * The bottom-trailing, or bottom-end, corner radius, in dp.
     */
    @SerialName("bottom_trailing")
    val bottomTrailing: Double,
) {
    companion object {
        val zero = CornerRadiuses(0.0, 0.0, 0.0, 0.0)
        val default = zero
    }
}
