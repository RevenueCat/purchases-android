package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.common.AdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ads.events.AdTracker
import com.revenuecat.purchases.ads.events.types.AdDisplayedData
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdMediatorName
import com.revenuecat.purchases.ads.events.types.AdOpenedData
import com.revenuecat.purchases.ads.events.types.AdRevenueData

/** The single SDK callback that represents a displayed ad for a given format. */
internal enum class AdDisplayedTrigger {
    IMPRESSION,
    FULL_SCREEN_SHOW,
}

/**
 * An [AdEventCallback] wrapper that injects RevenueCat ad-event tracking before
 * delegating every callback to the user-provided [delegate].
 *
 * [responseInfoProvider] is read at event time rather than captured up front, so that
 * formats whose response info changes over the callback's lifetime (auto-refreshing
 * banners) report the currently displayed creative instead of the first-loaded one.
 */
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal abstract class TrackingAdEventCallback<CallbackT : AdEventCallback>(
    delegate: CallbackT?,
    private val adFormat: AdFormat,
    placement: String?,
    private val adUnitId: String,
    private val responseInfoProvider: () -> ResponseInfo,
    private val adDisplayedTrigger: AdDisplayedTrigger,
) : AdEventCallback {

    internal var delegate: CallbackT? = delegate

    internal var placement: String? = placement

    override fun onAdShowedFullScreenContent() {
        if (adDisplayedTrigger == AdDisplayedTrigger.FULL_SCREEN_SHOW) {
            trackAdDisplayed()
        }
        delegate?.onAdShowedFullScreenContent()
    }

    override fun onAdDismissedFullScreenContent() {
        delegate?.onAdDismissedFullScreenContent()
    }

    override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
        delegate?.onAdFailedToShowFullScreenContent(fullScreenContentError)
    }

    override fun onAdImpression() {
        if (adDisplayedTrigger == AdDisplayedTrigger.IMPRESSION) {
            trackAdDisplayed()
        }
        delegate?.onAdImpression()
    }

    override fun onAdClicked() {
        track { networkName, impressionId ->
            trackFromAdapter(
                AdOpenedData(
                    networkName = networkName,
                    mediatorName = AdMediatorName.AD_MOB,
                    adFormat = adFormat,
                    placement = placement,
                    adUnitId = adUnitId,
                    impressionId = impressionId,
                ),
            )
        }
        delegate?.onAdClicked()
    }

    override fun onAdPaid(value: AdValue) {
        track { networkName, impressionId ->
            trackFromAdapter(
                AdRevenueData(
                    networkName = networkName,
                    mediatorName = AdMediatorName.AD_MOB,
                    adFormat = adFormat,
                    placement = placement,
                    adUnitId = adUnitId,
                    impressionId = impressionId,
                    revenueMicros = value.valueMicros,
                    currency = value.currencyCode,
                    precision = value.precisionType.toAdRevenuePrecision(),
                ),
            )
        }
        delegate?.onAdPaid(value)
    }

    private fun trackAdDisplayed() {
        track { networkName, impressionId ->
            trackFromAdapter(
                AdDisplayedData(
                    networkName = networkName,
                    mediatorName = AdMediatorName.AD_MOB,
                    adFormat = adFormat,
                    placement = placement,
                    adUnitId = adUnitId,
                    impressionId = impressionId,
                ),
            )
        }
    }

    /**
     * Runs [trackEvent] against the ad tracker, skipping it when Purchases is not configured.
     *
     * Response info is resolved here rather than in each caller so that every tracked event reads
     * it at event time, which is what keeps refreshing banners attributed to the creative on screen.
     */
    private inline fun track(trackEvent: AdTracker.(networkName: String?, impressionId: String) -> Unit) {
        trackIfConfigured {
            val responseInfo = responseInfoProvider()
            adTracker.trackEvent(responseInfo.adapterClassName, responseInfo.responseId.orEmpty())
        }
    }
}
