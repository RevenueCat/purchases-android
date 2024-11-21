package com.revenuecat.purchases.paywalls.components.properties

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class Size(
    val width: SizeConstraint,
    val height: SizeConstraint,
)

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
