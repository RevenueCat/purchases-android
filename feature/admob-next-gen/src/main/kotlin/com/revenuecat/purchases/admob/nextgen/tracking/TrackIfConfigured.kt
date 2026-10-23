package com.revenuecat.purchases.admob.nextgen.tracking

import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.nextgen.AdMobNextGenStrings
import com.revenuecat.purchases.admob.nextgen.Logger

/**
 * Executes [block] with the [Purchases] ad tracker if the SDK is configured.
 * If [Purchases] has not been configured yet, logs a warning and skips the block.
 *
 * This prevents crashes in ad callbacks when the developer has not yet called
 * [Purchases.configure].
 *
 * Every tracking path runs inside a Google Mobile Ads callback that the application is also waiting on, so a failure
 * while tracking is logged and swallowed rather than propagated: losing a RevenueCat event must never take down the
 * app's own ad handling.
 */
internal inline fun trackIfConfigured(block: Purchases.() -> Unit) {
    if (!Purchases.isConfigured) {
        Logger.w(AdMobNextGenStrings.PURCHASES_NOT_CONFIGURED)
        return
    }
    runCatching { Purchases.sharedInstance.block() }
        .onFailure { Logger.e(AdMobNextGenStrings.TRACKING_FAILED, it) }
}
