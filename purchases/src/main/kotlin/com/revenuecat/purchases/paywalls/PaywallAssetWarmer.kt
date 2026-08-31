package com.revenuecat.purchases.paywalls

import android.content.Context
import android.net.Uri
import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * Manages paywall assets ahead of display, using facilities only the RevenueCat UI module has.
 *
 * Discovered through [java.util.ServiceLoader]: an implementation declares its fully qualified name in
 * `META-INF/services/com.revenuecat.purchases.paywalls.PaywallAssetWarmer` and needs a public no-argument
 * constructor. Every method is best-effort: implementations must return promptly and must not throw.
 */
@InternalRevenueCatAPI
public interface PaywallAssetWarmer {

    public fun warmImages(context: Context, imageUris: List<Uri>)

    /** Starts the WebView engine, so the first `web_view` render does not pay for it on the UI thread. */
    public fun prebootWebView(context: Context)

    /** Loads bundles offscreen into the http cache. Bounded concurrency, so it returns before they finish. */
    public fun warmWebViewUrls(context: Context, urls: List<String>)

    /** Drops the browsing data the outgoing user left behind, as far as the System WebView allows. */
    public fun clearWebViewStorage(context: Context)
}
