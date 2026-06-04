package com.revenuecat.purchases.ui.revenuecatui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivityArgs
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallContract
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

private const val PAYWALL_PREVIEW_HOST = "rc-paywall-preview"

/**
 * Attempts to present a paywall from a Preview Paywall deep link intent.
 *
 * This method parses the provided Intent and attempts to extract information that correlates
 * to a known offering and published paywall. If successful, this method returns `true` and
 * attempts to present that paywall for previewing.
 *
 * The [activity] parameter is optional. If omitted, the SDK uses the currently resumed Activity
 * it tracks automatically. If no Activity can be found (e.g. the app is backgrounded), a
 * warning is logged and the paywall is not shown.
 *
 * @param intent The [Intent] received by your Activity.
 * @param activity The [Activity] to present the paywall from. Defaults to the currently resumed
 * Activity tracked by the SDK.
 * @return `true` if the intent was a valid Preview Paywall link and handling has begun;
 * `false` if the intent is not a valid paywall preview link.
 */
@OptIn(InternalRevenueCatAPI::class)
public fun Purchases.presentPaywall(intent: Intent, activity: Activity? = null): Boolean {
    val uri = intent.data ?: return false
    if (uri.host != PAYWALL_PREVIEW_HOST) return false

    val queryParamCount = uri.queryParameterNames.size
    if (queryParamCount != 2) {
        Logger.w("Invalid rc-paywall-preview link. Expected 2 query parameters, but found $queryParamCount")
        return false
    }

    val offeringId = uri.getQueryParameter("offering_id")
    if (offeringId.isNullOrBlank()) {
        Logger.w("Invalid rc-paywall-preview link: Bad offering_id parameter")
        return false
    }

    val paywallId = uri.getQueryParameter("paywall_id")
    if (paywallId.isNullOrBlank()) {
        Logger.w("Invalid rc-paywall-preview link: Bad paywall_id parameter")
        return false
    }

    MainScope().launch {
        val offerings = try {
            awaitOfferings()
        } catch (e: PurchasesException) {
            Logger.w("Error fetching offerings for paywall preview: ${e.error}")
            return@launch
        }

        val offering = offerings.getOffering(offeringId)
        if (offering == null) {
            Logger.w(
                "Attempting to show paywall for offering ${offeringId}, " +
                    "but cannot locate a published offering with that id",
            )
            return@launch
        }

        // There is a one-to-one relationship between paywalls and offerings.
        // Validate that the paywall_id in the deep link matches the offering's actual paywall.
        val actualPaywallId = offering.paywall?.id
        if (actualPaywallId != paywallId) {
            Logger.w(
                "Attempting to show paywall ${paywallId}, " +
                    "but it does not match the paywall associated with ${offeringId}",
            )
            return@launch
        }

        // Use the explicitly provided Activity, falling back to the one the SDK tracks
        // automatically via ActivityLifecycleCallbacks — mirroring how iOS discovers the
        // key window from UIApplication.shared.connectedScenes.
        val presentationActivity = activity ?: currentActivity
        if (presentationActivity == null) {
            Logger.w("Unable to locate suitable presentation context for PaywallActivity")
            return@launch
        }

        val args = PaywallActivityArgs(
            offeringIdAndPresentedOfferingContext = OfferingSelection.IdAndPresentedOfferingContext(
                offeringId = offeringId,
                presentedOfferingContext = PresentedOfferingContext(offeringId),
            ),
            fontProvider = null,
            shouldDisplayDismissButton = true,
        )
        presentationActivity.startActivity(PaywallContract().createIntent(presentationActivity, args))
    }

    return true
}
