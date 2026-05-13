package com.revenuecat.purchases.admob.rewardverification.internal

import android.app.Activity
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.revenuecat.purchases.admob.RewardVerificationResult

internal object RewardVerificationFacade {

    fun enable(ad: RewardedAd) {
        throw NotImplementedError("AdMob reward verification is not implemented yet.")
    }

    fun enable(ad: RewardedInterstitialAd) {
        throw NotImplementedError("AdMob reward verification is not implemented yet.")
    }

    @Suppress("UnusedParameter")
    fun show(
        ad: RewardedAd,
        activity: Activity,
        placement: String?,
        rewardVerificationStarted: (() -> Unit)?,
        rewardVerificationResult: (RewardVerificationResult) -> Unit,
    ) {
        throw NotImplementedError("AdMob reward verification is not implemented yet.")
    }

    @Suppress("UnusedParameter")
    fun show(
        ad: RewardedInterstitialAd,
        activity: Activity,
        placement: String?,
        rewardVerificationStarted: (() -> Unit)?,
        rewardVerificationResult: (RewardVerificationResult) -> Unit,
    ) {
        throw NotImplementedError("AdMob reward verification is not implemented yet.")
    }
}
