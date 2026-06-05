package com.revenuecat.purchases.ui.revenuecatui

import android.app.Activity
import android.content.Intent
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivityArgs
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallContract
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

internal const val PAYWALL_PREVIEW_HOST = "rc-paywall-preview"

/**
 * Handles parsing and presenting a Preview Paywall deep link.
 *
 * Extracted from [presentPaywall] for testability — [locateOffering] and [launchPaywall]
 * are injected as lambdas so the async offering fetch and Activity launch can be exercised
 * in unit tests without a real [Purchases] instance or Android Activity.
 *
 * Mirrors iOS's `PreviewPaywallPresenter`.
 */
internal class PaywallPreviewPresenter(
    private val launchPaywall: (activity: Activity, offeringId: String) -> Unit = { activity, offeringId ->
        val args = PaywallActivityArgs(
            offeringIdAndPresentedOfferingContext = OfferingSelection.IdAndPresentedOfferingContext(
                offeringId = offeringId,
                presentedOfferingContext = PresentedOfferingContext(offeringId),
            ),
            fontProvider = null,
            shouldDisplayDismissButton = true,
        )
        activity.startActivity(PaywallContract().createIntent(activity, args))
    },
) {

    /**
     * Attempts to handle a Preview Paywall deep link.
     *
     * Parses [intent] synchronously and validates all required parameters. If [activity] is
     * null (no presentation context), returns `false` immediately — matching the iOS behaviour
     * where a missing view controller causes an early `return false` before any async work.
     *
     * If all synchronous checks pass, launches a coroutine to fetch the offering (via
     * [locateOffering]) and then calls [launchPaywall] if the offering and paywall IDs match.
     *
     * @return `true` if the intent was a valid preview paywall link and async handling has
     * been started; `false` if any synchronous check fails.
     */
    fun handle(
        locateOffering: suspend (offeringId: String) -> Offering?,
        intent: Intent,
        activity: Activity?,
    ): Boolean {
        val uri = intent.data ?: return false
        if (uri.host != PAYWALL_PREVIEW_HOST) return false

        val queryParamCount = uri.queryParameterNames.size
        if (queryParamCount != 2) {
            Logger.w("Invalid rc-paywall-preview link. Expected 2 parameters, but found $queryParamCount")
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

        // Check for a presentation context synchronously before launching the coroutine —
        // a missing context means we return false immediately, just as iOS returns false
        // when viewController is nil before the Task { } block.
        if (activity == null) {
            Logger.w("Unable to locate suitable presentation context for PaywallActivity")
            return false
        }

        MainScope().launch {
            val offering = try {
                locateOffering(offeringId)
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.w("Error fetching offerings for paywall preview: ${e.message}")
                return@launch
            }

            if (offering == null) {
                Logger.w(
                    "Attempting to show paywall for offering $offeringId, " +
                        "but cannot locate a published offering with that id",
                )
                return@launch
            }

            // There is a one-to-one relationship between paywalls and offerings.
            // Validate that the paywall_id in the deep link matches the offering's actual paywall.
            val actualPaywallId = offering.paywall?.id
            if (actualPaywallId != paywallId) {
                Logger.w(
                    "Attempting to show paywall $paywallId, " +
                        "but it does not match the paywall associated with $offeringId",
                )
                return@launch
            }

            launchPaywall(activity, offeringId)
        }

        return true
    }
}
