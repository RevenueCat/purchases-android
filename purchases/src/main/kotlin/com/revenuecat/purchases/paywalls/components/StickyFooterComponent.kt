package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.InternalRevenueCatAPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Serializable
@SerialName("sticky_footer")
internal data class StickyFooterComponent(
    val stack: StackComponent,
) : PaywallComponent
