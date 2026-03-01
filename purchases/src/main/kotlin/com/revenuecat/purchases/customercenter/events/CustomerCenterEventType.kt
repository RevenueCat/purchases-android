package com.revenuecat.purchases.customercenter.events

import com.revenuecat.purchases.InternalRevenueCatAPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Types of events that can be tracked in the Customer Center. Meant for RevenueCatUI use.
 */
@Serializable
@InternalRevenueCatAPI
public enum class CustomerCenterEventType {
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

    /**
     * A promo offer was shown to the user.
     */
    @SerialName("customer_center_promo_offer_impression")
    PROMO_OFFER_IMPRESSION,

    /**
     * A promo offer was successfully completed.
     */
    @SerialName("customer_center_promo_offer_success")
    PROMO_OFFER_SUCCESS,

    /**
     * A promo offer was cancelled by the user.
     */
    @SerialName("customer_center_promo_offer_cancel")
    PROMO_OFFER_CANCEL,

    /**
     * A promo offer was rejected by the user before starting the purchase flow.
     */
    @SerialName("customer_center_promo_offer_rejected")
    PROMO_OFFER_REJECTED,

    /**
     * A promo offer encountered an error.
     */
    @SerialName("customer_center_promo_offer_error")
    PROMO_OFFER_ERROR,
}
