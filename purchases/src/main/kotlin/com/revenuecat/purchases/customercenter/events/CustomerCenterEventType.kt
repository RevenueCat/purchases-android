package com.revenuecat.purchases.customercenter.events

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Types of events that can be tracked in the Customer Center. Meant for RevenueCatUI use.
 */
@Serializable
@ExperimentalPreviewRevenueCatPurchasesAPI
enum class CustomerCenterEventType {
    /**
     * The customer center was shown to the user.
     */
    @SerialName("customer_center_impression")
    IMPRESSION,

    /**
     * The customer center was closed by the user.
     */
    @SerialName("customer_center_survey_option_chosen")
    SURVEY_OPTION_CHOSEN,
}
