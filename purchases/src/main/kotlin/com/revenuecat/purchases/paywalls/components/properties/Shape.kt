package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Serializable
sealed interface Shape {

    @Serializable
    @Poko
    @SerialName("rectangle")
    class Rectangle(
        @get:JvmSynthetic val corners: CornerRadiuses? = null,
    ) : Shape

    @Serializable
    @SerialName("pill")
    object Pill : Shape
}
