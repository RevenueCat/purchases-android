@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.localrules

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.common.warnLog
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import java.util.Date

/**
 * The dimensions the backend last delivered alongside the subscriber, cached because not every response carries
 * them, exposed as root-level names. Distinct from the reserved nested `backend` root, which holds the backend's
 * pre-evaluated predicate results for one evaluation.
 *
 * The names are the backend's to choose, and the root-name contract applies to it like any other source: one
 * that collides with an SDK-provided dimension fails the snapshot. An explicit null is kept as null, since the
 * backend stated it; a value no rule could read is dropped here, and a cache that cannot be read at all
 * contributes nothing.
 */
internal class SubscriberDimensionsProvider(
    private val cachedDimensionsJson: () -> String?,
) : RulesDimensionProvider {

    override val name: String = "subscriber_dimensions"

    @Suppress("ReturnCount")
    override suspend fun dimensions(date: Date): Map<String, RulesDimensionValue> {
        val dimensionsJson = try {
            cachedDimensionsJson()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            warnLog { "The subscriber dimensions are unavailable, so they can't be evaluated: $e" }
            return emptyMap()
        } ?: return emptyMap()
        val entries = try {
            JsonTools.json.parseToJsonElement(dimensionsJson) as? JsonObject
        } catch (e: SerializationException) {
            warnLog { "The cached subscriber dimensions can't be parsed, so they can't be evaluated: $e" }
            return emptyMap()
        }
        if (entries == null) {
            warnLog { "The cached subscriber dimensions are not a JSON object, so they can't be evaluated." }
            return emptyMap()
        }
        return entries.mapNotNull { (name, element) ->
            val value = element.asRulesDimensionValue()
            if (value == null) {
                warnLog { "Ignoring dimension '$name': its value can't be read by a rule." }
                null
            } else {
                name to value
            }
        }.toMap()
    }
}
