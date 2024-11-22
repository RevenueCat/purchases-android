package com.revenuecat.purchases.paywalls.components

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("purchase_button")
internal data class PurchaseButtonComponent(
    val stack: StackComponent,
) : PaywallComponent
