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
    public @get:JvmSynthetic val base: PaywallComponentsConfig,
)

@InternalRevenueCatAPI
@Poko
@Serializable
public class PaywallComponentsConfig(
    public @get:JvmSynthetic val stack: StackComponent,
    public @get:JvmSynthetic val background: Background,
    @get:JvmSynthetic
    @SerialName("sticky_footer")
    public val stickyFooter: StickyFooterComponent? = null,
)
