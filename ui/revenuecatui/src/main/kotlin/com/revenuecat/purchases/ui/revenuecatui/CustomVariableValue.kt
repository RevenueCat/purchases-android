package com.revenuecat.purchases.ui.revenuecatui

import android.os.Parcelable
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
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
 *         "level" to CustomVariableValue.String("42"),
 *     ))
 *     .build()
 * ```
 *
 * In the paywall text (configured in the dashboard), use the `custom.` prefix:
 * ```
 * Hello {{ custom.player_name }}!
 * ```
 */
abstract class CustomVariableValue internal constructor() : Parcelable {

    /**
     * A string value.
     */
    @Parcelize
    class String(val value: kotlin.String) : CustomVariableValue(), Parcelable {
        override fun equals(other: Any?): kotlin.Boolean =
            other is String && value == other.value

        override fun hashCode(): Int = value.hashCode()

        override fun toString(): kotlin.String = "CustomVariableValue.String(value=$value)"
    }

    /**
     * A numeric value (integer or decimal).
     */
    @Parcelize
    internal class Number(val value: kotlin.Double) : CustomVariableValue(), Parcelable {
        constructor(value: kotlin.Int) : this(value.toDouble())
        constructor(value: kotlin.Long) : this(value.toDouble())
        constructor(value: kotlin.Float) : this(value.toDouble())

        override fun equals(other: Any?): kotlin.Boolean =
            other is Number && value == other.value

        override fun hashCode(): Int = value.hashCode()

        override fun toString(): kotlin.String = "CustomVariableValue.Number(value=$value)"
    }

    /**
     * A boolean value.
     */
    @Parcelize
    internal class Boolean(val value: kotlin.Boolean) : CustomVariableValue(), Parcelable {
        override fun equals(other: Any?): kotlin.Boolean =
            other is Boolean && value == other.value

        override fun hashCode(): Int = value.hashCode()

        override fun toString(): kotlin.String = "CustomVariableValue.Boolean(value=$value)"
    }

    /**
     * The string representation of this value for use in paywall text replacement.
     */
    val stringValue: kotlin.String
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

/**
 * Validates custom variable keys and logs warnings for invalid keys.
 *
 * Valid keys:
 * - Must not be empty
 * - Must start with a letter
 * - Can only contain letters, numbers, and underscores
 */
internal object CustomVariableKeyValidator {

    /**
     * Validates all keys in a custom variables map and logs warnings for invalid keys.
     */
    fun validate(variables: Map<String, CustomVariableValue>) {
        for (key in variables.keys) {
            if (!isValidKey(key)) {
                Logger.w(
                    "Custom variable key '$key' is invalid. " +
                        "Keys must start with a letter and contain only letters, numbers, and underscores.",
                )
            }
        }
    }

    private fun isValidKey(key: String): Boolean =
        key.isNotEmpty() &&
            key.first().isLetter() &&
            key.all { it.isLetter() || it.isDigit() || it == '_' }
}
