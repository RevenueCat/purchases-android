package com.revenuecat.purchasetester

import com.revenuecat.purchases.Store

data class SdkConfiguration(
    val apiKey: String,
    val proxyUrl: String?,
    val store: Store,
) {
    val isAmazon: Boolean
        get() = store == Store.AMAZON
    val isPlayStore: Boolean
        get() = store == Store.PLAY_STORE
}
