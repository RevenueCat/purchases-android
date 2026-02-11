package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.utils.serializers.EnumDeserializerWithDefault
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
public class PaywallAnimation(
    public @get:JvmSynthetic val type: AnimationType,
    public @get:JvmSynthetic @SerialName("ms_delay") val msDelay: Int,
    public @get:JvmSynthetic @SerialName("ms_duration") val msDuration: Int,
) {

    @Serializable(with = AnimationTypeSerializer::class)
    public enum class AnimationType {
        EASE_IN,
        EASE_OUT,
        EASE_IN_OUT,
        LINEAR,
    }
}

@OptIn(InternalRevenueCatAPI::class)
internal object AnimationTypeSerializer : EnumDeserializerWithDefault<PaywallAnimation.AnimationType>(
    defaultValue = PaywallAnimation.AnimationType.EASE_IN_OUT,
    typeForValue = { value ->
        when (value) {
            PaywallAnimation.AnimationType.EASE_IN -> "ease_in"
            PaywallAnimation.AnimationType.EASE_OUT -> "ease_out"
            PaywallAnimation.AnimationType.EASE_IN_OUT -> "ease_in_out"
            PaywallAnimation.AnimationType.LINEAR -> "linear"
        }
    },
)
