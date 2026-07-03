package com.revenuecat.purchases.admob.rewardverification

import android.os.Handler
import android.os.Looper
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.Logger
import com.revenuecat.purchases.admob.threading.runOnMainIfPresent
import com.revenuecat.purchases.ads.rewardverification.RewardVerificationResult
import com.revenuecat.purchases.awaitPollRewardVerification
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Holds the per-configuration reward verification state. A fresh instance is created when [Purchases] is
 * configured and discarded with [close] when it closes, so no verification state outlives a configuration.
 */
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal class RewardVerificationRuntime(
    private val mainHandler: Handler = Handler(Looper.getMainLooper()),
    createVerificationScope: () -> CoroutineScope = {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    },
    private val poll: suspend (String) -> RewardVerificationResult = { clientTransactionId ->
        Purchases.sharedInstance.awaitPollRewardVerification(clientTransactionId)
    },
) {
    private val clientTransactionIdByAdResponseId = mutableMapOf<String, String>()
    private val verificationScope: CoroutineScope = createVerificationScope()

    @Synchronized
    fun setClientTransactionId(adResponseId: String, clientTransactionId: String) {
        clientTransactionIdByAdResponseId[adResponseId] = clientTransactionId
    }

    fun handleRewardEarned(
        adResponseId: String?,
        rewardVerificationStarted: (() -> Unit)?,
        rewardVerificationCompleted: (RewardVerificationResult) -> Unit,
    ) {
        val clientTransactionId = adResponseId?.let { getClientTransactionId(it) }
        warnAndAssertIfMissingClientTransactionId(clientTransactionId)

        val completionDelivered = AtomicBoolean(false)
        fun deliverOnce(result: RewardVerificationResult) {
            if (completionDelivered.compareAndSet(false, true)) {
                notifyCompleted(result, rewardVerificationCompleted)
            }
        }

        if (adResponseId == null || clientTransactionId == null) {
            deliverOnce(RewardVerificationResult.failed)
            return
        }

        // Notify started before launching so that on a non-main caller, the started post
        // is enqueued on the main handler before any completed post from the IO coroutine.
        notifyStarted(rewardVerificationStarted)

        val verificationTask = verificationScope.launch {
            val result = poll(clientTransactionId)
            deliverOnce(result)
        }

        verificationTask.invokeOnCompletion { cause ->
            removeClientTransactionId(adResponseId)
            if (cause != null) {
                // poll() swallows every non-cancellation failure into a failed result, so a non-null cause
                // here is the verification scope being cancelled (caller teardown / Purchases.close()).
                if (cause is CancellationException) {
                    Logger.w(RewardVerificationStrings.CANCELLED)
                }
                deliverOnce(RewardVerificationResult.failed)
            }
        }
    }

    private fun notifyStarted(block: (() -> Unit)?) {
        runOnMainIfPresent(mainHandler, block)
    }

    private fun notifyCompleted(
        result: RewardVerificationResult,
        rewardVerificationCompleted: (RewardVerificationResult) -> Unit,
    ) {
        runOnMainIfPresent(mainHandler) {
            rewardVerificationCompleted(result)
        }
    }

    private fun warnAndAssertIfMissingClientTransactionId(clientTransactionId: String?) {
        if (clientTransactionId != null) return

        Logger.w("Reward verification callback requires enableRewardVerification() before show().")
    }

    @Synchronized
    private fun getClientTransactionId(adResponseId: String): String? {
        return clientTransactionIdByAdResponseId[adResponseId]
    }

    @Synchronized
    private fun removeClientTransactionId(adResponseId: String) {
        clientTransactionIdByAdResponseId.remove(adResponseId)
    }

    @Synchronized
    fun close() {
        verificationScope.cancel()
        clientTransactionIdByAdResponseId.clear()
    }
}
