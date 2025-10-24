package com.revenuecat.purchases.paywalls.components

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Poko
@Serializable
@SerialName("sticky_footer")
@Immutable
class StickyFooterComponent(
    @get:JvmSynthetic
    val stack: StackComponent,
) : PaywallComponent
