@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.paywalls

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import java.util.ServiceLoader

/**
 * Core's handle on the [PaywallAssetWarmer] the RevenueCat UI module registers. When [isAvailable] is
 * false there is nothing to warm into, so callers skip the collection work that would only feed it.
 */
internal class PaywallAssetWarming(
    private val context: Context,
    warmerProvider: () -> PaywallAssetWarmer? = ::loadWarmer,
) {

    private val warmer: PaywallAssetWarmer? by lazy { warmerProvider() }

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    val isAvailable: Boolean
        get() = warmer != null

    fun warmImages(imageUris: Collection<Uri>) {
        if (imageUris.isEmpty()) return
        val warmer = warmer ?: return
        debugLog { "Pre-downloading ${imageUris.size} paywall image(s): $imageUris" }
        // An implementation comes from outside this module and runs before offerings are cached and
        // delivered, so it must not be able to fail that.
        runCatching { warmer.warmImages(context, imageUris.toList()) }.onFailure { error ->
            errorLog(error) { "Paywall image warming failed." }
        }
    }

    fun prebootWebView() {
        val warmer = warmer ?: return
        debugLog { "Prebooting the WebView engine for a paywall web_view component." }
        runCatching { warmer.prebootWebView(context) }.onFailure { error ->
            errorLog(error) { "Paywalls V2 WebView preboot failed." }
        }
    }

    fun warmWebViewUrls(urls: Collection<String>) {
        if (urls.isEmpty()) return
        val warmer = warmer ?: return
        debugLog { "Warming ${urls.size} Paywalls V2 web_view bundle(s): $urls" }
        // Moves WebView startup off the thread the first load would otherwise pay it on.
        prebootWebView()
        // Posted so warming never runs inline on the frame that delivered the offerings.
        mainHandler.post {
            runCatching { warmer.warmWebViewUrls(context, urls.toList()) }.onFailure { error ->
                errorLog(error) { "Paywalls V2 web_view warming failed." }
            }
        }
    }

    private companion object {
        fun loadWarmer(): PaywallAssetWarmer? {
            // A missing descriptor yields nothing rather than throwing; only one naming an unloadable class throws.
            val warmer = runCatching {
                ServiceLoader.load(
                    PaywallAssetWarmer::class.java,
                    PaywallAssetWarmer::class.java.classLoader,
                ).firstOrNull()
            }.getOrElse { error ->
                errorLog(error) { "Failed to load a PaywallAssetWarmer implementation." }
                null
            }
            if (warmer == null) {
                debugLog {
                    "No PaywallAssetWarmer found, so paywall asset warming is off " +
                        "(no RevenueCatUI dependency, or META-INF/services stripped by packaging)."
                }
            }
            return warmer
        }
    }
}
