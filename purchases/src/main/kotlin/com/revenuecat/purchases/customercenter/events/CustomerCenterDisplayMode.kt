package com.revenuecat.purchases.customercenter.events

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import kotlinx.serialization.Serializable

/**
 * Display mode for the Customer Center. Meant for RevenueCatUI use.
 */
@Serializable
@ExperimentalPreviewRevenueCatPurchasesAPI
enum class CustomerCenterDisplayMode(val value: String) {

    FULL_SCREEN(value = "full_screen"),
}
