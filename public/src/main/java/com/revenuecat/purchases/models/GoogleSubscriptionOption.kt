package com.revenuecat.purchases.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Defines an option for purchasing a Google subscription
 */
@Parcelize
data class GoogleSubscriptionOption(
    override val id: String,
    override val pricingPhases: List<PricingPhase>,
    override val tags: List<String>,

    /**
     * Token used to purchase
     */
    val token: String
) : SubscriptionOption, Parcelable
