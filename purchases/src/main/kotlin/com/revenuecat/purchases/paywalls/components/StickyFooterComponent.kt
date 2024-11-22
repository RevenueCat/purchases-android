package com.revenuecat.purchases.paywalls.components

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("sticky_footer")
internal data class StickyFooterComponent(
    val stack: StackComponent,
) : PaywallComponent
