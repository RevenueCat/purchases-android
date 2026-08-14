package com.revenuecat.purchases.ui.revenuecatui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * A value type for custom paywall variables that can be passed to paywalls at runtime.
 *
 * Custom variables allow developers to personalize paywall text with dynamic values.
 * Variables are defined in the RevenueCat dashboard and can be overridden at runtime.
 *
 * ### Usage
 * ```kotlin
 * PaywallOptions.Builder { /* dismiss */ }
 *     .setCustomVariables(mapOf(
 *         "player_name" to CustomVariableValue.String("John"),
 *         "level" to CustomVariableValue.Number(42),
 *         "is_premium" to CustomVariableValue.Boolean(true),
 *     ))
 *     .build()
 * ```
 *
 * In the paywall text (configured in the dashboard), use the `custom.` prefix:
 * ```
 * Hello {{ custom.player_name }}!
 * ```
 */
public abstract class CustomVariableValue internal constructor() : Parcelable {

    /**
     * A string value.
     */
    @Parcelize
    public class String(public val value: kotlin.String) : CustomVariableValue(), Parcelable {
        override fun equals(other: Any?): kotlin.Boolean =
            other is String && value == other.value

        override fun hashCode(): Int = value.hashCode()

        override fun toString(): kotlin.String = "CustomVariableValue.String(value=$value)"
    }

    /**
     * A numeric value (integer or decimal).
     */
    @Parcelize
    public class Number(public val value: kotlin.Double) : CustomVariableValue(), Parcelable {
        public constructor(value: kotlin.Int) : this(value.toDouble())
        public constructor(value: kotlin.Long) : this(value.toDouble())
        public constructor(value: kotlin.Float) : this(value.toDouble())

        override fun equals(other: Any?): kotlin.Boolean =
            other is Number && value == other.value

        override fun hashCode(): Int = value.hashCode()

        override fun toString(): kotlin.String = "CustomVariableValue.Number(value=$value)"
    }

    /**
     * A boolean value.
     */
    @Parcelize
    public class Boolean(public val value: kotlin.Boolean) : CustomVariableValue(), Parcelable {
        override fun equals(other: Any?): kotlin.Boolean =
            other is Boolean && value == other.value

        override fun hashCode(): Int = value.hashCode()

        override fun toString(): kotlin.String = "CustomVariableValue.Boolean(value=$value)"
    }

    /**
     * The string representation of this value for use in paywall text replacement.
     */
    public val stringValue: kotlin.String
        get() = when (this) {
            is String -> value
            is Number -> {
                // Format nicely: 100.0 -> "100", 99.99 -> "99.99"
                if (value % 1.0 == 0.0) {
                    value.toLong().toString()
                } else {
                    value.toString()
                }
            }
            is Boolean -> value.toString()
            else -> error("Unknown CustomVariableValue type")
        }

    internal companion object {
        fun from(value: Any): CustomVariableValue = when (value) {
            is kotlin.String -> String(value)
            is kotlin.Int -> Number(value)
            is kotlin.Long -> Number(value)
            is kotlin.Double -> Number(value)
            is kotlin.Float -> Number(value)
            is kotlin.Boolean -> Boolean(value)
            else -> throw IllegalArgumentException(
                "Unsupported custom variable type: ${value::class.simpleName}. " +
                    "Supported types are: String, Int, Long, Double, Float, Boolean.",
            )
        }
    }
}
