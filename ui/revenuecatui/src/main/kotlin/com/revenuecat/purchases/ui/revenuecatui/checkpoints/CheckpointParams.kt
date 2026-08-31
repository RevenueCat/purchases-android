package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.CustomVariableKeyValidator
import com.revenuecat.purchases.common.localrules.RulesDimensionValue
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue

/**
 * Marks the receivers of the [CheckpointParams] DSL, so an inner block cannot implicitly call methods of an
 * outer one.
 */
@DslMarker
@InternalRevenueCatAPI
public annotation class CheckpointParamsDsl

/**
 * Per-call parameters for [com.revenuecat.purchases.ui.revenuecatui.checkpoints.awaitCheckpoint].
 *
 * [customVariables] are both the values a checkpoint's targeting rules are evaluated against, readable as
 * `custom.<key>`, and the custom variables the presented paywall renders.
 *
 * Built through [Builder], or the DSL:
 * ```kotlin
 * val params = CheckpointParams {
 *     customVariables {
 *         "goal" to "lose_weight"
 *         "step" to 3
 *         "premium" to true
 *     }
 * }
 * ```
 */
@InternalRevenueCatAPI
public class CheckpointParams private constructor(
    customVariables: Map<String, CustomVariableValue>,
) {

    /**
     * Keys must start with a letter and contain only letters, numbers and underscores, since anything else cannot
     * be addressed as `custom.<key>`. Invalid entries are dropped here, once, with a warning: everything
     * downstream — targeting rules and the presented paywall alike — validates what it is given, and a map that is
     * already clean gives them nothing to report.
     */
    public val customVariables: Map<String, CustomVariableValue> =
        CustomVariableKeyValidator.validateAndFilter(customVariables)

    override fun equals(other: Any?): Boolean =
        other is CheckpointParams && other.customVariables == customVariables

    override fun hashCode(): Int = customVariables.hashCode()

    override fun toString(): String = "CheckpointParams(customVariables=$customVariables)"

    @CheckpointParamsDsl
    public class Builder {

        private var customVariables: Map<String, CustomVariableValue> = emptyMap()

        /** Replaces any previously set custom variables. */
        public fun setCustomVariables(customVariables: Map<String, CustomVariableValue>): Builder = apply {
            this.customVariables = customVariables
        }

        /** Replaces any previously set custom variables. */
        @JvmSynthetic
        public fun customVariables(block: CustomVariablesBuilder.() -> Unit): Builder = apply {
            customVariables = CustomVariablesBuilder().apply(block).build()
        }

        public fun build(): CheckpointParams = CheckpointParams(customVariables)
    }

    /**
     * Builds a custom variables map from typed entries: fluent [add] overloads, or the equivalent infix `to`
     * inside [Builder.customVariables] blocks. A repeated key keeps the last value.
     */
    @CheckpointParamsDsl
    @Suppress("TooManyFunctions")
    public class CustomVariablesBuilder {

        private val customVariables = mutableMapOf<String, CustomVariableValue>()

        public fun add(key: String, value: CustomVariableValue): CustomVariablesBuilder = apply {
            customVariables[key] = value
        }

        public fun add(key: String, value: String): CustomVariablesBuilder =
            add(key, CustomVariableValue.String(value))

        public fun add(key: String, value: Int): CustomVariablesBuilder =
            add(key, CustomVariableValue.Number(value))

        public fun add(key: String, value: Long): CustomVariablesBuilder =
            add(key, CustomVariableValue.Number(value))

        public fun add(key: String, value: Double): CustomVariablesBuilder =
            add(key, CustomVariableValue.Number(value))

        public fun add(key: String, value: Float): CustomVariablesBuilder =
            add(key, CustomVariableValue.Number(value))

        public fun add(key: String, value: Boolean): CustomVariablesBuilder =
            add(key, CustomVariableValue.Boolean(value))

        @JvmSynthetic
        public infix fun String.to(value: CustomVariableValue) {
            add(this, value)
        }

        @JvmSynthetic
        public infix fun String.to(value: String) {
            add(this, value)
        }

        @JvmSynthetic
        public infix fun String.to(value: Int) {
            add(this, value)
        }

        @JvmSynthetic
        public infix fun String.to(value: Long) {
            add(this, value)
        }

        @JvmSynthetic
        public infix fun String.to(value: Double) {
            add(this, value)
        }

        @JvmSynthetic
        public infix fun String.to(value: Float) {
            add(this, value)
        }

        @JvmSynthetic
        public infix fun String.to(value: Boolean) {
            add(this, value)
        }

        /**
         * Catches `to` with an unsupported value type at compile time. Without it, resolution would silently
         * fall back to [kotlin.to] and build a discarded [Pair].
         */
        @Deprecated(
            "Unsupported custom variable type. Supported types: String, Int, Long, Double, Float, Boolean and " +
                "CustomVariableValue. If you really need a Pair here, use Pair(first, second).",
            level = DeprecationLevel.ERROR,
        )
        @JvmSynthetic
        public infix fun String.to(value: Any?): Unit =
            throw UnsupportedOperationException("Unsupported custom variable type: $value")

        public fun build(): Map<String, CustomVariableValue> = customVariables.toMap()
    }
}

/**
 * DSL entry point: `CheckpointParams { customVariables { "goal" to "lose_weight" } }`.
 */
@JvmSynthetic
@InternalRevenueCatAPI
@Suppress("FunctionName")
public fun CheckpointParams(block: CheckpointParams.Builder.() -> Unit): CheckpointParams =
    CheckpointParams.Builder().apply(block).build()

/**
 * The rules-engine equivalent of a custom variable. Numbers stay doubles, since [CustomVariableValue.Number] holds
 * one and the engine compares `42.0` and `42` alike.
 */
@OptIn(InternalRevenueCatAPI::class)
internal val CustomVariableValue.asRulesDimensionValue: RulesDimensionValue
    get() = map(
        string = { RulesDimensionValue.StringValue(it) },
        number = { RulesDimensionValue.DoubleValue(it) },
        boolean = { RulesDimensionValue.BoolValue(it) },
    )
