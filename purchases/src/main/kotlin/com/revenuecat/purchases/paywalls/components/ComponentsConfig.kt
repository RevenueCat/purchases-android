package com.revenuecat.purchases.paywalls.components

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ComponentsConfig(
    val base: PaywallComponentsConfig,
)

@Serializable
internal data class PaywallComponentsConfig(
    val stack: StackComponent,
    val background: Background,
    @SerialName("sticky_footer")
    val stickyFooter: StickyFooterComponent? = null,
)
