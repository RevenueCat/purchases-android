package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.InternalRevenueCatAPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Serializable
internal data class Size(
    val width: SizeConstraint,
    val height: SizeConstraint,
)

@InternalRevenueCatAPI
@Serializable
internal sealed interface SizeConstraint {

    @Serializable
    @SerialName("fit")
    object Fit : SizeConstraint

    @Serializable
    @SerialName("fill")
    object Fill : SizeConstraint

    @Serializable
    @SerialName("fixed")
    data class Fixed(
        val value: UInt,
    ) : SizeConstraint
}
