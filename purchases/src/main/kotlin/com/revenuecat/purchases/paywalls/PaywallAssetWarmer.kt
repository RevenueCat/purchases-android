package com.revenuecat.purchases.paywalls

import android.content.Context
import android.net.Uri
import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * Warms paywall assets ahead of display, using facilities only the RevenueCat UI module has.
 *
 * Discovered through [java.util.ServiceLoader]: an implementation declares its fully qualified name in
 * `META-INF/services/com.revenuecat.purchases.paywalls.PaywallAssetWarmer` and needs a public no-argument
 * constructor. Warming is best-effort: implementations must return promptly and must not throw.
 */
@InternalRevenueCatAPI
public interface PaywallAssetWarmer {

    public fun warmImages(context: Context, imageUris: List<Uri>)

    /**
     * Starts the WebView engine ahead of the first `web_view` render, which otherwise pays for it on the
     * UI thread. Called only when a paywall contains one: the engine's memory is held for the process
     * lifetime once started.
     */
    public fun prebootWebView(context: Context)
}
