package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.utils.serializers.SealedDeserializerWithDefault
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

    /**
     * Defines the type of animation to use for paywall transitions.
     *
     * [NOTE] This is a sealed class and not an enum because we see a future where we may want
     * to pass back more verbose instructions to the view layer than a simple enum case
     */
    @InternalRevenueCatAPI
    @Serializable(with = AnimationTypeDeserializer::class)
    sealed class AnimationType {
        @Serializable
        object EaseIn : AnimationType()

        @Serializable
        object EaseInOut : AnimationType()

        @Serializable
        object EaseOut : AnimationType()

        @Serializable
        object Linear : AnimationType()
    }
}

@OptIn(InternalRevenueCatAPI::class)
internal object AnimationTypeDeserializer : SealedDeserializerWithDefault<PaywallAnimation.AnimationType>(
    serialName = "AnimationType",
    serializerByType = mapOf(
        "ease_in" to { PaywallAnimation.AnimationType.EaseIn.serializer() },
        "ease_out" to { PaywallAnimation.AnimationType.EaseOut.serializer() },
        "ease_in_out" to { PaywallAnimation.AnimationType.EaseInOut.serializer() },
        "linear" to { PaywallAnimation.AnimationType.Linear.serializer() },
    ),
    defaultValue = { PaywallAnimation.AnimationType.EaseInOut },
)
