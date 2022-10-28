package com.revenuecat.purchases.models

import android.os.Parcelable
import com.revenuecat.purchases.parceler.JSONObjectParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import org.json.JSONObject

@Parcelize
@TypeParceler<JSONObject, JSONObjectParceler>()
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
) : Parcelable