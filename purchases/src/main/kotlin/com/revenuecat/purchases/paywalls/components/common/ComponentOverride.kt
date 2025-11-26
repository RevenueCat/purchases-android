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
        /**
         * Compares the selected package's intro-offer eligibility (not the whole paywall) against [value].
         * This matches the package the customer currently has highlighted in the UI.
         *
         * Default values provide backward compatibility with legacy JSON that only has `{"type": "intro_offer"}`.
         */
        @Serializable
        data class IntroOffer(
            @SerialName("operator") val operator: EqualityOperatorType = EqualityOperatorType.EQUALS,
            @SerialName("value") val value: Boolean = true,
        ) : Condition

        /**
         * Compares whether the selected package exposes multiple intro offers. Other packages are ignored.
         *
         * Default values provide backward compatibility with legacy JSON.
         */
        @Serializable
        data class MultipleIntroOffers(
            @SerialName("operator") val operator: EqualityOperatorType = EqualityOperatorType.EQUALS,
            @SerialName("value") val value: Boolean = true,
        ) : Condition

        /**
         * Compares against whether any package on the paywall has an intro offer.
         */
        @Serializable
        data class AnyIntroOffer(
            @SerialName("operator") val operator: EqualityOperatorType,
            @SerialName("value") val value: Boolean,
        ) : Condition

        /**
         * Compares against whether any package on the paywall exposes multiple intro offers.
         */
        @Serializable
        data class AnyMultipleIntroOffers(
            @SerialName("operator") val operator: EqualityOperatorType,
            @SerialName("value") val value: Boolean,
        ) : Condition

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
        enum class EqualityOperatorType {
            @SerialName("=") // WIP… Should we make this human language from the server like in and not_in?
            EQUALS,

            @SerialName("!=") // WIP… Should we make this human language from the server like in and not_in?
            NOT_EQUALS,
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
        "introductory_offer_available" to { Condition.AnyIntroOffer.serializer() },
        "multiple_intro_offers_available" to { Condition.AnyMultipleIntroOffers.serializer() },
        "selected" to { Condition.Selected.serializer() },
        "orientation" to { Condition.Orientation.serializer() },
        "screen_size" to { Condition.ScreenSize.serializer() },
        "selected_package" to { Condition.SelectedPackage.serializer() },
    ),
    defaultValue = { Condition.Unsupported },
)
