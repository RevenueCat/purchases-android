@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob

import android.content.Context
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ads.events.types.AdDisplayedData
import com.revenuecat.purchases.ads.events.types.AdFailedToLoadData
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdLoadedData
import com.revenuecat.purchases.ads.events.types.AdMediatorName
import com.revenuecat.purchases.ads.events.types.AdOpenedData
import com.revenuecat.purchases.ads.events.types.AdRevenueData

@Suppress("LongParameterList")
internal fun loadAndTrackNativeAdInternal(
    context: Context,
    adUnitId: String,
    adRequest: AdRequest,
    placement: String? = null,
    nativeAdOptions: NativeAdOptions? = null,
    adListener: AdListener? = null,
    onPaidEventListener: OnPaidEventListener? = null,
    onAdLoaded: (NativeAd) -> Unit = {},
): AdLoader {
    var loadedNativeAd: NativeAd? = null

    val trackingAdListener = object : AdListener() {
        override fun onAdImpression() {
            val nativeAd = loadedNativeAd
            trackIfConfigured {
                adTracker.trackAdDisplayed(
                    AdDisplayedData(
                        networkName = nativeAd?.responseInfo?.mediationAdapterClassName,
                        mediatorName = AdMediatorName.AD_MOB,
                        adFormat = AdFormat.NATIVE,
                        placement = placement,
                        adUnitId = adUnitId,
                        impressionId = nativeAd?.responseInfo?.responseId.orEmpty(),
                    ),
                )
            }
            adListener?.onAdImpression()
        }

        override fun onAdClicked() {
            val nativeAd = loadedNativeAd
            trackIfConfigured {
                adTracker.trackAdOpened(
                    AdOpenedData(
                        networkName = nativeAd?.responseInfo?.mediationAdapterClassName,
                        mediatorName = AdMediatorName.AD_MOB,
                        adFormat = AdFormat.NATIVE,
                        placement = placement,
                        adUnitId = adUnitId,
                        impressionId = nativeAd?.responseInfo?.responseId.orEmpty(),
                    ),
                )
            }
            adListener?.onAdClicked()
        }

        override fun onAdFailedToLoad(error: LoadAdError) {
            trackIfConfigured {
                adTracker.trackAdFailedToLoad(
                    AdFailedToLoadData(
                        mediatorName = AdMediatorName.AD_MOB,
                        adFormat = AdFormat.NATIVE,
                        placement = placement,
                        adUnitId = adUnitId,
                        mediatorErrorCode = error.code,
                    ),
                )
            }
            adListener?.onAdFailedToLoad(error)
        }

        override fun onAdLoaded() {
            adListener?.onAdLoaded()
        }

        override fun onAdClosed() {
            adListener?.onAdClosed()
        }

        override fun onAdOpened() {
            adListener?.onAdOpened()
        }

        override fun onAdSwipeGestureClicked() {
            adListener?.onAdSwipeGestureClicked()
        }
    }

    val builder = AdLoader.Builder(context, adUnitId)
        .forNativeAd { nativeAd ->
            loadedNativeAd = nativeAd

            trackIfConfigured {
                adTracker.trackAdLoaded(
                    AdLoadedData(
                        networkName = nativeAd.responseInfo?.mediationAdapterClassName,
                        mediatorName = AdMediatorName.AD_MOB,
                        adFormat = AdFormat.NATIVE,
                        placement = placement,
                        adUnitId = adUnitId,
                        impressionId = nativeAd.responseInfo?.responseId.orEmpty(),
                    ),
                )
            }

            nativeAd.setOnPaidEventListener { adValue ->
                trackIfConfigured {
                    adTracker.trackAdRevenue(
                        AdRevenueData(
                            networkName = nativeAd.responseInfo?.mediationAdapterClassName,
                            mediatorName = AdMediatorName.AD_MOB,
                            adFormat = AdFormat.NATIVE,
                            placement = placement,
                            adUnitId = adUnitId,
                            impressionId = nativeAd.responseInfo?.responseId.orEmpty(),
                            revenueMicros = adValue.valueMicros,
                            currency = adValue.currencyCode,
                            precision = adValue.precisionType.toAdRevenuePrecision(),
                        ),
                    )
                }
                onPaidEventListener?.onPaidEvent(adValue)
            }

            onAdLoaded(nativeAd)
        }
        .withAdListener(trackingAdListener)

    if (nativeAdOptions != null) {
        builder.withNativeAdOptions(nativeAdOptions)
    }

    val adLoader = builder.build()
    adLoader.loadAd(adRequest)
    return adLoader
}
