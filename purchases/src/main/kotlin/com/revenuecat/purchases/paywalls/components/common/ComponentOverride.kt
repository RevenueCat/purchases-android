package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.PartialComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride.Condition
import com.revenuecat.purchases.utils.serializers.SealedDeserializerWithDefault
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Poko
@Serializable
public class ComponentOverride<T : PartialComponent>(
    public @get:JvmSynthetic val conditions: List<Condition>,
    public @get:JvmSynthetic val properties: T,
) {

    @Serializable(with = ConditionSerializer::class)
    public sealed interface Condition {
        @Serializable
        public object Compact : Condition

        @Serializable
        public object Medium : Condition

        @Serializable
        public object Expanded : Condition

        @Serializable
        public object IntroOffer : Condition

        @Serializable
        public object MultiplePhaseOffers : Condition

        @Serializable
        public object Selected : Condition

        @Serializable
        public object PromoOffer : Condition

        @Serializable
        public object Unsupported : Condition
    }
}

@OptIn(InternalRevenueCatAPI::class)
internal object ConditionSerializer : SealedDeserializerWithDefault<Condition>(
    serialName = "Condition",
    serializerByType = mapOf(
        "compact" to { Condition.Compact.serializer() },
        "medium" to { Condition.Medium.serializer() },
        "expanded" to { Condition.Expanded.serializer() },
        "intro_offer" to { Condition.IntroOffer.serializer() },
        "multiple_intro_offers" to { Condition.MultiplePhaseOffers.serializer() },
        "selected" to { Condition.Selected.serializer() },
        "promo_offer" to { Condition.PromoOffer.serializer() },
    ),
    defaultValue = { Condition.Unsupported },
)
