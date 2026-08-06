package com.revenuecat.sample.admob.checkpoints

import android.app.Activity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.loadAndTrackInterstitialAd
import com.revenuecat.purchases.checkpoints.AdCheckpointPresenter
import com.revenuecat.purchases.checkpoints.AdCheckpointPresenterDelegate
import com.revenuecat.purchases.checkpoints.CheckpointAdOutcome

/**
 * POC [AdCheckpointPresenter], registered via [java.util.ServiceLoader]
 * (see META-INF/services). Loads and shows a real AdMob interstitial, tracked into RevenueCat via
 * the existing `loadAndTrackInterstitialAd` adapter.
 */
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
internal class AdmobCheckpointPresenterImpl : AdCheckpointPresenter {

    override fun present(
        activity: Activity,
        callId: String,
        adUnitId: String,
        delegate: AdCheckpointPresenterDelegate,
    ) {
        Purchases.sharedInstance.adTracker.loadAndTrackInterstitialAd(
            context = activity,
            adUnitId = adUnitId,
            adRequest = AdRequest.Builder().build(),
            placement = "checkpoint",
            loadCallback = object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            delegate.onCheckpointAdFinished(callId, CheckpointAdOutcome.Shown)
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            delegate.onCheckpointAdFinished(callId, CheckpointAdOutcome.Failed(adError.message))
                        }
                    }
                    ad.show(activity)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    delegate.onCheckpointAdFinished(callId, CheckpointAdOutcome.Failed(error.message))
                }
            },
        )
    }
}
