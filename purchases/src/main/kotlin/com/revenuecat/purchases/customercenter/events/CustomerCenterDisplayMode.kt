package com.revenuecat.purchases.customercenter.events

import com.revenuecat.purchases.InternalRevenueCatAPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Display mode for the Customer Center. Meant for RevenueCatUI use.
 */
@Serializable
@InternalRevenueCatAPI
public enum class CustomerCenterDisplayMode {

    @SerialName("full_screen")
    FULL_SCREEN,
}
