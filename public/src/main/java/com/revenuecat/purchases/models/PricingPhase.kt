package com.revenuecat.purchases.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Encapsulates how a user pays for a subscription at a given point in time.
 */
@Parcelize
data class PricingPhase(
    /**
     * Billing period for which the [PricingPhase] applies, in ISO 8601 format (i.e. one month -> P1M)
     */
    val billingPeriod: String,
    // TODO use Price class instead

    /**
     * The currency code in ISO 4217 format.
     */
    val priceCurrencyCode: String,

    /**
     * Formatted price for the [PricingPhase], including currency sign.
     */
    val formattedPrice: String,

    /**
     * Price for the payment cycle in micro-units, where 1M micro-units == one unit in the currency
     */
    val priceAmountMicros: Long,

    /**
     * [RecurrenceMode] of the [PricingPhase]
     */
    val recurrenceMode: RecurrenceMode,

    /**
     * Number of cycles for which the pricing phase applies.
     * Null for INFINITE_RECURRING or NON_RECURRING recurrence modes.
     */
    val billingCycleCount: Int?
) : Parcelable {

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "billingPeriod" to this.billingPeriod,
            "billingCycleCount" to this.billingCycleCount,
            "formattedPrice" to this.formattedPrice,
            "priceAmountMicros" to this.priceAmountMicros,
            "priceCurrencyCode" to this.priceCurrencyCode,
            "recurrenceMode" to this.recurrenceMode.identifier
        )
    }
}
