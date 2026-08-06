package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * Entry point for prerendering Paywalls V2 `web_view` bundles ahead of display.
 *
 * Not wired to core yet: preboot reaches this module through `PaywallAssetWarmer`, and prerender will
 * become a method on that same seam once its policy is settled. Until then this is driven only by the
 * paywall tester.
 */
@InternalRevenueCatAPI
public object PaywallWebViewPrewarming {

    /**
     * Prerenders the bundle at [url] for the `web_view` component identified by [componentId], so
     * that displaying it becomes a local activation. Best-effort: unsupported WebViews, unresolvable
     * URLs and prerender failures all leave the component to load normally.
     *
     * A prerender issued while WebView startup is still running will contend with it on the main
     * thread, so leave a gap after preboot.
     */
    public fun prewarm(
        context: Context,
        url: String,
        componentId: String,
        sizeToContentWidth: Boolean = false,
        sizeToContentHeight: Boolean = false,
    ) {
        onMainThread {
            PaywallWebViewPrewarmer.shared.prewarm(
                context = context,
                url = url,
                componentId = componentId,
                sizeToContentWidth = sizeToContentWidth,
                sizeToContentHeight = sizeToContentHeight,
            )
        }
    }

    // WebView is main-thread only, and callers are expected to be background asset-prewarming paths.
    private fun onMainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else Handler(Looper.getMainLooper()).post(block)
    }
}
