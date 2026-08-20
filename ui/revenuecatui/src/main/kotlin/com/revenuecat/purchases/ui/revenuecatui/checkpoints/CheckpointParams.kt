package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.CustomVariableKeyValidator
import com.revenuecat.purchases.common.localrules.RulesDimensionValue
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue

/**
 * Per-call parameters for [com.revenuecat.purchases.ui.revenuecatui.checkpoints.awaitCheckpoint].
 *
 * [customVariables] are both the values a checkpoint's targeting rules are evaluated against, readable as
 * `custom.<key>`, and the custom variables the presented paywall renders.
 */
@InternalRevenueCatAPI
public class CheckpointParams(
    customVariables: Map<String, CustomVariableValue> = emptyMap(),
) {

    public constructor(vararg customVariables: Pair<String, CustomVariableValue>) : this(customVariables.toMap())

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
}

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
