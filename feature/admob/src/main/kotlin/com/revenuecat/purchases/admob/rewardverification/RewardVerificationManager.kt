package com.revenuecat.purchases.admob.rewardverification

import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.RewardVerificationToken
import com.revenuecat.purchases.admob.Logger
import com.revenuecat.purchases.admob.threading.runOnMainIfPresent
import com.revenuecat.purchases.ads.rewardverification.RewardVerificationResult

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
internal object RewardVerificationManager {
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * The [RewardVerificationService] for the active [Purchases] configuration, or `null` before
     * configuration / after close. The service owns the [RewardVerificationRuntime], so the verification
     * state is held on that instance and cleared when it is closed, rather than living on this object.
     */
    @Volatile
    internal var activeService: RewardVerificationService? = null

    private val runtime: RewardVerificationRuntime?
        get() = activeService?.runtime

    fun install(ad: RewardedAd) = installInternal(ad.responseInfo?.responseId, ad::setServerSideVerificationOptions)

    fun install(ad: RewardedInterstitialAd) =
        installInternal(ad.responseInfo?.responseId, ad::setServerSideVerificationOptions)

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

    private fun installInternal(adResponseId: String?, attachOptions: (ServerSideVerificationOptions) -> Unit) {
        val runtime = runtime
        when {
            !Purchases.isConfigured ->
                Logger.e("Purchases is not configured. Call Purchases.configure() before enabling reward verification.")
            adResponseId == null ->
                Logger.e(
                    "Reward verification requires a loaded ad with a responseId. " +
                        "Call enableRewardVerification() after the ad has loaded.",
                )
            runtime == null ->
                Logger.e(
                    "Reward verification setup is not ready. " +
                        "Try enabling reward verification after Purchases is configured.",
                )
            else -> {
                val token = Purchases.sharedInstance.generateRewardVerificationToken(impressionId = adResponseId)
                runtime.setClientTransactionId(
                    adResponseId = adResponseId,
                    clientTransactionId = token.clientTransactionId,
                )
                // Correlate the ad with the backend verification through AdMob's server-side verification options. The
                // SSV callback forwards these to RevenueCat, which keys the verification by the client transaction id.
                attachOptions(serverSideVerificationOptions(token))
            }
        }
    }

    private fun serverSideVerificationOptions(token: RewardVerificationToken): ServerSideVerificationOptions =
        ServerSideVerificationOptions.Builder()
            .setCustomData(token.customData)
            .setUserId(token.appUserID)
            .build()

    private fun handleRewardEarnedInternal(
        adResponseId: String?,
        rewardVerificationStarted: (() -> Unit)?,
        rewardVerificationCompleted: (RewardVerificationResult) -> Unit,
    ) {
        val runtime = runtime
        if (runtime == null) {
            // Not configured (or already closed): nothing to verify, so fail on the main thread.
            runOnMainIfPresent(mainHandler) { rewardVerificationCompleted(RewardVerificationResult.failed) }
            return
        }
        runtime.handleRewardEarned(
            adResponseId = adResponseId,
            rewardVerificationStarted = rewardVerificationStarted,
            rewardVerificationCompleted = rewardVerificationCompleted,
        )
    }
}
