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
        object IntroOffer : Condition

        @Serializable
        object MultipleIntroOffers : Condition

        @Serializable
        object Selected : Condition

        @Serializable
        object Unsupported : Condition

        @Serializable
        data class Orientation(
            @SerialName("operator") val operator: ArrayOperatorType,
            @SerialName("orientations") val orientations: List<OrientationType>,
        ) : Condition

        @Serializable
        data class ScreenSize(
            @SerialName("operator") val operator: ArrayOperatorType,
            @SerialName("sizes") val sizes: List<String>,
        ) : Condition

        @Serializable
        data class SelectedPackage(
            @SerialName("operator") val operator: ArrayOperatorType,
            @SerialName("packages") val packages: List<String>,
        ) : Condition

        @Serializable
        enum class ArrayOperatorType {
            @SerialName("in")
            IN,

            @SerialName("not_in")
            NOT_IN,
        }

        @Serializable
        enum class OrientationType {
            @SerialName("portrait")
            PORTRAIT,

            @SerialName("landscape")
            LANDSCAPE,
        }
    }
}

@OptIn(InternalRevenueCatAPI::class)
internal object ConditionSerializer : SealedDeserializerWithDefault<Condition>(
    serialName = "Condition",
    serializerByType = mapOf(
        "intro_offer" to { Condition.IntroOffer.serializer() },
        "multiple_intro_offers" to { Condition.MultipleIntroOffers.serializer() },
        "selected" to { Condition.Selected.serializer() },
        "orientation" to { Condition.Orientation.serializer() },
        "screen_size" to { Condition.ScreenSize.serializer() },
        "selected_package" to { Condition.SelectedPackage.serializer() },
    ),
    defaultValue = { Condition.Unsupported },
)
