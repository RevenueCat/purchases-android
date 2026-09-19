@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.utils

import android.content.Context
import android.net.Uri
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.PaywallAssetWarmer
import com.revenuecat.purchases.paywalls.PaywallAssetWarming
import io.mockk.mockk

internal class RecordingPaywallAssetWarmer : PaywallAssetWarmer {

    val warmedImages = mutableListOf<Uri>()
    val warmedWebViewUrls = mutableListOf<String>()
    var prebootCount = 0
    var clearedStorageCount = 0

    override fun warmImages(context: Context, imageUris: List<Uri>) {
        warmedImages.addAll(imageUris)
    }

    override fun prebootWebView(context: Context) {
        prebootCount++
    }

    override fun warmWebViewUrls(context: Context, urls: List<String>) {
        warmedWebViewUrls.addAll(urls)
    }

    override fun clearWebViewStorage(context: Context) {
        clearedStorageCount++
    }
}

internal fun paywallAssetWarming(
    warmer: PaywallAssetWarmer?,
    context: Context = mockk(relaxed = true),
) = PaywallAssetWarming(context, warmerProvider = { warmer })
