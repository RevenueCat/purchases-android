package com.revenuecat.purchases.paywalls.components.properties

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class Size(
    val width: SizeConstraint,
    val height: SizeConstraint,
)

@Serializable
internal sealed class SizeConstraint {

    @Serializable
    @SerialName("fit")
    internal object Fit : SizeConstraint()

    @Serializable
    @SerialName("fill")
    internal object Fill : SizeConstraint()

    @Serializable
    @SerialName("fixed")
    internal data class Fixed(
        val value: UInt,
    ) : SizeConstraint()
}
