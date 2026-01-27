package com.revenuecat.purchases.common

import com.revenuecat.purchases.InternalRevenueCatAPI

internal object Constants {
    const val GOOGLE_PLAY_MANAGEMENT_URL = "https://play.google.com/store/account/subscriptions"
    const val GALAXY_STORE_MANAGEMENT_URL = "samsungapps://SubscriptionList/"
    const val SUBS_ID_BASE_PLAN_ID_SEPARATOR = ":"
}

@InternalRevenueCatAPI
object SharedConstants {
    const val RC_CUSTOMER_CENTER_TAG = "rc-customer-center"
    const val MICRO_MULTIPLIER = 1_000_000.0
    const val EXPECTED_SAMSUNG_IAP_SDK_VERSION = "6.5.0.001"
}
