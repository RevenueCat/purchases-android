@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.paywalls

import android.content.Context
import android.net.Uri
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import java.util.ServiceLoader

/**
 * Core's handle on the [PaywallAssetWarmer] the RevenueCat UI module registers.
 *
 * [isAvailable] doubles as the "is the paywalls SDK here" answer: without it there is nothing to warm
 * into, so callers skip the work that would only feed it.
 */
internal class PaywallAssetWarming(
    private val context: Context,
    warmerProvider: () -> PaywallAssetWarmer? = ::loadWarmer,
) {

    // Lazy so the ServiceLoader scan happens on the first warm rather than during Purchases.configure().
    private val warmer: PaywallAssetWarmer? by lazy { warmerProvider() }

    val isAvailable: Boolean
        get() = warmer != null

    fun warmImages(imageUris: Collection<Uri>) {
        if (imageUris.isEmpty()) return
        val warmer = warmer ?: return
        debugLog { "Pre-downloading ${imageUris.size} paywall image(s): $imageUris" }
        // Warming runs inline on the offerings success path, before the offerings are cached and handed to
        // the app. A misbehaving implementation must not take that path down with it.
        runCatching { warmer.warmImages(context, imageUris.toList()) }.onFailure { error ->
            errorLog(error) { "Paywall image warming failed." }
        }
    }

    private companion object {
        fun loadWarmer(): PaywallAssetWarmer? {
            // A missing service descriptor is not an error: ServiceLoader yields nothing and this succeeds
            // with null. Only a descriptor naming a class that cannot be loaded throws.
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
