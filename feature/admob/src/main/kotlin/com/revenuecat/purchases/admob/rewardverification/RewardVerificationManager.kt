package com.revenuecat.purchases.admob.rewardverification

import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.Logger
import com.revenuecat.purchases.admob.RewardVerificationResult
import java.util.UUID

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
internal object RewardVerificationManager {
    private val mainHandler = Handler(Looper.getMainLooper())

    private val runtime = RewardVerificationRuntime(
        mainHandler = mainHandler,
    )

    init {
        Purchases.registerService(runtime)
    }

    fun install(ad: RewardedAd) = installInternal(ad.responseInfo?.responseId)
    fun install(ad: RewardedInterstitialAd) = installInternal(ad.responseInfo?.responseId)

    fun handleRewardEarned(
        ad: RewardedAd,
        rewardVerificationStarted: (() -> Unit)?,
        rewardVerificationCompleted: (RewardVerificationResult) -> Unit,
    ) = handleRewardEarnedInternal(
        ad.responseInfo?.responseId,
        rewardVerificationStarted,
        rewardVerificationCompleted,
    )

    fun handleRewardEarned(
        ad: RewardedInterstitialAd,
        rewardVerificationStarted: (() -> Unit)?,
        rewardVerificationCompleted: (RewardVerificationResult) -> Unit,
    ) = handleRewardEarnedInternal(
        ad.responseInfo?.responseId,
        rewardVerificationStarted,
        rewardVerificationCompleted,
    )

    private fun installInternal(adResponseId: String?) {
        if (!Purchases.isConfigured) {
            Logger.e("Purchases is not configured. Call Purchases.configure() before enabling reward verification.")
            return
        }
        if (adResponseId == null) {
            Logger.e(
                "Reward verification requires a loaded ad with a responseId. " +
                    "Call enableRewardVerification() after the ad has loaded.",
            )
            return
        }

        val didStoreClientTransactionId = runtime.setClientTransactionId(
            adResponseId = adResponseId,
            clientTransactionId = UUID.randomUUID().toString(),
        )
        if (!didStoreClientTransactionId) {
            Logger.e(
                "Reward verification setup is not ready. " +
                    "Try enabling reward verification after Purchases is configured.",
            )
        }
    }

    private fun handleRewardEarnedInternal(
        adResponseId: String?,
        rewardVerificationStarted: (() -> Unit)?,
        rewardVerificationCompleted: (RewardVerificationResult) -> Unit,
    ) {
        runtime.handleRewardEarned(
            adResponseId = adResponseId,
            rewardVerificationStarted = rewardVerificationStarted,
            rewardVerificationCompleted = rewardVerificationCompleted,
        )
    }
}
