package com.revenuecat.purchases.admob.nextgen.rewardverification

internal object RewardVerificationStrings {

    // Verification lifecycle
    const val CANCELLED: String =
        "Reward verification was cancelled before completion."

    // Setup
    const val NOT_SET_UP_FOR_AD: String =
        "Reward verification is not set up for this ad. " +
            "Call enableRewardVerification() after loading the ad and before showing it."
}
