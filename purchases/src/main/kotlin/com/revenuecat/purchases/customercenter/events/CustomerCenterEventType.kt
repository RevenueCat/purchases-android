package com.revenuecat.purchases.customercenter.events

/**
 * Types of events that can be tracked in the Customer Center. Meant for RevenueCatUI use.
 */
enum class CustomerCenterEventType(val value: String) {
    /**
     * The customer center was shown to the user.
     */
    IMPRESSION("customer_center_impression"),

    /**
     * The customer center was closed by the user.
     */
    SURVER_OPTION_CHOSEN("customer_center_survey_option_chosen")
}

/**
 * Display mode for the Customer Center. Meant for RevenueCatUI use.
 */
enum class CustomerCenterDisplayMode {
    FULL_SCREEN
}
