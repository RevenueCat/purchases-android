package com.revenuecat.purchases.admob.nextgen.rewardverification

import android.os.Handler
import android.os.Looper
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.ServerSideVerificationOptions
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.nextgen.Logger
import com.revenuecat.purchases.admob.nextgen.threading.runOnMainIfPresent
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingRewardedAdEventCallback
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingRewardedInterstitialAdEventCallback
import com.revenuecat.purchases.ads.rewardverification.RewardVerificationResult
import com.revenuecat.purchases.ads.rewardverification.RewardVerificationToken
import com.revenuecat.purchases.ads.rewardverification.RewardedAdTrackingMetadata

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

    fun install(ad: RewardedAd) = installInternal(ad.getResponseInfo().responseId, ad::setServerSideVerificationOptions)

    fun install(ad: RewardedInterstitialAd) =
        installInternal(ad.getResponseInfo().responseId, ad::setServerSideVerificationOptions)

    fun handleRewardEarned(
        ad: RewardedAd,
        rewardVerificationStarted: (() -> Unit)?,
        rewardVerificationCompleted: (RewardVerificationResult) -> Unit,
    ) = handleRewardEarnedInternal(
        ad.getResponseInfo().responseId,
        ad.rewardTrackingMetadata(),
        rewardVerificationStarted,
        rewardVerificationCompleted,
    )

    fun handleRewardEarned(
        ad: RewardedInterstitialAd,
        rewardVerificationStarted: (() -> Unit)?,
        rewardVerificationCompleted: (RewardVerificationResult) -> Unit,
    ) = handleRewardEarnedInternal(
        ad.getResponseInfo().responseId,
        ad.rewardTrackingMetadata(),
        rewardVerificationStarted,
        rewardVerificationCompleted,
    )

    // Null when the ad wasn't loaded through RevenueCat's tracking APIs, because no tracking callback is installed.
    private fun RewardedAd.rewardTrackingMetadata(): RewardedAdTrackingMetadata? =
        (adEventCallback as? TrackingRewardedAdEventCallback)?.rewardTrackingMetadata()

    private fun RewardedInterstitialAd.rewardTrackingMetadata(): RewardedAdTrackingMetadata? =
        (adEventCallback as? TrackingRewardedInterstitialAdEventCallback)?.rewardTrackingMetadata()

    private fun installInternal(adResponseId: String?, attachOptions: (ServerSideVerificationOptions) -> Unit) {
        val runtime = runtime
        when {
            !Purchases.isConfigured -> Logger.e(RewardVerificationStrings.PURCHASES_NOT_CONFIGURED)
            adResponseId == null -> Logger.e(RewardVerificationStrings.MISSING_AD_RESPONSE_ID)
            runtime == null -> Logger.e(RewardVerificationStrings.RUNTIME_NOT_READY)
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
        ServerSideVerificationOptions(
            userId = token.appUserID,
            customData = token.customData,
        )

    private fun handleRewardEarnedInternal(
        adResponseId: String?,
        trackingMetadata: RewardedAdTrackingMetadata?,
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
            trackingMetadata = trackingMetadata,
            rewardVerificationStarted = rewardVerificationStarted,
            rewardVerificationCompleted = rewardVerificationCompleted,
        )
    }
}
