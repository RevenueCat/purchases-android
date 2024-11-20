package com.revenuecat.purchases.paywalls.components.properties

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed class Shape {

    @Serializable
    @SerialName("rectangle")
    internal data class Rectangle(
        val corners: CornerRadiuses? = null,
    ) : Shape()

    @Serializable
    @SerialName("pill")
    internal object Pill : Shape()
}
