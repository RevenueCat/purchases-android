@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.utils

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.PaywallAssetWarming

/**
 * A no-op unless the offerings response carries the component tree, which it does only while remote config is
 * disabled; with it enabled the tree arrives on the workflows topic instead.
 */
internal class OfferingWebViewPrewarmer(
    private val assetWarming: PaywallAssetWarming,
) {

    fun prewarmWebViews(offerings: List<Offering>) {
        if (!assetWarming.isAvailable) return
        val urls = offerings.flatMapTo(linkedSetOf<String>()) { offering ->
            offering.baseComponentsConfig()?.collectAssets()?.webViewUrls.orEmpty()
        }
        assetWarming.warmWebViewUrls(urls)
    }
}
