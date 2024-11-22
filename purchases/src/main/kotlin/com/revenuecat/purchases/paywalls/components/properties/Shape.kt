package com.revenuecat.purchases.paywalls.components.properties

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface Shape {

    @Serializable
    @SerialName("rectangle")
    data class Rectangle(
        val corners: CornerRadiuses? = null,
    ) : Shape

    @Serializable
    @SerialName("pill")
    object Pill : Shape
}
