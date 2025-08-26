package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.utils.serializers.EnumAsObjectSerializer
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

    /* Defines the types of animations a the paywall can use
    *
    * [NOTE] This is serialized as an object instead of a top level enum so that it can be expanded
    * later to include user defined transitions if we choose to go there
    */
    @InternalRevenueCatAPI
    @Serializable(with = AnimationTypeAsObjectSerializer::class)
    enum class AnimationType {
        EASE_IN,
        EASE_OUT,
        EASE_IN_OUT,
        LINEAR;
    }
}

@OptIn(InternalRevenueCatAPI::class)
object AnimationTypeAsObjectSerializer : EnumAsObjectSerializer<PaywallAnimation.AnimationType>(
    enumClass = PaywallAnimation.AnimationType::class,
    defaultValue = PaywallAnimation.AnimationType.EASE_IN_OUT,
    keyName = "type"
)
