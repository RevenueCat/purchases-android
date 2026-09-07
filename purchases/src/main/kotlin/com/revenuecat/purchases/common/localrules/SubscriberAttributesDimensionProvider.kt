@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.localrules

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.subscriberattributes.SubscriberAttribute
import java.util.Date

internal class SubscriberAttributesDimensionProvider(
    private val storedAttributes: () -> Map<String, SubscriberAttribute>,
) : RulesDimensionProvider {

    override val name: String = KEY_SUBSCRIBER_ATTRIBUTES

    /**
     * Read per evaluation rather than snapshotted: an app can set an attribute at any time, and an audience keyed
     * on one has to agree with what the app has said by the time the checkpoint is resolved.
     *
     * Attributes that cannot be read contribute no dimensions instead of failing the snapshot: they are parsed out
     * of whatever is on disk, so a payload an older version wrote differently surfaces here, and that should not
     * take an otherwise resolvable checkpoint down with it.
     */
    @Suppress("ReturnCount")
    override suspend fun dimensions(date: Date): Map<String, RulesDimensionValue> {
        val attributes = try {
            storedAttributes()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            warnLog { "The subscriber attributes are unavailable, so they can't be evaluated: $e" }
            return emptyMap()
        }
        val records = attributes.values.mapNotNull { attribute -> attribute.record() }.toMap()
        // Absent rather than empty when there is nothing to say: an empty object is truthy in JSON Logic, so
        // `{"var": "subscriber_attributes"}` would read as present for a customer who has no attributes.
        if (records.isEmpty()) return emptyMap()
        return mapOf(KEY_SUBSCRIBER_ATTRIBUTES to RulesDimensionValue.ObjectValue(records))
    }

    @Suppress("ReturnCount")
    private fun SubscriberAttribute.record(): Pair<String, RulesDimensionValue>? {
        // A deleted attribute is kept as a tombstone with no value until it has been posted, and an empty value is
        // the SDK's other spelling of a deletion, so both mean the customer no longer has the attribute.
        val value = value?.takeIf { it.isNotEmpty() } ?: return null
        // RulesDimensionResolver applies the same reachability rule at every depth, but this provider must filter
        // before it counts: a customer whose only attributes are unreadable has to contribute no root at all,
        // and by the time the resolver drops the names, the root object would already exist (empty, and truthy).
        if (!key.backendKey.isReachableDimensionName) {
            warnLog {
                "Ignoring dimension '$KEY_SUBSCRIBER_ATTRIBUTES$DIMENSION_PATH_SEPARATOR${key.backendKey}': " +
                    "a dimension name can't be blank or contain '$DIMENSION_PATH_SEPARATOR'."
            }
            return null
        }
        return key.backendKey to RulesDimensionValue.ObjectValue(
            mapOf(
                KEY_VALUE to RulesDimensionValue.StringValue(value),
                KEY_UPDATED_AT to RulesDimensionValue.DateValue(setTime),
            ),
        )
    }

    internal companion object {
        const val KEY_SUBSCRIBER_ATTRIBUTES = "subscriber_attributes"
        const val KEY_UPDATED_AT = "updated_at"
        const val KEY_VALUE = "value"
    }
}
