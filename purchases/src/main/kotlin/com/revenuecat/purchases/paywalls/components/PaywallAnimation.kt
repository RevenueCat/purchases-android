package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * Defines an animation to be used for paywall transitions.
 *
 * @property type The type of animation to use like ease in, ease out, etc.
 * @property msDelay The delay in milliseconds before the animation starts.
 * @property msDuration The duration in milliseconds of the animation.
 */
@Suppress("unused")
@InternalRevenueCatAPI
@Poko
@Serializable
@SerialName("animation")
class PaywallAnimation(
    @get:JvmSynthetic val type: AnimationType,
    @get:JvmSynthetic
    @SerialName("ms_delay")
    val msDelay: Int?,
    @get:JvmSynthetic
    @SerialName("ms_duration")
    val msDuration: Int?,
) {

    @OptIn(ExperimentalSerializationApi::class)
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
        @SerialName("ease_out")
        object EaseOut : AnimationType()

        @Serializable
        @SerialName("linear")
        object Linear : AnimationType()

        @Poko
        @Serializable
        @SerialName("custom")
        class Custom(
            @get:JvmSynthetic val value: String,
        ) : AnimationType()
    }
}
