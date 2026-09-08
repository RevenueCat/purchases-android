package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.PartialComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride.Condition
import com.revenuecat.purchases.utils.serializers.SealedDeserializerWithDefault
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
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

    /**
     * Numeric comparison operators for layout condition evaluation (window size).
     * [EQUALS] compares with a small epsilon tolerance but remains fragile against
     * measured fractional sizes; it is intended for authored integer breakpoints.
     */
    @Serializable
    public enum class ComparisonOperator {
        @SerialName(">=")
        GREATER_THAN_OR_EQUAL,

        @SerialName(">")
        GREATER_THAN,

        @SerialName("<=")
        LESS_THAN_OR_EQUAL,

        @SerialName("<")
        LESS_THAN,

        @SerialName("=")
        EQUALS,
    }

    @Serializable(with = ConditionSerializer::class)
    public sealed interface Condition {

        /**
         * Whether this condition is a rule introduced by conditional configurability
         * (e.g., variable_condition, selected_package_condition, intro_offer_condition, promo_offer_condition).
         * When an unsupported condition is encountered, all overrides containing rules are discarded,
         * rendering the "default paywall" with only base conditions applied.
         */
        @InternalRevenueCatAPI
        public val isRule: Boolean get() = false

        @Serializable
        public object Compact : Condition

        @Serializable
        public object Medium : Condition

        @Serializable
        public object Expanded : Condition

        @Serializable
        public object IntroOffer : Condition

        @Serializable
        public data class IntroOfferRule(
            public val operator: EqualityOperator,
            public val value: Boolean,
        ) : Condition { override val isRule: Boolean get() = true }

        @Serializable
        public object MultiplePhaseOffers : Condition

        @Serializable
        public object Selected : Condition

        @Serializable
        public object PromoOffer : Condition

        @Serializable
        public data class PromoOfferRule(
            public val operator: EqualityOperator,
            public val value: Boolean,
        ) : Condition { override val isRule: Boolean get() = true }

        @Serializable
        public data class SelectedPackage(
            public val operator: ArrayOperator,
            public val packages: List<String>,
        ) : Condition { override val isRule: Boolean get() = true }

        @Serializable
        public data class Variable(
            public val operator: EqualityOperator,
            public val variable: String,
            public val value: JsonPrimitive,
        ) : Condition { override val isRule: Boolean get() = true }

        @Serializable
        public data class State(
            public val operator: EqualityOperator,
            public val name: String,
            public val value: JsonPrimitive,
        ) : Condition {
            override val isRule: Boolean get() = true

            init {
                // Parity with iOS, where a null value fails ConditionValue decoding: fall back to Unsupported.
                require(value !is JsonNull) { "State condition value must not be null" }
            }
        }

        /**
         * Matches against the paywall's own rendered bounds, same as iOS: a paywall in a
         * sheet, dialog, or multi-window pane sees its own size, not the app window's.
         * [value] is density-independent (Android dp / iOS points). Evaluates to false
         * while the size is unknown and re-evaluates live as the paywall resizes.
         * Conditions within one override AND together, so `WindowWidthRule >= 700` plus
         * `WindowHeightRule >= 480` targets large windows while excluding landscape phones.
         */
        @Serializable
        public data class WindowWidthRule(
            public val operator: ComparisonOperator,
            public val value: Double,
        ) : Condition { override val isRule: Boolean get() = true }

        /** See [WindowWidthRule]; same semantics for the window's height. */
        @Serializable
        public data class WindowHeightRule(
            public val operator: ComparisonOperator,
            public val value: Double,
        ) : Condition { override val isRule: Boolean get() = true }

        /**
         * Matches when the window's aspect ratio (width / height: above 1 is landscape,
         * below 1 is portrait) satisfies the comparison; rotation re-evaluates it. Pair
         * with a width floor (e.g. [WindowWidthRule] >= 600) so small multi-window sizes
         * don't match; never matches while the size is unknown or its height is zero.
         */
        @Serializable
        public data class WindowAspectRatioRule(
            public val operator: ComparisonOperator,
            public val value: Double,
        ) : Condition { override val isRule: Boolean get() = true }

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
        "intro_offer_condition" to { Condition.IntroOfferRule.serializer() },
        "multiple_intro_offers" to { Condition.MultiplePhaseOffers.serializer() },
        "selected" to { Condition.Selected.serializer() },
        "promo_offer" to { Condition.PromoOffer.serializer() },
        "promo_offer_condition" to { Condition.PromoOfferRule.serializer() },
        "selected_package_condition" to { Condition.SelectedPackage.serializer() },
        "variable_condition" to { Condition.Variable.serializer() },
        "state_condition" to { Condition.State.serializer() },
        "window_width_condition" to { Condition.WindowWidthRule.serializer() },
        "window_height_condition" to { Condition.WindowHeightRule.serializer() },
        "window_aspect_ratio_condition" to { Condition.WindowAspectRatioRule.serializer() },
    ),
    defaultValue = { Condition.Unsupported },
)
