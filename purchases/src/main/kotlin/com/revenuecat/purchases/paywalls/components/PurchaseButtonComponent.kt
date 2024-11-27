package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.InternalRevenueCatAPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Serializable
@SerialName("purchase_button")
internal data class PurchaseButtonComponent(
    @get:JvmSynthetic val stack: StackComponent,
) : PaywallComponent
