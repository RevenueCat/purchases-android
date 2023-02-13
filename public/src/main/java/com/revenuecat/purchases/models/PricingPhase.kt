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

    /**
     * [RecurrenceMode] of the [PricingPhase]
     */
    val recurrenceMode: RecurrenceMode,

    /**
     * Number of cycles for which the pricing phase applies.
     * Null for INFINITE_RECURRING or NON_RECURRING recurrence modes.
     */
    val billingCycleCount: Int?,

    /**
     * [Price] of the [PricingPhase]
     */
    val price: Price
) : Parcelable {

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "billingPeriod" to this.billingPeriod,
            "billingCycleCount" to this.billingCycleCount,
            "formattedPrice" to this.price.formattedPrice,
            "priceAmountMicros" to this.price.priceAmountMicros,
            "priceCurrencyCode" to this.price.currencyCode,
            "recurrenceMode" to this.recurrenceMode.identifier
        )
    }
}
