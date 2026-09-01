package com.revenuecat.purchases.common

import com.revenuecat.purchases.Store

/**
 * The platform name the backend knows this SDK by, sent as the `X-Platform` header.
 *
 * Deliberately not exhaustive over [Store]: every store other than Amazon is served by the Android SDK build.
 */
internal val Store.platformName: String
    get() = when (this) {
        Store.AMAZON -> "amazon"
        else -> "android"
    }
