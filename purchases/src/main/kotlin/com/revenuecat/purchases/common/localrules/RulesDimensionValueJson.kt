@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.localrules

import com.revenuecat.purchases.InternalRevenueCatAPI
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/**
 * `null` for a value no rule could read: an explicit JSON `null` (no value to compare) or an array of anything
 * but objects ([RulesDimensionValue.ObjectListValue] is the only collection a dimension can be). An entry an
 * object holds that can't be read is dropped from it, mirroring how absent record values behave.
 */
internal fun JsonElement.asRulesDimensionValue(): RulesDimensionValue? = when (this) {
    is JsonNull -> null
    is JsonPrimitive -> when {
        isString -> RulesDimensionValue.StringValue(content)
        else -> booleanOrNull?.let { RulesDimensionValue.BoolValue(it) }
            ?: longOrNull?.let { RulesDimensionValue.IntValue(it) }
            ?: doubleOrNull?.let { RulesDimensionValue.DoubleValue(it) }
    }
    is JsonObject -> RulesDimensionValue.ObjectValue(
        mapNotNull { (name, element) -> element.asRulesDimensionValue()?.let { name to it } }.toMap(),
    )
    is JsonArray -> map { element -> (element as? JsonObject)?.asRulesDimensionValue() }
        .takeIf { records -> records.all { it is RulesDimensionValue.ObjectValue } }
        ?.let { records ->
            RulesDimensionValue.ObjectListValue(records.map { (it as RulesDimensionValue.ObjectValue).value })
        }
}
