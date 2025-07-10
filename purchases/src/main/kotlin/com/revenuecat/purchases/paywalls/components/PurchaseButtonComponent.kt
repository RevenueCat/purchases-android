package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Poko
@Serializable
@SerialName("purchase_button")
public class PurchaseButtonComponent(
    @get:JvmSynthetic public val stack: StackComponent,
) : PaywallComponent
