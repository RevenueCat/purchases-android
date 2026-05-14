package com.revenuecat.purchases.admob.rewardverification

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.RewardVerificationResult
import com.revenuecat.purchases.admob.threading.runOnMainIfPresent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.WeakHashMap

private const val TAG = "PurchasesAdMob"

internal object RewardVerificationManager {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val runtime = Runtime()
    private val serviceLocator = RewardVerificationServiceLocator()

    init {
        serviceLocator.registerHook(runtime)
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    fun install(onAd: Any) {
        if (!Purchases.isConfigured) {
            Log.w(
                TAG,
                "Purchases is not configured. Call Purchases.configure() before enabling reward verification.",
            )
            return
        }

        val didStoreClientTransactionId = runtime.setClientTransactionId(
            ad = onAd,
            clientTransactionId = UUID.randomUUID().toString(),
        )
        if (!didStoreClientTransactionId) {
            Log.w(
                TAG,
                "Reward verification setup is not ready. Try enabling reward verification after Purchases is configured.",
            )
        }
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    fun handleRewardEarned(
        onAd: Any,
        rewardVerificationStarted: (() -> Unit)?,
        rewardVerificationResult: (RewardVerificationResult) -> Unit,
    ) {
        runtime.handleRewardEarned(
            onAd = onAd,
            rewardVerificationStarted = rewardVerificationStarted,
            rewardVerificationResult = rewardVerificationResult,
        )
    }

    private fun deliverStarted(block: (() -> Unit)?) {
        runOnMainIfPresent(mainHandler, block)
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    private fun deliverFailed(rewardVerificationResult: (RewardVerificationResult) -> Unit) {
        deliverResult(RewardVerificationResult.failed, rewardVerificationResult)
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    private fun deliverResult(
        result: RewardVerificationResult,
        rewardVerificationResult: (RewardVerificationResult) -> Unit,
    ) {
        runOnMainIfPresent(mainHandler) {
            rewardVerificationResult(result)
        }
    }

    private fun warnAndAssertIfMissingClientTransactionId(clientTransactionId: String?) {
        if (clientTransactionId != null) return

        Log.w(
            TAG,
            "Reward verification callback requires enableRewardVerification() before show().",
        )
        assert(false) {
            "Call enableRewardVerification() before using reward verification show overloads."
        }
    }

    private class Runtime : RewardVerificationLifecycleHook {
        private var clientTransactionIdByAd: MutableMap<Any, String>? = null
        private var verificationJob: Job? = null
        private var verificationScope: CoroutineScope? = null

        @Synchronized
        fun setClientTransactionId(ad: Any, clientTransactionId: String): Boolean {
            val store = clientTransactionIdByAd ?: return false
            store[ad] = clientTransactionId
            return true
        }

        @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
        fun handleRewardEarned(
            onAd: Any,
            rewardVerificationStarted: (() -> Unit)?,
            rewardVerificationResult: (RewardVerificationResult) -> Unit,
        ) {
            val clientTransactionId = removeClientTransactionId(onAd)
            warnAndAssertIfMissingClientTransactionId(clientTransactionId)

            deliverStarted(rewardVerificationStarted)

            if (clientTransactionId == null) {
                deliverFailed(rewardVerificationResult)
                return
            }

            val activeScope = synchronized(this) { verificationScope }
            if (activeScope == null) {
                deliverFailed(rewardVerificationResult)
                return
            }

            verifyReward(
                scope = activeScope,
                clientTransactionId = clientTransactionId,
                rewardVerificationResult = rewardVerificationResult,
            )
        }

        @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
        private fun verifyReward(
            scope: CoroutineScope,
            clientTransactionId: String,
            rewardVerificationResult: (RewardVerificationResult) -> Unit,
        ) {
            scope.launch {
                val result = Poller.poll(clientTransactionId)
                deliverResult(result, rewardVerificationResult)
            }
        }

        @Synchronized
        private fun removeClientTransactionId(ad: Any): String? {
            return clientTransactionIdByAd?.remove(ad)
        }

        @Synchronized
        override fun onPurchasesConfigured(purchases: Purchases) {
            verificationJob?.cancel()
            verificationJob = SupervisorJob()
            verificationScope = CoroutineScope(verificationJob!! + Dispatchers.IO)
            clientTransactionIdByAd = WeakHashMap()
        }

        @Synchronized
        override fun onPurchasesClosed(purchases: Purchases) {
            clientTransactionIdByAd?.clear()
            clientTransactionIdByAd = null
            verificationJob?.cancel()
            verificationJob = null
            verificationScope?.cancel()
            verificationScope = null
        }
    }
}
