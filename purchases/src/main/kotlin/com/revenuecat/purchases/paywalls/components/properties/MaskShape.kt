package com.revenuecat.purchases.paywalls.components.properties

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface MaskShape {

    @Serializable
    @SerialName("rectangle")
    data class Rectangle(
        val corners: CornerRadiuses? = null,
    ) : MaskShape

    @Serializable
    @SerialName("pill")
    object Pill : MaskShape

    @Serializable
    @SerialName("concave")
    object Concave : MaskShape

    @Serializable
    @SerialName("convex")
    object Convex : MaskShape
}