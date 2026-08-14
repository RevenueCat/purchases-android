@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.localrules

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.CustomVariableKeyValidator
import com.revenuecat.purchases.common.warnLog

/**
 * Converts caller-supplied custom variables into the `custom.*` dimensions a predicate can read.
 *
 * Unlike an SDK-owned [RulesDimensionProvider], the input here is developer-supplied and crosses an
 * `@InternalRevenueCatAPI` boundary that hybrid SDKs call directly, so it is re-validated rather than trusted, and
 * a bad entry only costs its own dimension: it is dropped with a warning instead of failing the evaluation.
 */
internal fun customVariableDimensions(customVariables: Map<String, Any>): Map<String, RulesDimensionValue> =
    CustomVariableKeyValidator.validateAndFilter(customVariables)
        .mapNotNull { (key, value) ->
            val dimensionValue = value.asRulesDimensionValueOrNull()
            if (dimensionValue == null) {
                warnLog {
                    "Dropping custom variable '$key': ${value.javaClass.name} cannot be used in a rule. " +
                        "Supported types are: String, Int, Long, Double, Float, Boolean."
                }
                null
            } else {
                key to dimensionValue
            }
        }
        .toMap()

private fun Any.asRulesDimensionValueOrNull(): RulesDimensionValue? = when (this) {
    is String -> RulesDimensionValue.StringValue(this)
    is Boolean -> RulesDimensionValue.BoolValue(this)
    is Int -> RulesDimensionValue.IntValue(toLong())
    is Long -> RulesDimensionValue.IntValue(this)
    is Float -> RulesDimensionValue.DoubleValue(toDouble())
    is Double -> RulesDimensionValue.DoubleValue(this)
    else -> null
}
