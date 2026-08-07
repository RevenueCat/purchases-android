package com.revenuecat.purchases.ui.revenuecatui.paywalls

import android.content.Context
import android.net.Uri
import coil.request.ImageRequest
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.PaywallAssetWarmer
import com.revenuecat.purchases.ui.revenuecatui.components.webview.PaywallWebViewStartUp

/**
 * Registered through `META-INF/services/com.revenuecat.purchases.paywalls.PaywallAssetWarmer`, which is
 * how core finds it without depending on this module. Needs the public no-argument constructor
 * `ServiceLoader` requires, kept by a rule in `consumer-rules.pro`.
 */
@OptIn(InternalRevenueCatAPI::class)
internal class PaywallAssetWarmerImpl : PaywallAssetWarmer {

    override fun warmImages(context: Context, imageUris: List<Uri>) {
        val imageLoader = PaywallImageLoader.get(context)
        imageUris.forEach { uri ->
            imageLoader.enqueue(ImageRequest.Builder(context).data(uri).build())
        }
    }

    override fun prebootWebView(context: Context) {
        PaywallWebViewStartUp.startUp(context)
    }
}
