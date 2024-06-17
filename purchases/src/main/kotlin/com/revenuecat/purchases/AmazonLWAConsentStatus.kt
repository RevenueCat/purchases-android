package com.revenuecat.purchases

/**
 * Specifies the user consent status for Login with Amazon
 */
enum class AmazonLWAConsentStatus {
    /**
     * User has provided consent to access data.
     */
    CONSENTED,

    /**
     * Customer hasn't provided consent or the consent has expired.
     */
    UNAVAILABLE,
}
