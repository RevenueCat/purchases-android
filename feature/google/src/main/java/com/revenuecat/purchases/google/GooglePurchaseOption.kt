package com.revenuecat.purchases.google

import android.os.Parcelable
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.PurchaseInfo
import com.revenuecat.purchases.models.PurchaseOption
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * Defines an option for purchasing a Google subscription
 */
@Parcelize
data class GooglePurchaseOption(
    override val id: String,
    override val pricingPhases: List<PricingPhase>,
    override val tags: List<String>,

    /**
     * Token used to purchase
     */
    val token: String,

    override val purchaseInfo: @RawValue PurchaseInfo
) : PurchaseOption, Parcelable
