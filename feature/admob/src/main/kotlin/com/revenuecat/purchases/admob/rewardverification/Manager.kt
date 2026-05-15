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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.WeakHashMap

private const val TAG = "PurchasesAdMob"

internal object RewardVerificationManager {
    private val mainHandler = Handler(Looper.getMainLooper())

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
    private val runtime = RewardVerificationRuntime(
        mainHandler = mainHandler,
    )
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
                "Reward verification setup is not ready. " +
                    "Try enabling reward verification after Purchases is configured.",
            )
        }
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
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

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
internal class RewardVerificationRuntime(
    private val mainHandler: Handler = Handler(Looper.getMainLooper()),
    private val createVerificationScope: () -> CoroutineScope = {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    },
    private val poll: suspend (String) -> RewardVerificationResult = { clientTransactionId ->
        Poller.poll(clientTransactionId)
    },
) : RewardVerificationLifecycleHook {
    private var clientTransactionIdByAd: MutableMap<Any, String>? = null
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

        notifyStarted(rewardVerificationStarted)

        if (clientTransactionId == null) {
            notifyCompleted(RewardVerificationResult.failed, rewardVerificationCompleted)
            return
        }

        val verificationTask = synchronized(this) {
            verificationScope?.launch {
                val result = poll(clientTransactionId)
                notifyCompleted(result, rewardVerificationCompleted)
            }
        }
        if (verificationTask == null) {
            notifyCompleted(RewardVerificationResult.failed, rewardVerificationCompleted)
            return
        }

        verificationTask.invokeOnCompletion { cause ->
            if (cause != null) {
                notifyCompleted(RewardVerificationResult.failed, rewardVerificationCompleted)
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
            TAG,
            "Reward verification callback requires enableRewardVerification() before show().",
        )
        assert(false) {
            "Call enableRewardVerification() before using reward verification show overloads."
        }
    }

    @Synchronized
    private fun removeClientTransactionId(ad: Any): String? {
        return clientTransactionIdByAd?.remove(ad)
    }

    @Synchronized
    override fun onPurchasesConfigured(purchases: Purchases) {
        verificationScope?.cancel()
        verificationScope = createVerificationScope()
        clientTransactionIdByAd = WeakHashMap()
    }

    @Synchronized
    override fun onPurchasesClosed(purchases: Purchases) {
        clientTransactionIdByAd?.clear()
        clientTransactionIdByAd = null
        verificationScope?.cancel()
        verificationScope = null
    }
}
