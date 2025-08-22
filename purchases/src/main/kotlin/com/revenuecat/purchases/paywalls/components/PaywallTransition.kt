package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

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
    @Serializable
    enum class DisplacementStrategy {
        @SerialName("greedy")
        GREEDY,

        @SerialName("lazy")
        LAZY,
    }

    @InternalRevenueCatAPI
    @Serializable
    @JsonClassDiscriminator("type")
    sealed class TransitionType {
        @Serializable
        @SerialName("fade")
        object Fade : TransitionType()

        @Serializable
        @SerialName("fade_and_scale")
        object FadeAndScale : TransitionType()

        @Serializable
        @SerialName("scale")
        object Scale : TransitionType()

        @Serializable
        @SerialName("slide")
        object Slide : TransitionType()

        @Poko
        @Serializable
        @SerialName("custom")
        class Custom(
            @get:JvmSynthetic val value: String,
        ) : TransitionType()
    }
}
