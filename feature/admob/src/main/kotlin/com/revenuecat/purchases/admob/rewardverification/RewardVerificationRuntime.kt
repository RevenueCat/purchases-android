package com.revenuecat.purchases.admob.rewardverification

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesService
import com.revenuecat.purchases.admob.Constants
import com.revenuecat.purchases.admob.RewardVerificationResult
import com.revenuecat.purchases.admob.threading.runOnMainIfPresent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.WeakHashMap
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
    private var clientTransactionIdByAd: MutableMap<Any, String>? = null

    @Volatile
    private var verificationScope: CoroutineScope? = null

    @Synchronized
    fun setClientTransactionId(ad: Any, clientTransactionId: String): Boolean {
        val store = clientTransactionIdByAd ?: return false
        store[ad] = clientTransactionId
        return true
    }

    fun handleRewardEarned(
        onAd: Any,
        rewardVerificationStarted: (() -> Unit)?,
        rewardVerificationCompleted: (RewardVerificationResult) -> Unit,
    ) {
        val clientTransactionId = removeClientTransactionId(onAd)
        warnAndAssertIfMissingClientTransactionId(clientTransactionId)

        val completionDelivered = AtomicBoolean(false)
        fun deliverOnce(result: RewardVerificationResult) {
            if (completionDelivered.compareAndSet(false, true)) {
                notifyCompleted(result, rewardVerificationCompleted)
            }
        }

        if (clientTransactionId == null) {
            deliverOnce(RewardVerificationResult.failed)
            return
        }

        // verificationScope is @Volatile rather than locked: launch returns a Job immediately,
        // so the poll runs on Dispatchers.IO outside any lock. A concurrent close() either nulls
        // the scope (we deliver failed below) or cancels it after launch (invokeOnCompletion
        // delivers failed). Holding a lock across the launch would gain nothing.
        val verificationTask = verificationScope?.launch {
            val result = poll(clientTransactionId)
            deliverOnce(result)
        }
        if (verificationTask == null) {
            deliverOnce(RewardVerificationResult.failed)
            return
        }

        notifyStarted(rewardVerificationStarted)

        verificationTask.invokeOnCompletion { cause ->
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

        Log.w(
            Constants.TAG,
            "Reward verification callback requires enableRewardVerification() before show().",
        )
    }

    @Synchronized
    private fun removeClientTransactionId(ad: Any): String? {
        return clientTransactionIdByAd?.remove(ad)
    }

    @Synchronized
    override fun initialize(purchases: Purchases) {
        verificationScope?.cancel()
        verificationScope = createVerificationScope()
        clientTransactionIdByAd = WeakHashMap()
    }

    @Synchronized
    override fun close(purchases: Purchases) {
        clientTransactionIdByAd?.clear()
        clientTransactionIdByAd = null
        verificationScope?.cancel()
        verificationScope = null
    }
}
