package com.revenuecat.purchases.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class GooglePurchaseOption(
    override val pricingPhases: List<PricingPhase>,
    override val storeProduct: StoreProduct,
    override val tags: List<String>,

    /**
     * Token used to purchase
     */
    val token: String,
) : PurchaseOption, Parcelable