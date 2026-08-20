package com.revenuecat.purchases.ui.revenuecatui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * A value type for custom variables that can be passed to RevenueCat UI at runtime.
 *
 * Custom variables allow developers to personalize paywall content with dynamic values, and supply the values a
 * checkpoint's targeting rules are evaluated against. Variables are defined in the RevenueCat dashboard and can be
 * overridden at runtime.
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
     * Maps the underlying value, one lambda per variant.
     *
     * Abstract on purpose: a new variant has to implement it, so it can't silently be handled as one of the
     * existing ones. Callers get a total function without an unreachable `else` branch. Kotlin can't offer that
     * through a `when` here, since this class is not sealed — and it can't become sealed without breaking
     * consumers' exhaustive `when`s.
     */
    internal abstract fun <T> map(
        string: (kotlin.String) -> T,
        number: (kotlin.Double) -> T,
        boolean: (kotlin.Boolean) -> T,
    ): T

    /**
     * A string value.
     */
    @Parcelize
    public class String(public val value: kotlin.String) : CustomVariableValue(), Parcelable {
        override fun equals(other: Any?): kotlin.Boolean =
            other is String && value == other.value

        override fun hashCode(): Int = value.hashCode()

        override fun toString(): kotlin.String = "CustomVariableValue.String(value=$value)"

        override fun <T> map(
            string: (kotlin.String) -> T,
            number: (kotlin.Double) -> T,
            boolean: (kotlin.Boolean) -> T,
        ): T = string(value)
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

        override fun <T> map(
            string: (kotlin.String) -> T,
            number: (kotlin.Double) -> T,
            boolean: (kotlin.Boolean) -> T,
        ): T = number(value)
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

        override fun <T> map(
            string: (kotlin.String) -> T,
            number: (kotlin.Double) -> T,
            boolean: (kotlin.Boolean) -> T,
        ): T = boolean(value)
    }

    /**
     * The string representation of this value for use in paywall text replacement.
     */
    public val stringValue: kotlin.String
        get() = map(
            string = { it },
            // Format nicely: 100.0 -> "100", 99.99 -> "99.99"
            number = { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() },
            boolean = { it.toString() },
        )

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
