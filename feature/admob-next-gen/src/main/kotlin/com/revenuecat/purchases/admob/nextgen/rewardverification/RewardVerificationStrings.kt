package com.revenuecat.purchases.admob.nextgen.rewardverification

internal object RewardVerificationStrings {

    // Verification lifecycle
    const val CANCELLED: String =
        "Reward verification was cancelled before completion."

    // Setup
    const val NOT_SET_UP_FOR_AD: String =
        "Reward verification is not set up for this ad. " +
            "Call enableRewardVerification() after loading the ad and before showing it."

    const val PURCHASES_NOT_CONFIGURED: String =
        "Purchases is not configured. Call Purchases.configure() before enabling reward verification."

    const val MISSING_AD_RESPONSE_ID: String =
        "Cannot enable reward verification because the loaded ad has no response ID."

    const val RUNTIME_NOT_READY: String =
        "Reward verification setup is not ready. " +
            "Try enabling reward verification after Purchases is configured."
}
