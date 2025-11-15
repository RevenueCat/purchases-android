package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.PartialComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride.Condition
import com.revenuecat.purchases.utils.serializers.SealedDeserializerWithDefault
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Poko
@Serializable
class ComponentOverride<T : PartialComponent>(
    @get:JvmSynthetic val conditions: List<Condition>,
    @get:JvmSynthetic val properties: T,
) {

    @Serializable(with = ConditionSerializer::class)
    sealed interface Condition {
        @Serializable
        object Compact : Condition

        @Serializable
        object Medium : Condition

        @Serializable
        object Expanded : Condition

        @Serializable
        object IntroOffer : Condition

        @Serializable
        object MultipleIntroOffers : Condition

        @Serializable
        object Selected : Condition

        @Poko
        @Serializable
        class SelectedPackage(
            @get:JvmSynthetic val operator: ArrayOperatorType,
            @get:JvmSynthetic val packages: List<String>,
        ) : Condition

        @Serializable
        object Unsupported : Condition

        @Serializable
        enum class ArrayOperatorType {
            @SerialName("in")
            IN,

            @SerialName("not_in")
            NOT_IN,
        }
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
        "multiple_intro_offers" to { Condition.MultipleIntroOffers.serializer() },
        "selected" to { Condition.Selected.serializer() },
        "selected_package" to { Condition.SelectedPackage.serializer() },
    ),
    defaultValue = { Condition.Unsupported },
)
