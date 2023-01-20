package com.revenuecat.purchases.models

import android.os.Parcelable
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

    override val purchasingData: @RawValue PurchasingData
) : PurchaseOption, Parcelable
