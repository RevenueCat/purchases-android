package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.utils.serializers.EnumDeserializerWithDefault
import com.revenuecat.purchases.utils.serializers.SealedDeserializerWithDefault
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Defines how a paywall screen is transitioned when it initially appears.
 *
 * @property type The type of transition to use. Defaults to [TransitionType.Fade].
 * @property displacementStrategy Determines how/when the view hierarchy is displaced by the view being animated in.
 * @property animation Additional animation configuration for the transition.
 */
@InternalRevenueCatAPI
@Poko
@Serializable
class PaywallTransition(
    @get:JvmSynthetic val type: TransitionType = TransitionType.Fade,
    @get:JvmSynthetic
    @SerialName("displacement_strategy")
    val displacementStrategy: DisplacementStrategy,
    @get:JvmSynthetic val animation: PaywallAnimation? = null,
) {

    /**
     * Determines how the view being animated out is displaced by the view being animated in.
     *
     * A [GREEDY] displacement will result in the space being taken up by the incoming view
     * *before* it attempts to transition into the view hierarchy.
     *
     * A [LAZY] displacement will not do this, instead it will result in shifting the layout
     * as the new view inserts itself.
     */
    @InternalRevenueCatAPI
    @Serializable(with = DisplacementStrategyDeserializer::class)
    enum class DisplacementStrategy {
        @SerialName("greedy")
        GREEDY,

        @SerialName("lazy")
        LAZY,
    }

    @InternalRevenueCatAPI
    @Serializable(with = TransitionTypeDeserializer::class)
    sealed class TransitionType {
        @Serializable
        object Fade : TransitionType()

        @Serializable
        object FadeAndScale : TransitionType()

        @Serializable
        object Scale : TransitionType()

        @Serializable
        object Slide : TransitionType()
    }
}


@OptIn(InternalRevenueCatAPI::class)
internal object DisplacementStrategyDeserializer : EnumDeserializerWithDefault<PaywallTransition.DisplacementStrategy>(
    defaultValue = PaywallTransition.DisplacementStrategy.GREEDY,
    typeForValue = { value ->
        when (value) {
            PaywallTransition.DisplacementStrategy.GREEDY -> "greedy"
            PaywallTransition.DisplacementStrategy.LAZY -> "lazy"
        }
    },
)

@OptIn(InternalRevenueCatAPI::class)
internal object TransitionTypeDeserializer : SealedDeserializerWithDefault<PaywallTransition.TransitionType>(
    serialName = "TransitionType",
    serializerByType = mapOf(
        "fade" to { PaywallTransition.TransitionType.Fade.serializer() },
        "fade_and_scale" to { PaywallTransition.TransitionType.FadeAndScale.serializer() },
        "scale" to { PaywallTransition.TransitionType.Scale.serializer() },
        "slide" to { PaywallTransition.TransitionType.Slide.serializer() },
    ),
    defaultValue = { PaywallTransition.TransitionType.Fade },
)
