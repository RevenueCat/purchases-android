package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Serializable
internal data class ComponentsConfig(
    @get:JvmSynthetic val base: PaywallComponentsConfig,
)

@InternalRevenueCatAPI
@Serializable
internal data class PaywallComponentsConfig(
    @get:JvmSynthetic val stack: StackComponent,
    @get:JvmSynthetic val background: Background,
    @get:JvmSynthetic
    @SerialName("sticky_footer")
    val stickyFooter: StickyFooterComponent? = null,
)
