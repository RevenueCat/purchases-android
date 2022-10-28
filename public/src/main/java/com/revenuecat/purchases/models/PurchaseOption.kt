package com.revenuecat.purchases.models

import android.os.Parcelable
import com.revenuecat.purchases.parceler.JSONObjectParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import org.json.JSONObject

@Parcelize
@TypeParceler<JSONObject, JSONObjectParceler>()
data class PurchaseOption(
    /**
     * Pricing phases defining a user's payment plan for the product over time.
     */
    val pricingPhases: List<PricingPhase>,

    /**
     * Tags defined on the base plan or offer in the Google dashboard.
     */
    val tags: List<String> = listOf(),

    /**
     * Token used to purchase. Only used for Google BC5 subscriptions, null otherwise.
     */
    val token: String? = null
) : Parcelable {

    /**
     * True if this PurchaseOption represents a Google subscription base plan (rather than an offer).
     * Not applicable for Amazon or one-time purchases.
     */
    val isBasePlan: Boolean
        get() = pricingPhases.size == 1

    // TODO add helpers to check if free trial, intro price, etc?

}