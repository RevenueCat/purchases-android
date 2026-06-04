package com.revenuecat.purchases.admob.rewardverification

internal object RewardVerificationStrings {

    const val BACKEND_REJECTED_WITHOUT_MESSAGE: String =
        "Reward verification was rejected by AdMob server-side verification."

    const val EXHAUSTED_WHILE_PENDING: String =
        "Reward verification timed out: the AdMob server-side verification (SSV) callback was not " +
            "received in time. Possible causes: SSV is not enabled/configured for this ad unit in the " +
            "AdMob Dashboard, the SSV callback URL is misconfigured in the AdMob Dashboard, AdMob delayed " +
            "delivering the callback, or RevenueCat failed to process the SSV webhook."

    const val EXHAUSTED_WHILE_TRANSIENT_ERRORING: String =
        "Reward verification timed out after repeated transient errors while polling — typically " +
            "unstable device network connectivity. The reward couldn't be verified."

    const val UNEXPECTED_RESPONSE: String =
        "Reward verification stopped after the server returned a status this SDK version doesn't " +
            "recognize. Update to the latest SDK version; if you're already on the latest, contact " +
            "RevenueCat support."

    const val CANCELLED: String =
        "Reward verification was cancelled before completion."

    fun terminalError(error: String): String =
        "Reward verification stopped after an unrecoverable error: $error. This is unexpected; if it " +
            "persists, contact RevenueCat support with the error above."
}
