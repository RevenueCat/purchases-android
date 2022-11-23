package com.revenuecat.purchases.models

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * Defines an option for purchasing a Google subscription
 */
@Parcelize
data class GooglePurchaseOption(
    override val basePlanId: String,
    override val offerId: String?,
    override val pricingPhases: List<PricingPhase>,
    override val tags: List<String>,

    /**
     * Token used to purchase
     */
    val token: String
) : PurchaseOption, Parcelable