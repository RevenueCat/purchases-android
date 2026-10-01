@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.localrules

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.subscriberdimensions.SubscriberDimensions
import com.revenuecat.purchases.common.subscriberdimensions.SubscriberDimensionsResolution
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.common.warnLog
import java.util.Date

/**
 * The backend's dimensions for the subscriber, exposed as root-level names. Two sources carry them, each with
 * the server instant it was taken at: the `subscriber_dimensions` config topic and the last purchase response.
 * The fresher one is used whole (never merged); on a tie the purchase response wins. A purchase copy the config
 * has superseded is discarded.
 *
 * The names are the backend's to choose, and the root-name contract applies to it like any other source: one
 * that collides with an SDK-provided dimension fails the snapshot. An explicit null is kept as null, since the
 * backend stated it, and a purchase copy that cannot be read contributes nothing.
 */
internal class SubscriberDimensionsProvider(
    private val configDimensions: suspend () -> SubscriberDimensionsResolution,
    private val receiptDimensions: () -> SubscriberDimensions?,
    private val discardReceiptDimensions: (SubscriberDimensions) -> Unit,
) : RulesDimensionProvider {

    override val name: String = "subscriber_dimensions"

    override suspend fun dimensions(date: Date): Map<String, RulesDimensionValue> {
        val receipt = try {
            receiptDimensions()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            warnLog { "The purchase response's subscriber dimensions are unavailable, so they can't be evaluated: $e" }
            null
        }
        val config = (configDimensions() as? SubscriberDimensionsResolution.Found)?.dimensions
        val chosen = when {
            config == null -> receipt
            receipt == null -> config
            config.asOf.after(receipt.asOf) -> {
                discardReceiptDimensions(receipt)
                config
            }
            else -> receipt
        }
        verboseLog {
            when {
                chosen == null -> "No subscriber dimensions are available to evaluate rules against."
                chosen === receipt ->
                    "Evaluating rules against the purchase response's subscriber dimensions (as of ${chosen.asOf}; " +
                        "config copy as of ${config?.asOf})."
                else ->
                    "Evaluating rules against the config endpoint's subscriber dimensions (as of ${chosen.asOf}; " +
                        "purchase copy as of ${receipt?.asOf})."
            }
        }
        return chosen?.values.orEmpty()
    }
}
