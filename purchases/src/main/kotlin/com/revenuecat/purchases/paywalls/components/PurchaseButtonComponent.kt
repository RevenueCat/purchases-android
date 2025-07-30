package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Poko
@Serializable
@SerialName("purchase_button")
class PurchaseButtonComponent(
    @get:JvmSynthetic val stack: StackComponent,
) : PaywallComponent
