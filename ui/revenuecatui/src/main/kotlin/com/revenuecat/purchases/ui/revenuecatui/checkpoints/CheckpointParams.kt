package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.CustomVariableKeyValidator
import com.revenuecat.purchases.common.localrules.RulesDimensionValue
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI

/**
 * Marks the receivers of the [CheckpointParams] DSL, so an inner block cannot implicitly call methods of an
 * outer one.
 */
@DslMarker
@InviteOnlyCheckpointsAPI
public annotation class CheckpointParamsDsl

/**
 * Per-call parameters for [com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpoint].
 *
 * [customVariables] are both the values a checkpoint's targeting rules are evaluated against, readable as
 * `custom.<key>`, and the custom variables the presented paywall renders.
 *
 * [paywallPresenter] presents the offering this call resolves to with app-owned UI, ahead of the presenter
 * registered through [com.revenuecat.purchases.ui.revenuecatui.checkpoints.paywallPresenter]. When neither is set,
 * the offering's configured paywall is presented, falling back to the default paywall.
 *
 * [errorPresenter] presents the errors of the flow the SDK presents for this call with app-owned UI, ahead of the
 * presenter registered through [com.revenuecat.purchases.ui.revenuecatui.checkpoints.errorPresenter]. When neither
 * is set, the SDK presents its own error dialog through a presenter of its own.
 *
 * [presentationMode] decides how the SDK presents the flow itself, full screen or as a sheet, or leaves the choice
 * to the SDK with [FlowPresentationMode.DEFAULT].
 *
 * Built through [Builder], or the DSL:
 * ```kotlin
 * val params = CheckpointParams {
 *     customVariables {
 *         "goal" to "lose_weight"
 *         "step" to 3
 *         "premium" to true
 *     }
 *     paywallPresenter { params, completion -> presentCustomPaywall(params, completion) }
 *     errorPresenter { params, completion -> presentCustomError(params, completion) }
 * }
 * ```
 */
@InviteOnlyCheckpointsAPI
public class CheckpointParams private constructor(
    customVariables: Map<String, CustomVariableValue>,
    public val paywallPresenter: PaywallPresenter?,
    public val errorPresenter: ErrorPresenter?,
    public val presentationMode: FlowPresentationMode,
    @InternalRevenueCatAPI
    public val errorDialogFactory: ErrorDialogFactory?,
) {

    /**
     * Keys must not be empty and contain only letters, numbers and underscores, since anything else cannot
     * be addressed as `custom.<key>`. Invalid entries are dropped here, once, with a warning: everything
     * downstream — targeting rules and the presented paywall alike — validates what it is given, and a map that is
     * already clean gives them nothing to report.
     */
    public val customVariables: Map<String, CustomVariableValue> =
        CustomVariableKeyValidator.validateAndFilter(customVariables)

    @OptIn(InternalRevenueCatAPI::class)
    override fun equals(other: Any?): Boolean =
        other is CheckpointParams &&
            other.customVariables == customVariables &&
            other.paywallPresenter == paywallPresenter &&
            other.errorPresenter == errorPresenter &&
            other.presentationMode == presentationMode &&
            other.errorDialogFactory == errorDialogFactory

    @OptIn(InternalRevenueCatAPI::class)
    override fun hashCode(): Int {
        var result = customVariables.hashCode()
        result = 31 * result + paywallPresenter.hashCode()
        result = 31 * result + errorPresenter.hashCode()
        result = 31 * result + presentationMode.hashCode()
        result = 31 * result + errorDialogFactory.hashCode()
        return result
    }

    @OptIn(InternalRevenueCatAPI::class)
    override fun toString(): String =
        "CheckpointParams(customVariables=$customVariables, paywallPresenter=$paywallPresenter, " +
            "errorPresenter=$errorPresenter, presentationMode=$presentationMode, " +
            "errorDialogFactory=$errorDialogFactory)"

    @CheckpointParamsDsl
    @Suppress("TooManyFunctions")
    public class Builder {

        private var customVariables: Map<String, CustomVariableValue> = emptyMap()
        private var paywallPresenter: PaywallPresenter? = null
        private var errorPresenter: ErrorPresenter? = null
        private var presentationMode: FlowPresentationMode = FlowPresentationMode.DEFAULT

        @InternalRevenueCatAPI
        private var errorDialogFactory: ErrorDialogFactory? = null

        /** Replaces any previously set custom variables. */
        public fun setCustomVariables(customVariables: Map<String, CustomVariableValue>): Builder = apply {
            this.customVariables = customVariables
        }

        /** Replaces any previously set custom variables. */
        @JvmSynthetic
        public fun customVariables(block: CustomVariablesBuilder.() -> Unit): Builder = apply {
            customVariables = CustomVariablesBuilder().apply(block).build()
        }

        /** Presents the offering this call resolves to; null leaves it to the registered presenter or the SDK. */
        public fun setPaywallPresenter(paywallPresenter: PaywallPresenter?): Builder = apply {
            this.paywallPresenter = paywallPresenter
        }

        /** Presents the offering this call resolves to, ahead of the registered presenter. */
        @JvmSynthetic
        public fun paywallPresenter(paywallPresenter: PaywallPresenter): Builder =
            setPaywallPresenter(paywallPresenter)

        /** Presents the errors of this call; null leaves it to the registered presenter or the SDK. */
        public fun setErrorPresenter(errorPresenter: ErrorPresenter?): Builder = apply {
            this.errorPresenter = errorPresenter
        }

        /** Presents the errors of this call, ahead of the registered presenter. */
        @JvmSynthetic
        public fun errorPresenter(errorPresenter: ErrorPresenter): Builder = setErrorPresenter(errorPresenter)

        /** How the SDK presents the flow this call resolves to. Defaults to [FlowPresentationMode.DEFAULT]. */
        public fun setPresentationMode(presentationMode: FlowPresentationMode): Builder = apply {
            this.presentationMode = presentationMode
        }

        /** How the SDK presents the flow this call resolves to. Defaults to [FlowPresentationMode.DEFAULT]. */
        @JvmSynthetic
        public fun presentationMode(presentationMode: FlowPresentationMode): Builder =
            setPresentationMode(presentationMode)

        /**
         * Replaces any previously set error dialog factory; null clears it. The factory supplies the content of the
         * error dialog for this call, and is used only when no [ErrorPresenter] is set for this call or registered
         * globally.
         */
        @InternalRevenueCatAPI
        public fun setErrorDialogFactory(factory: ErrorDialogFactory?): Builder = apply {
            this.errorDialogFactory = factory
        }

        /** Provides custom dialog content for errors of this call, ahead of the SDK's own localized copy. */
        @JvmSynthetic
        @InternalRevenueCatAPI
        public fun errorDialogFactory(factory: ErrorDialogFactory): Builder = setErrorDialogFactory(factory)

        @OptIn(InternalRevenueCatAPI::class)
        public fun build(): CheckpointParams =
            CheckpointParams(customVariables, paywallPresenter, errorPresenter, presentationMode, errorDialogFactory)
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
@InviteOnlyCheckpointsAPI
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
