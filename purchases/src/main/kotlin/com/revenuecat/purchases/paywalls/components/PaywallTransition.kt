package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.utils.serializers.EnumDeserializerWithDefault
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Defines how a paywall screen is transitioned when it initially appears.
 *
 * @property type The type of transition to use. Defaults to [TransitionType.FADE].
 * @property displacementStrategy Determines how/when the view hierarchy is displaced by the view being animated in.
 * @property animation Additional animation configuration for the transition.
 */
@InternalRevenueCatAPI
@Poko
@Serializable
public class PaywallTransition(
    @get:JvmSynthetic val type: TransitionType = TransitionType.FADE,
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
    @Serializable(with = DisplacementStrategyDeserializer::class)
    enum class DisplacementStrategy {
        @SerialName("greedy")
        GREEDY,

        @SerialName("lazy")
        LAZY,
    }

    @Serializable(with = TransitionTypeSerializer::class)
    enum class TransitionType {
        FADE,
        FADE_AND_SCALE,
        SCALE,
        SLIDE,
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
internal object TransitionTypeSerializer : EnumDeserializerWithDefault<PaywallTransition.TransitionType>(
    defaultValue = PaywallTransition.TransitionType.FADE,
    typeForValue = { value ->
        when (value) {
            PaywallTransition.TransitionType.FADE -> "fade"
            PaywallTransition.TransitionType.FADE_AND_SCALE -> "fade_and_scale"
            PaywallTransition.TransitionType.SCALE -> "scale"
            PaywallTransition.TransitionType.SLIDE -> "slide"
        }
    },
)
