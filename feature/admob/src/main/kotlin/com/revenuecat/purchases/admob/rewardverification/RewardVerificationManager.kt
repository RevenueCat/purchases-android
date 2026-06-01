package com.revenuecat.purchases.admob.rewardverification

import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.Logger
import com.revenuecat.purchases.admob.RewardVerificationResult
import org.json.JSONObject
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

        val purchases = Purchases.sharedInstance
        val clientTransactionId = UUID.randomUUID().toString()
        val didStoreClientTransactionId = runtime.setClientTransactionId(
            adResponseId = adResponseId,
            clientTransactionId = clientTransactionId,
        )
        if (didStoreClientTransactionId) {
            // Correlate the ad with the backend verification through AdMob's server-side verification options. The
            // SSV callback forwards these to RevenueCat, which keys the verification by the client transaction id.
            attachOptions(
                serverSideVerificationOptions(
                    apiKey = purchases.currentConfiguration.apiKey,
                    appUserID = purchases.appUserID,
                    clientTransactionId = clientTransactionId,
                ),
            )
        } else {
            Logger.e(
                "Reward verification setup is not ready. " +
                    "Try enabling reward verification after Purchases is configured.",
            )
        }
    }

    private fun serverSideVerificationOptions(
        apiKey: String,
        appUserID: String,
        clientTransactionId: String,
    ): ServerSideVerificationOptions =
        ServerSideVerificationOptions.Builder()
            .setCustomData(customData(apiKey = apiKey, clientTransactionId = clientTransactionId))
            .setUserId(appUserID)
            .build()

    // JSON keys and ordering must stay in sync with the other RevenueCat SDKs that hit the same SSV endpoint.
    private fun customData(apiKey: String, clientTransactionId: String): String =
        JSONObject()
            .put("api_key", apiKey)
            .put("client_transaction_id", clientTransactionId)
            .toString()

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
