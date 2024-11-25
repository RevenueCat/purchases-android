package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Serializable
internal data class ComponentsConfig(
    val base: PaywallComponentsConfig,
)

@InternalRevenueCatAPI
@Serializable
internal data class PaywallComponentsConfig(
    val stack: StackComponent,
    val background: Background,
    @SerialName("sticky_footer")
    val stickyFooter: StickyFooterComponent? = null,
)
