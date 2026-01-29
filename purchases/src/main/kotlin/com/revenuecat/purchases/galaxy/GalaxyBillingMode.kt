package com.revenuecat.purchases.galaxy

/**
 * Represents the operation mode used by the Galaxy Store.
 *
 * This enum allows you to specify whether the app should operate in
 * real production mode, test mode, or a mode that forces failures for
 * debugging and QA purposes.
 *
 * Please ensure that you always provide the production value when submitting your app for beta testing
 * or normal app distribution.
 *
 * Refer to https://developer.samsung.com/iap/programming-guide/iap-helper-programming.html
 * for more information.
 */
enum class GalaxyBillingMode {
    /**
     * Process purchases with the production environment. Financial transactions occur for successful requests.
     * Use this mode when submitting your app for beta or production distribution.
     */
    PRODUCTION,

    /**
     * Payment requests are processed normally, except no financial transactions occur, and successful purchase
     * results are always returned.
     *
     * To test in-app purchases, testers must be added as License Testers in the seller’s Seller Portal
     * account. When operating as a tester, in-app items are available at no cost. Users not designated as a
     * tester will receive an error if they attempt to purchase an in-app product in this mode.
     *
     * Do not submit your app for beta or production distribution with this mode enabled.
     */
    TEST,

    /**
     * All IAP requests fail in this mode. Useful for testing error scenarios.
     */
    ALWAYS_FAIL,
}
