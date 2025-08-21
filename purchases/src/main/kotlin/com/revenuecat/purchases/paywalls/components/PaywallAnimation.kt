package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@InternalRevenueCatAPI
@Poko
@Serializable
@SerialName("animation")
class PaywallAnimation(
    @get:JvmSynthetic val type: AnimationType,
    @get:JvmSynthetic val msDelay: Int?,
    @get:JvmSynthetic val msDuration: Int?
) {

    @InternalRevenueCatAPI
    @Serializable
    @JsonClassDiscriminator("type")
    sealed class AnimationType {
        @Serializable
        @SerialName("ease_in")
        object EaseIn : AnimationType()

        @Serializable
        @SerialName("ease_in_out")
        object EaseInOut : AnimationType()

        @Serializable
        @SerialName("easeOut")
        object EaseOut : AnimationType()

        @Serializable
        @SerialName("linear")
        object Linear : AnimationType()

        @Serializable
        @SerialName("bouncy")
        object Bouncy : AnimationType()

        @Serializable
        @SerialName("smooth")
        object Smooth : AnimationType()

        @Serializable
        @SerialName("spring")
        object Spring : AnimationType()


        @Poko
        @Serializable
        @SerialName("custom")
        class Custom(
            @get:JvmSynthetic val value: String,
        ) : AnimationType()
    }
}