@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.localrules

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.subscriberattributes.SubscriberAttribute
import java.util.Date

internal class SubscriberAttributesDimensionProvider(
    private val storedAttributes: () -> Map<String, SubscriberAttribute>,
) : RulesDimensionProvider {

    override val namespace: RulesDimensionNamespace = RulesDimensionNamespace.SubscriberAttributes

    /**
     * Read per evaluation rather than snapshotted: an app can set an attribute at any time, and an audience keyed
     * on one has to agree with what the app has said by the time the checkpoint is resolved.
     *
     * Attributes that cannot be read contribute no dimensions instead of failing the snapshot: they are parsed out
     * of whatever is on disk, so a payload an older version wrote differently surfaces here, and that should not
     * take an otherwise resolvable checkpoint down with it.
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

    private fun SubscriberAttribute.dimension(date: Date): Pair<String, RulesDimensionValue>? {
        // A deleted attribute is kept as a tombstone with no value until it has been posted, and an empty value is
        // the SDK's other spelling of a deletion, so both mean the customer no longer has the attribute.
        val value = value?.takeIf { it.isNotEmpty() } ?: return null
        // A name the engine could not resolve is dropped by RulesDimensionResolver, which applies the same rule to
        // every source.
        return key.backendKey to RulesDimensionValue.ObjectValue(
            mapOf(
                KEY_VALUE to RulesDimensionValue.StringValue(value),
                KEY_UPDATED_AT to RulesDimensionValue.DateValue(setTime),
                KEY_EVALUATED_AT to RulesDimensionValue.DateValue(date),
            ),
        )
    }

    internal companion object {
        const val KEY_EVALUATED_AT = "evaluatedAt"
        const val KEY_UPDATED_AT = "updatedAt"
        const val KEY_VALUE = "value"
    }
}
