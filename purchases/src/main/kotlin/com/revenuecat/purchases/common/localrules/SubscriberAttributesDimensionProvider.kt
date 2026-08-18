@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.localrules

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.subscriberattributes.SubscriberAttribute
import java.util.Date

/**
 * The subscriber attributes this device has stored for the current customer, named the way the app set them:
 * `subscriberAttributes.$email`, `subscriberAttributes.goal`.
 *
 * Each one is a record rather than a bare value, so a predicate can read what it is and when it was set
 * independently. Every value is a string, because that is the only thing a subscriber attribute can be — the SDK
 * does not guess a type the app never declared, and the loose operators coerce anyway, so
 * `{"==": [{"var": "subscriberAttributes.seats.value"}, 3]}` matches an attribute set to `"3"`.
 *
 * Only what this device knows: an attribute set on another device reaches the backend, not this cache, so a
 * predicate about it is an ordinary non-match here. Whether an attribute has been posted yet is deliberately not
 * exposed — it describes how far along the attribute is on its way to the backend rather than anything about the
 * customer.
 */
internal class SubscriberAttributesDimensionProvider(
    private val storedAttributes: () -> Map<String, SubscriberAttribute>,
) : RulesDimensionProvider {

    override val identifier: String = "subscriberAttributes"

    override val namespace: RulesDimensionNamespace = RulesDimensionNamespace.SubscriberAttributes

    /**
     * Read per evaluation rather than snapshotted: an app can set an attribute at any time, and an audience keyed
     * on one has to agree with what the app has said by the time the checkpoint is resolved.
     *
     * Attributes that cannot be read contribute no dimensions instead of failing the snapshot, since the read
     * reaches out through the configured instance, which an app can tear down mid-evaluation. That should not take
     * an otherwise resolvable checkpoint down with it.
     */
    override suspend fun dimensions(date: Date): Map<String, RulesDimensionValue> {
        val attributes = try {
            storedAttributes()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            warnLog { "The subscriber attributes are unavailable, so they can't be evaluated: $e" }
            return emptyMap()
        }
        return attributes.values.mapNotNull { attribute -> attribute.dimension(date) }.toMap()
    }

    @Suppress("ReturnCount")
    private fun SubscriberAttribute.dimension(date: Date): Pair<String, RulesDimensionValue>? {
        val name = key.backendKey
        if (name.isEmpty() || name.contains(DIMENSION_PATH_SEPARATOR)) {
            warnLog {
                "Ignoring subscriber attribute '$name': a dimension name can't be empty or contain " +
                    "'$DIMENSION_PATH_SEPARATOR'."
            }
            return null
        }
        // A deleted attribute is kept as a tombstone with no value until it has been posted, and an empty value is
        // the SDK's other spelling of a deletion, so both mean the customer no longer has the attribute.
        val value = value?.takeIf { it.isNotEmpty() } ?: return null
        return name to RulesDimensionValue.ObjectValue(
            mapOf(
                KEY_VALUE to RulesDimensionValue.StringValue(value),
                // When *this device* set the value. Not refreshed by setting the same value again, since the SDK
                // has nothing new to post in that case.
                KEY_UPDATED_AT to RulesDimensionValue.DateValue(setTime),
                KEY_EVALUATED_AT to RulesDimensionValue.DateValue(date),
            ),
        )
    }

    internal companion object {
        const val KEY_EVALUATED_AT = "evaluatedAt"
        const val KEY_UPDATED_AT = "updatedAt"
        const val KEY_VALUE = "value"

        /**
         * `var` walks a strict dot-path, so a name containing this would be read as a path through a nested object
         * that does not exist. Such a name is left out rather than exposed as something no predicate can reach.
         */
        private const val DIMENSION_PATH_SEPARATOR = '.'
    }
}
