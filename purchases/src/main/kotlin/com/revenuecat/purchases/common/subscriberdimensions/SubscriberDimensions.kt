@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.subscriberdimensions

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.localrules.RulesDimensionValue
import com.revenuecat.purchases.common.localrules.asRulesDimensionValue
import com.revenuecat.purchases.common.warnLog
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.util.Date

/**
 * The backend's view of a subscriber's dimensions as served by the `subscriber_dimensions` topic: the same
 * root-level names the `dimensions` sibling of a `/subscribers` response carries, taken [asOf] a server instant so
 * the two copies can be ordered.
 */
internal data class SubscriberDimensions(
    val values: Map<String, RulesDimensionValue>,
    val asOf: Date,
) {
    companion object {
        /**
         * Reads the topic's `default` item metadata. `null` when the item is unusable: `dimensions` is not an
         * object, or `as_of` is not a non-negative epoch-millis number. Like the `/subscribers` copy, an explicit
         * JSON `null` is a stated value and is kept; an entry no rule could read is dropped.
         */
        fun parse(item: JsonObject): SubscriberDimensions? {
            val dimensions = item[KEY_DIMENSIONS] as? JsonObject
            val asOf = (item[KEY_AS_OF] as? JsonPrimitive)?.longOrNull?.takeIf { it >= 0 }
            if (dimensions == null || asOf == null) {
                errorLog {
                    "Ignoring the subscriber dimensions config: it needs a '$KEY_DIMENSIONS' object and an " +
                        "'$KEY_AS_OF' timestamp."
                }
                return null
            }
            val values = dimensions.mapNotNull { (name, element) ->
                val value = element.asRulesDimensionValue()
                if (value == null) {
                    warnLog { "Ignoring subscriber dimension '$name': its value can't be read by a rule." }
                    null
                } else {
                    name to value
                }
            }.toMap()
            return SubscriberDimensions(values, Date(asOf))
        }

        private const val KEY_DIMENSIONS = "dimensions"
        private const val KEY_AS_OF = "as_of"
    }
}
