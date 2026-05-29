package com.revenuecat.purchases.admob.rewardverification

import android.os.Handler
import android.os.Looper
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesService
import com.revenuecat.purchases.admob.Logger
import com.revenuecat.purchases.admob.RewardVerificationResult
import com.revenuecat.purchases.admob.threading.runOnMainIfPresent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
internal class RewardVerificationRuntime(
    private val mainHandler: Handler = Handler(Looper.getMainLooper()),
    private val createVerificationScope: () -> CoroutineScope = {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    },
    private val poll: suspend (String) -> RewardVerificationResult = { clientTransactionId ->
        Poller.poll(clientTransactionId)
    },
) : PurchasesService {
    private var clientTransactionIdByAdResponseId: MutableMap<String, String>? = null

    @Volatile
    private var verificationScope: CoroutineScope? = null

    @Synchronized
    fun setClientTransactionId(adResponseId: String, clientTransactionId: String): Boolean {
        val store = clientTransactionIdByAdResponseId ?: return false
        store[adResponseId] = clientTransactionId
        return true
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

        val verificationTask = verificationScope?.launch {
            val result = poll(clientTransactionId)
            deliverOnce(result)
        }
        if (verificationTask == null) {
            removeClientTransactionId(adResponseId)
            deliverOnce(RewardVerificationResult.failed)
            return
        }

        notifyStarted(rewardVerificationStarted)

        verificationTask.invokeOnCompletion { cause ->
            removeClientTransactionId(adResponseId)
            if (cause != null) {
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
        return clientTransactionIdByAdResponseId?.get(adResponseId)
    }

    @Synchronized
    private fun removeClientTransactionId(adResponseId: String) {
        clientTransactionIdByAdResponseId?.remove(adResponseId)
    }

    @Synchronized
    override fun initialize(purchases: Purchases) {
        verificationScope?.cancel()
        verificationScope = createVerificationScope()
        clientTransactionIdByAdResponseId = mutableMapOf()
    }

    @Synchronized
    override fun close(purchases: Purchases) {
        clientTransactionIdByAdResponseId?.clear()
        clientTransactionIdByAdResponseId = null
        verificationScope?.cancel()
        verificationScope = null
    }
}
