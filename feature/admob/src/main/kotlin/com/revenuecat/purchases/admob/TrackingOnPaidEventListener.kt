@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob

import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.ResponseInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdMediatorName
import com.revenuecat.purchases.ads.events.types.AdRevenueData

/**
 * An [OnPaidEventListener] wrapper that injects RevenueCat ad-revenue tracking
 * before delegating to the user-provided [delegate].
 *
 * This named wrapper allows [loadAndTrackBannerAdInternal] to detect and unwrap
 * an existing tracking listener when `loadAndTrackBannerAd` is called multiple
 * times on the same [com.google.android.gms.ads.AdView], preventing
 * double-tracking.
 */
internal class TrackingOnPaidEventListener(
    internal val delegate: OnPaidEventListener?,
    private val adFormat: AdFormat,
    private val placement: String?,
    private val adUnitId: String,
    private val responseInfoProvider: () -> ResponseInfo?,
) : OnPaidEventListener {

    override fun onPaidEvent(adValue: AdValue) {
        val responseInfo = responseInfoProvider()
        trackIfConfigured {
            adTracker.trackAdRevenue(
                AdRevenueData(
                    networkName = responseInfo?.mediationAdapterClassName,
                    mediatorName = AdMediatorName.AD_MOB,
                    adFormat = adFormat,
                    placement = placement,
                    adUnitId = adUnitId,
                    impressionId = responseInfo?.responseId.orEmpty(),
                    revenueMicros = adValue.valueMicros,
                    currency = adValue.currencyCode,
                    precision = adValue.precisionType.toAdRevenuePrecision(),
                ),
            )
        }
        delegate?.onPaidEvent(adValue)
    }
}
