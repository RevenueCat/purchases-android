package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Serializable
sealed interface MaskShape {

    @Poko
    @Serializable
    @SerialName("rectangle")
    class Rectangle(
        @get:JvmSynthetic val corners: CornerRadiuses? = null,
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

    @Serializable
    @SerialName("circle")
    object Circle : MaskShape
}
