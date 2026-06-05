package com.revenuecat.purchases.ui.revenuecatui

import android.app.Activity
import android.content.Intent
import androidx.annotation.MainThread
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.awaitOfferings

/**
 * Attempts to present a paywall from a Preview Paywall deep link intent.
 *
 * This method parses the provided [Intent] and attempts to extract information that correlates
 * to a known offering and published paywall. If successful, this method returns `true` and
 * attempts to present that paywall for previewing.
 *
 * The [activity] parameter is optional. If omitted, the SDK uses the currently resumed Activity
 * it tracks automatically via [ActivityLifecycleCallbacks]. If no Activity can be found
 * (e.g. the app is backgrounded), a warning is logged and the method returns `false`.
 *
 * Expected deep link format:
 * `{yourScheme}://rc-paywall-preview?offering_id={ID}&paywall_id={ID}`
 *
 * Example — minimal (let the SDK discover the Activity):
 * ```kotlin
 * override fun onNewIntent(intent: Intent) {
 *     super.onNewIntent(intent)
 *     Purchases.sharedInstance.presentPaywall(intent)
 * }
 * ```
 *
 * Example — explicit Activity:
 * ```kotlin
 * Purchases.sharedInstance.presentPaywall(intent, activity = this)
 * ```
 *
 * @param intent The [Intent] received by your Activity.
 * @param activity The [Activity] to present the paywall from. Defaults to the currently resumed
 * Activity tracked by the SDK.
 * @return `true` if the intent was a valid Preview Paywall link and handling has begun;
 * `false` if the intent is not a valid paywall preview link.
 */
@MainThread
@OptIn(InternalRevenueCatAPI::class)
public fun Purchases.presentPaywall(intent: Intent, activity: Activity? = null): Boolean {
    return PaywallPreviewPresenter().handle(
        locateOffering = { offeringId -> awaitOfferings().getOffering(offeringId) },
        intent = intent,
        activity = activity ?: currentActivity,
    )
}
