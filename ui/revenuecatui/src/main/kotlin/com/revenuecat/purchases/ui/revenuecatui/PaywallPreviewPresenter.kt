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

// Holds the validated IDs extracted from a preview paywall deep link.
private data class PreviewLinkParams(
    val offeringId: String,
    val paywallId: String,
)

/**
 * Handles parsing and presenting a Preview Paywall deep link.
 *
 * Extracted from [presentPaywall] for testability — [locateOffering] and [launchPaywall] are
 * injected as lambdas so the async work can be exercised in unit tests without a real
 * [Purchases] instance or Android Activity.
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
     * Parses [intent] synchronously via [parseLink] and validates all URL parameters.
     * Returns `false` immediately for any URL problem.
     *
     * If the URL is valid, launches a coroutine that fetches the offering (via [locateOffering]),
     * validates the paywall ID, then presents the paywall from [activity].
     *
     * @return `true` if the intent was a valid preview paywall link and async handling has
     * been started; `false` if any synchronous URL check fails.
     */
    fun handle(
        locateOffering: suspend (offeringId: String) -> Offering?,
        intent: Intent,
        activity: Activity,
    ): Boolean {
        val params = parseLink(intent) ?: return false

        MainScope().launch {
            val offering = try {
                locateOffering(params.offeringId)
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.w("Error fetching offerings for paywall preview: ${e.message}")
                return@launch
            }

            if (offering == null) {
                Logger.w(
                    "Attempting to show paywall for offering ${params.offeringId}, " +
                        "but cannot locate a published offering with that id",
                )
                return@launch
            }

            // There is a one-to-one relationship between paywalls and offerings.
            // Validate that the paywall_id in the deep link matches the offering's actual paywall.
            val actualPaywallId = offering.paywall?.id
            if (actualPaywallId != params.paywallId) {
                Logger.w(
                    "Attempting to show paywall ${params.paywallId}, " +
                        "but it does not match the paywall associated with ${params.offeringId}",
                )
                return@launch
            }

            // The activity may have been destroyed, finished, or rotated away while the
            // offerings fetch was in flight. Guard against calling startActivity on a
            // stale reference, which would throw IllegalStateException or silently fail.
            if (activity.isFinishing || activity.isDestroyed) {
                Logger.w("Activity is no longer usable; skipping paywall presentation")
                return@launch
            }

            launchPaywall(activity, params.offeringId)
        }

        return true
    }

    /**
     * Parses and validates the URL parameters in a preview paywall deep link.
     * Returns [PreviewLinkParams] on success, or null (after logging a warning) on any failure.
     */
    private fun parseLink(intent: Intent): PreviewLinkParams? {
        val uri = intent.data?.takeIf { it.host == PAYWALL_PREVIEW_HOST } ?: return null

        val offeringId = uri.getQueryParameter("offering_id")?.takeIf { it.isNotBlank() }
        val paywallId = uri.getQueryParameter("paywall_id")?.takeIf { it.isNotBlank() }

        return when {
            offeringId == null -> {
                Logger.w("Invalid rc-paywall-preview link: Bad offering_id parameter")
                null
            }
            paywallId == null -> {
                Logger.w("Invalid rc-paywall-preview link: Bad paywall_id parameter")
                null
            }
            else -> PreviewLinkParams(offeringId, paywallId)
        }
    }
}
