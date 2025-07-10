package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Poko
@Serializable
public class ComponentsConfig(
    @get:JvmSynthetic public val base: PaywallComponentsConfig,
)

@InternalRevenueCatAPI
@Poko
@Serializable
public class PaywallComponentsConfig(
    @get:JvmSynthetic public val stack: StackComponent,
    @get:JvmSynthetic public val background: Background,
    @get:JvmSynthetic
    @SerialName("sticky_footer")
    public val stickyFooter: StickyFooterComponent? = null,
)
