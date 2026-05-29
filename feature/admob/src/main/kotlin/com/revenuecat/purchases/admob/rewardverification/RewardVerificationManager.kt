package com.revenuecat.purchases.admob.rewardverification

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.Constants
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

    fun install(onAd: Any) {
        if (!Purchases.isConfigured) {
            Log.e(
                Constants.TAG,
                "Purchases is not configured. Call Purchases.configure() before enabling reward verification.",
            )
            return
        }

        val didStoreClientTransactionId = runtime.setClientTransactionId(
            ad = onAd,
            clientTransactionId = UUID.randomUUID().toString(),
        )
        if (!didStoreClientTransactionId) {
            Log.e(
                Constants.TAG,
                "Reward verification setup is not ready. " +
                    "Try enabling reward verification after Purchases is configured.",
            )
        }
    }

    fun handleRewardEarned(
        onAd: Any,
        rewardVerificationStarted: (() -> Unit)?,
        rewardVerificationCompleted: (RewardVerificationResult) -> Unit,
    ) {
        runtime.handleRewardEarned(
            onAd = onAd,
            rewardVerificationStarted = rewardVerificationStarted,
            rewardVerificationCompleted = rewardVerificationCompleted,
        )
    }
}
