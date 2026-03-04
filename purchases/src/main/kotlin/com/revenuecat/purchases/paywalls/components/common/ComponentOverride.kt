package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.PartialComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride.Condition
import com.revenuecat.purchases.utils.serializers.SealedDeserializerWithDefault
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@InternalRevenueCatAPI
@Poko
@Serializable
public class ComponentOverride<T : PartialComponent>(
    @get:JvmSynthetic public val conditions: List<Condition>,
    @get:JvmSynthetic public val properties: T,
) {

    @Serializable
    public enum class EqualityOperator {
        @SerialName("=")
        EQUALS,

        @SerialName("!=")
        NOT_EQUALS,
    }

    @Serializable
    public enum class ArrayOperator {
        @SerialName("in")
        IN,

        @SerialName("not in")
        NOT_IN,
    }

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
        public data class IntroOfferCondition(
            public val operator: EqualityOperator,
            public val value: Boolean,
        ) : Condition

        @Serializable
        public object MultiplePhaseOffers : Condition

        @Serializable
        public object Selected : Condition

        @Serializable
        public object PromoOffer : Condition

        @Serializable
        public data class PromoOfferCondition(
            public val operator: EqualityOperator,
            public val value: Boolean,
        ) : Condition

        @Serializable
        public data class SelectedPackage(
            public val operator: ArrayOperator,
            public val packages: List<String>,
        ) : Condition

        @Serializable
        public data class Variable(
            public val operator: EqualityOperator,
            public val variable: String,
            public val value: JsonPrimitive,
        ) : Condition

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
        "intro_offer_condition" to { Condition.IntroOfferCondition.serializer() },
        "multiple_intro_offers" to { Condition.MultiplePhaseOffers.serializer() },
        "selected" to { Condition.Selected.serializer() },
        "promo_offer" to { Condition.PromoOffer.serializer() },
        "promo_offer_condition" to { Condition.PromoOfferCondition.serializer() },
        "selected_package_condition" to { Condition.SelectedPackage.serializer() },
        "variable_condition" to { Condition.Variable.serializer() },
    ),
    defaultValue = { Condition.Unsupported },
)
