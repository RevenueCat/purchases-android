package com.revenuecat.purchases.models

import android.os.Parcelable
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.parceler.JSONObjectParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import org.json.JSONObject


interface PurchaseOption : Parcelable {
    /**
     * Pricing phases defining a user's payment plan for the product over time.
     */
    val pricingPhases: List<PricingPhase>

    /**
     * Tags defined on the base plan or offer. Empty for Amazon.
     */
    val tags: List<String>

    /**
     * The StoreProduct this PurchaseOption is associated with.
     */
    val storeProduct: StoreProduct

    /**
     * True if this PurchaseOption represents a Google subscription base plan (rather than an offer).
     * Not applicable for Amazon or INAPP products.
     */
    val isBasePlan: Boolean
        get() = pricingPhases.size == 1
}

