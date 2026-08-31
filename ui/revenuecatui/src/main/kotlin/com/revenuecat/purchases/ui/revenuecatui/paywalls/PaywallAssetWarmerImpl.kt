package com.revenuecat.purchases.ui.revenuecatui.paywalls

import android.content.Context
import android.net.Uri
import coil.request.ImageRequest
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.PaywallAssetWarmer
import com.revenuecat.purchases.ui.revenuecatui.components.webview.PaywallWebViewPrewarmer
import com.revenuecat.purchases.ui.revenuecatui.components.webview.PaywallWebViewStartUp
import com.revenuecat.purchases.ui.revenuecatui.components.webview.clearPaywallProfileStorage

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

    override fun warmWebViewUrls(context: Context, urls: List<String>) {
        urls.forEach { url -> PaywallWebViewPrewarmer.shared.prewarm(context, url) }
    }

    override fun clearWebViewStorage(context: Context) {
        clearPaywallProfileStorage()
    }
}
