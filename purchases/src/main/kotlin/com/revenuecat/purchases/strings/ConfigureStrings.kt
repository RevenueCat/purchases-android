package com.revenuecat.purchases.strings

internal object ConfigureStrings {
    const val APP_BACKGROUNDED = "App backgrounded"
    const val APP_FOREGROUNDED = "App foregrounded"
    const val CONFIGURING_PURCHASES_PROXY_URL_SET = "Purchases is being configured using a proxy for RevenueCat"
    const val DEBUG_ENABLED = "Debug logging enabled"
    const val INITIAL_APP_USER_ID = "Initial App User ID - %s"
    const val VERIFICATION_MODE_SELECTED = "Purchases configured with response verification: %s"
    const val LISTENER_SET = "Listener set"
    const val NO_SINGLETON_INSTANCE = "There is no singleton instance. " +
        "Make sure you configure Purchases before trying to get the default instance. " +
        "More info here: https://errors.rev.cat/configuring-sdk"
    const val SDK_VERSION = "SDK Version - %s"
    const val PACKAGE_NAME = "Package name - %s"
    const val LEGACY_API_KEY = "Looks like you're using a legacy API key.\n" +
        "This is still supported, but it's recommended to migrate to using platform-specific API key, " +
        "which should look like 'goog_1a2b3c4d5e6f7h' or 'amzn_1a2b3c4d5e6f7h'.\n" +
        "See https://rev.cat/auth for more details."
    const val AMAZON_API_KEY_GOOGLE_STORE = "Looks like you're using an Amazon API key but have configured the SDK " +
        "for the Google play store.\nEither use a Google API key which should look like 'goog_1a2b3c4d5e6f7h' or " +
        "configure the SDK to use Amazon.\nSee https://rev.cat/auth for more details."
    const val GOOGLE_API_KEY_AMAZON_STORE = "Looks like you're using a Google API key but have configured the SDK " +
        "for the Amazon app store.\nEither use an Amazon API key which should look like 'amzn_1a2b3c4d5e6f7h' or " +
        "configure the SDK to use Google.\nSee https://rev.cat/auth for more details."
    const val INVALID_API_KEY = "The specified API Key is not recognized.\n" +
        "Ensure that you are using the public app-specific API key, " +
        "which should look like 'goog_1a2b3c4d5e6f7h' or 'amzn_1a2b3c4d5e6f7h'.\n" +
        "See https://rev.cat/auth for more details."
    const val AUTO_SYNC_PURCHASES_DISABLED = "Automatic syncing of purchases has been disabled. \n" +
        "RevenueCat won’t observe the StoreKit queue, and it will not sync any purchase \n" +
        "automatically. Call syncPurchases whenever a new transaction is completed so the \n" +
        "receipt is sent to RevenueCat’s backend. Consumables disappear from the receipt \n" +
        "after the transaction is finished, so make sure purchases are synced before \n" +
        "finishing any consumable transaction, otherwise RevenueCat won’t register the \n" +
        "purchase."
    const val DO_NOT_CONSUME_IAP_ENABLED = "Dangerous setting: Consumption of IAP purchases has been disabled."
    const val INSTANCE_ALREADY_EXISTS = "Purchases instance already set. " +
        "Did you mean to configure two Purchases objects?"
}
