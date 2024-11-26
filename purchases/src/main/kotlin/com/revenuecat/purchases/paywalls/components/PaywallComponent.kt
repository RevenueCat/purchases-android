package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.InternalRevenueCatAPI
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Serializable
internal sealed interface PaywallComponent
