package com.revenuecat.purchases.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class GooglePurchaseOption(
    override val pricingPhases: List<PricingPhase>,
    override val storeProduct: StoreProduct,
    override val tags: List<String>,
    /**
     * Token used to purchase. Only used for Google BC5 subscriptions, null otherwise.
     */
    val token: String? = null,
) : PurchaseOption, Parcelable {
}
