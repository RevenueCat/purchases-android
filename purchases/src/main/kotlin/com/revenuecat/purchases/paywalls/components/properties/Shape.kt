package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.InternalRevenueCatAPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Serializable
internal sealed interface Shape {

    @Serializable
    @SerialName("rectangle")
    data class Rectangle(
        @get:JvmSynthetic val corners: CornerRadiuses? = null,
    ) : Shape

    @Serializable
    @SerialName("pill")
    object Pill : Shape
}
