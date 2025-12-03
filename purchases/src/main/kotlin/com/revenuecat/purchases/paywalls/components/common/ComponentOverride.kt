package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.PartialComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride.Condition
import com.revenuecat.purchases.utils.serializers.SealedDeserializerWithDefault
import com.revenuecat.purchases.utils.serializers.VersionIntSerializer
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
         *
         * Assuming the user is eligible for the offer
         */
        @Serializable
        data class AnyPackageContainsIntroOffer(
            @SerialName("operator") val operator: EqualityOperatorType,
            @SerialName("value") val value: Boolean,
        ) : Condition

        /**
         * Compares against whether any package on the paywall exposes multiple intro offers.
         *
         * Assuming the user is eligible for the offers
         */
        @Serializable
        data class AnyPackageContainsMultipleIntroOffers(
            @SerialName("operator") val operator: EqualityOperatorType,
            @SerialName("value") val value: Boolean,
        ) : Condition

        /**
         * Is the current component in a selected state?
         * */
        @Serializable
        object Selected : Condition

        @Serializable
        object Unsupported : Condition

        /**
         * Compares the current device orientation against the list of [orientations].
         * */
        @Serializable
        data class Orientation(
            @SerialName("operator") val operator: ArrayOperatorType,
            @SerialName("orientations") val orientations: List<OrientationType>,
        ) : Condition

        /**
         * Compares the current screen size against the list of [sizes].
         * */
        @Serializable
        data class ScreenSize(
            @SerialName("operator") val operator: ArrayOperatorType,
            @SerialName("sizes") val sizes: List<String>,
        ) : Condition

        /**
         * Compares the selected package against the list of [packages].
         * */
        @Serializable
        data class SelectedPackage(
            @SerialName("operator") val operator: ArrayOperatorType,
            @SerialName("packages") val packages: List<String>,
        ) : Condition

        /**
         * Compares the app version against the [version].
         * */
        @Serializable
        data class AppVersion(
            @SerialName("operator") val operator: ComparisonOperatorType,
            @SerialName("android_version")
            @Serializable(with = VersionIntSerializer::class)
            val version: Int,
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
            @SerialName("=")
            EQUALS,

            @SerialName("!=")
            NOT_EQUALS,
        }

        @Serializable
        enum class ComparisonOperatorType {
            @SerialName("=")
            EQUALS,

            @SerialName("<")
            LESS_THAN,

            @SerialName("<=")
            LESS_THAN_OR_EQUAL_TO,

            @SerialName(">")
            GREATER_THAN,

            @SerialName(">=")
            GREATER_THAN_OR_EQUAL_TO,
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
        "introductory_offer_available" to { Condition.AnyPackageContainsIntroOffer.serializer() },
        "multiple_intro_offers_available" to { Condition.AnyPackageContainsMultipleIntroOffers.serializer() },
        "selected" to { Condition.Selected.serializer() },
        "orientation" to { Condition.Orientation.serializer() },
        "screen_size" to { Condition.ScreenSize.serializer() },
        "selected_package" to { Condition.SelectedPackage.serializer() },
        "app_version" to { Condition.AppVersion.serializer() },
    ),
    defaultValue = { Condition.Unsupported },
)
