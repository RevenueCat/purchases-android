package com.revenuecat.purchases.models

import android.os.Parcelable
import com.android.billingclient.api.ProductDetails.RecurrenceMode
import com.revenuecat.purchases.parceler.JSONObjectParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import org.json.JSONObject

@Parcelize
@TypeParceler<JSONObject, JSONObjectParceler>()
data class PricingPhase(
    val billingPeriod: String,
    val priceCurrencyCode: String,
    val formattedPrice: String,
    val priceAmountMicros: Long,
    @RecurrenceMode val recurrenceMode: Int, // todo should we make our own version of recurrencemode?
    val billingCycleCount: Int
) : Parcelable