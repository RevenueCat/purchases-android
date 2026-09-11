package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.common.AdEventCallback
import com.revenuecat.purchases.admob.nextgen.AdMobNextGenStrings
import com.revenuecat.purchases.admob.nextgen.Logger

/**
 * Redirects the placement RevenueCat reports for this ad's display, click and revenue events.
 *
 * Passing `null` clears the placement recorded at load time rather than keeping it.
 */
internal fun AdEventCallback?.applyPlacementOverride(placement: String?) {
    val trackingCallback = this as? TrackingAdEventCallback<*>
    if (trackingCallback == null) {
        Logger.w(AdMobNextGenStrings.PLACEMENT_OVERRIDE_IGNORED)
        return
    }
    trackingCallback.placement = placement
}
