package com.revenuecat.purchases

import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.strings.PurchaseStrings
import com.revenuecat.purchases.strings.RestoreStrings
import com.revenuecat.purchases.utils.Result

internal class SyncPendingTransactionsHelper(
    private val appConfig: AppConfig,
    private val deviceCache: DeviceCache,
    private val billing: BillingAbstract,
    private val dispatcher: Dispatcher,
    private val identityManager: IdentityManager,
    private val postTransactionWithProductDetailsHelper: PostTransactionWithProductDetailsHelper,
) {

    fun syncPendingPurchaseQueue(
        allowSharingPlayStoreAccount: Boolean,
        onError: ((PurchasesError) -> Unit)? = null,
        onSuccess: ((CustomerInfo?) -> Unit)? = null,
    ) {
        if (!appConfig.dangerousSettings.autoSyncPurchases) {
            log(LogIntent.DEBUG, PurchaseStrings.SKIPPING_AUTOMATIC_SYNC)
            onSuccess?.invoke(null)
            return
        }
        if (billing.isConnected()) {
            log(LogIntent.DEBUG, PurchaseStrings.UPDATING_PENDING_PURCHASE_QUEUE)
            val appUserID = identityManager.currentAppUserID
            dispatcher.enqueue({
                billing.queryPurchases(
                    appUserID,
                    onSuccess = { purchasesByHashedToken ->
                        purchasesByHashedToken.forEach { (hash, purchase) ->
                            log(
                                LogIntent.DEBUG,
                                RestoreStrings.QUERYING_PURCHASE_WITH_HASH.format(purchase.type, hash),
                            )
                        }
                        deviceCache.cleanPreviouslySentTokens(purchasesByHashedToken.keys)
                        val transactionsToSync = deviceCache.getActivePurchasesNotInCache(purchasesByHashedToken)
                        postTransactionsWithCompletion(
                            transactionsToSync,
                            allowSharingPlayStoreAccount,
                            appUserID,
                            onError,
                            onSuccess,
                        )
                    },
                    onError = { error ->
                        log(LogIntent.GOOGLE_ERROR, error.toString())
                        onError?.invoke(error)
                    },
                )
            })
        } else {
            log(LogIntent.DEBUG, PurchaseStrings.BILLING_CLIENT_NOT_CONNECTED)
            onError?.invoke(PurchasesError(PurchasesErrorCode.StoreProblemError, "Billing client disconnected"))
        }
    }

    private fun postTransactionsWithCompletion(
        transactionsToSync: List<StoreTransaction>,
        allowSharingPlayStoreAccount: Boolean,
        appUserID: String,
        onError: ((PurchasesError) -> Unit)? = null,
        onSuccess: ((CustomerInfo?) -> Unit)? = null,
    ) {
        if (transactionsToSync.isEmpty()) {
            log(LogIntent.DEBUG, PurchaseStrings.NO_PENDING_PURCHASES_TO_SYNC)
            onSuccess?.invoke(null)
        } else {
            val results: MutableList<Result<CustomerInfo, PurchasesError>> = mutableListOf()
            postTransactionWithProductDetailsHelper.postTransactions(
                transactionsToSync,
                allowSharingPlayStoreAccount,
                appUserID,
                transactionPostSuccess = { _, customerInfo ->
                    results.add(Result.Success(customerInfo))
                    callCompletionFromResults(transactionsToSync, results, onError, onSuccess)
                },
                transactionPostError = { _, purchasesError ->
                    results.add(Result.Error(purchasesError))
                    callCompletionFromResults(transactionsToSync, results, onError, onSuccess)
                },
            )
        }
    }

    private fun callCompletionFromResults(
        transactionsToSync: List<StoreTransaction>,
        results: List<Result<CustomerInfo, PurchasesError>>,
        onError: ((PurchasesError) -> Unit)? = null,
        onSuccess: ((CustomerInfo?) -> Unit)? = null,
    ) {
        if (transactionsToSync.size == results.size) {
            results.forEachIndexed { index, result ->
                if (result is Result.Error) {
                    onError?.invoke(result.value)
                    return
                } else if (index == results.size - 1) {
                    onSuccess?.invoke((result as Result.Success).value)
                }
            }
        }
    }
}
