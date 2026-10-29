package com.revenuecat.purchases.ui.revenuecatui.paywalls

import android.content.Context
import android.net.Uri
import coil.request.ImageRequest
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.PaywallAssetWarmer
import com.revenuecat.purchases.ui.revenuecatui.components.webview.PaywallWebViewPrewarmer
import com.revenuecat.purchases.ui.revenuecatui.components.webview.PaywallWebViewStartUp
import com.revenuecat.purchases.ui.revenuecatui.components.webview.clearPaywallProfileStorage
import java.util.concurrent.ConcurrentHashMap

@OptIn(InternalRevenueCatAPI::class)
internal class PaywallAssetWarmerImpl : PaywallAssetWarmer {

    private val inFlightImages = ConcurrentHashMap<Uri, Any>()

    override fun warmImages(context: Context, imageUris: List<Uri>) {
        val imageLoader = PaywallImageLoader.get(context)
        imageUris.forEach { uri ->
            val owner = Any()
            if (inFlightImages.putIfAbsent(uri, owner) != null) return@forEach

            // Workflows can share images. Keep one warming request per URL until it completes,
            // then let the image loader's normal cache policies handle subsequent requests.
            fun completed() {
                inFlightImages.remove(uri, owner)
            }
            runCatching {
                imageLoader.enqueue(
                    ImageRequest.Builder(context)
                        .data(uri)
                        .listener(
                            onSuccess = { _, _ -> completed() },
                            onError = { _, _ -> completed() },
                            onCancel = { completed() },
                        )
                        .build(),
                )
            }.onFailure {
                completed()
            }.getOrThrow()
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
