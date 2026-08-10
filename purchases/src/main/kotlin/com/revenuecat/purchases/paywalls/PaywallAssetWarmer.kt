package com.revenuecat.purchases.paywalls

import android.content.Context
import android.net.Uri
import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * Warms paywall assets ahead of display, using facilities only the RevenueCat UI module has.
 *
 * Implemented by that module and discovered through [java.util.ServiceLoader], so the core module can
 * warm without a compile-time dependency on it. To register an implementation, declare its fully
 * qualified name in `META-INF/services/com.revenuecat.purchases.paywalls.PaywallAssetWarmer` and give
 * it a public no-argument constructor.
 *
 * Warming is best-effort: implementations should return promptly and must not throw.
 */
@InternalRevenueCatAPI
public interface PaywallAssetWarmer {

    public fun warmImages(context: Context, imageUris: List<Uri>)

    /**
     * Starts the WebView engine ahead of the first `web_view` render, which otherwise pays for it on the
     * UI thread. Called only when a paywall contains one: the engine costs ~90 MB resident.
     */
    public fun prebootWebView(context: Context)
}
