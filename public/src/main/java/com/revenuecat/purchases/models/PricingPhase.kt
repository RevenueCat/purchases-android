package com.revenuecat.purchases.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
// TODO javadocs
data class PricingPhase(
    val billingPeriod: String,
    val priceCurrencyCode: String,
    val formattedPrice: String,
    val priceAmountMicros: Long,
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
