package com.revenuecat.purchases.paywalls.components.properties

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed class MaskShape {

    @Serializable
    @SerialName("rectangle")
    internal data class Rectangle(
        val corners: CornerRadiuses? = null,
    ) : MaskShape()

    @Serializable
    @SerialName("pill")
    internal object Pill : MaskShape()

    @Serializable
    @SerialName("concave")
    internal object Concave : MaskShape()

    @Serializable
    @SerialName("convex")
    internal object Convex : MaskShape()
}
