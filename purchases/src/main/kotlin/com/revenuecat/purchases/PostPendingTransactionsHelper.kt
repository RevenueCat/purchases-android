package com.revenuecat.purchases

import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.strings.PurchaseStrings
import com.revenuecat.purchases.strings.RestoreStrings
import com.revenuecat.purchases.utils.Result

internal sealed class SyncPendingPurchaseResult {
    data class Success(val customerInfo: CustomerInfo) : SyncPendingPurchaseResult()
    data class Error(val error: PurchasesError) : SyncPendingPurchaseResult()
    object AutoSyncDisabled : SyncPendingPurchaseResult()
    object NoPendingPurchasesToSync : SyncPendingPurchaseResult()
}

@OptIn(InternalRevenueCatAPI::class)
@Suppress("LongParameterList")
internal class PostPendingTransactionsHelper(
    private val appConfig: AppConfig,
    private val deviceCache: DeviceCache,
    private val billing: BillingAbstract,
    private val dispatcher: Dispatcher,
    private val identityManager: IdentityManager,
    private val postTransactionWithProductDetailsHelper: PostTransactionWithProductDetailsHelper,
    private val postReceiptHelper: PostReceiptHelper,
) {

    @Suppress("LongMethod")
    fun syncPendingPurchaseQueue(
        allowSharingPlayStoreAccount: Boolean,
        callback: ((SyncPendingPurchaseResult) -> Unit)? = null,
    ) {
        if (!appConfig.dangerousSettings.autoSyncPurchases) {
            log(LogIntent.DEBUG) { PurchaseStrings.SKIPPING_AUTOMATIC_SYNC }
            callback?.invoke(SyncPendingPurchaseResult.AutoSyncDisabled)
            return
        }
        log(LogIntent.DEBUG) { PurchaseStrings.UPDATING_PENDING_PURCHASE_QUEUE }
        val appUserID = identityManager.currentAppUserID
        dispatcher.enqueue({
            billing.queryPurchases(
                appUserID,
                onSuccess = { purchasesByHashedToken ->
                    purchasesByHashedToken.forEach { (hash, purchase) ->
                        log(LogIntent.DEBUG) {
                            RestoreStrings.QUERYING_PURCHASE_WITH_HASH.format(purchase.type, hash)
                        }
                    }
                    deviceCache.cleanPreviouslySentTokens(purchasesByHashedToken.keys)
                    val transactionsToSync = deviceCache.getActivePurchasesNotInCache(purchasesByHashedToken)
                    val pendingTransactionsTokens = purchasesByHashedToken.values
                        .filter { it.purchaseState == PurchaseState.PENDING }
                        .map { it.purchaseToken }
                        .toSet()
                    postTransactionsWithCompletion(
                        transactionsToSync,
                        allowSharingPlayStoreAccount,
                        appUserID,
                        onNoTransactionsToSync = {
                            postReceiptHelper.postRemainingCachedTransactionMetadata(
                                appUserID = appUserID,
                                allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
                                pendingTransactionsTokens = pendingTransactionsTokens,
                                onNoTransactionsToSync = {
                                    callback?.invoke(SyncPendingPurchaseResult.NoPendingPurchasesToSync)
                                },
                                onError = {
                                    callback?.invoke(SyncPendingPurchaseResult.Error(it))
                                },
                                onSuccess = {
                                    callback?.invoke(SyncPendingPurchaseResult.Success(it))
                                },
                            )
                        },
                        onError = { error ->
                            postReceiptHelper.postRemainingCachedTransactionMetadata(
                                appUserID = appUserID,
                                allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
                                pendingTransactionsTokens = pendingTransactionsTokens,
                                onNoTransactionsToSync = {
                                    log(LogIntent.DEBUG) { PurchaseStrings.NO_PENDING_PURCHASES_TO_SYNC }
                                    callback?.invoke(SyncPendingPurchaseResult.Error(error))
                                },
                                onError = {
                                    callback?.invoke(SyncPendingPurchaseResult.Error(error))
                                },
                                onSuccess = {
                                    callback?.invoke(SyncPendingPurchaseResult.Success(it))
                                },
                            )
                        },
                        onSuccess = { customerInfo ->
                            postReceiptHelper.postRemainingCachedTransactionMetadata(
                                appUserID = appUserID,
                                allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
                                pendingTransactionsTokens = pendingTransactionsTokens,
                                onNoTransactionsToSync = {
                                    log(LogIntent.DEBUG) { PurchaseStrings.NO_PENDING_PURCHASES_TO_SYNC }
                                    callback?.invoke(SyncPendingPurchaseResult.Success(customerInfo))
                                },
                                onError = {
                                    callback?.invoke(SyncPendingPurchaseResult.Error(it))
                                },
                                onSuccess = {
                                    callback?.invoke(SyncPendingPurchaseResult.Success(it))
                                },
                            )
                        },
                    )
                },
                onError = { error ->
                    log(LogIntent.GOOGLE_ERROR) { error.toString() }
                    callback?.invoke(SyncPendingPurchaseResult.Error(error))
                },
            )
        })
    }

    @SuppressWarnings("LongParameterList")
    private fun postTransactionsWithCompletion(
        transactionsToSync: List<StoreTransaction>,
        allowSharingPlayStoreAccount: Boolean,
        appUserID: String,
        onNoTransactionsToSync: (() -> Unit),
        onError: ((PurchasesError) -> Unit),
        onSuccess: ((CustomerInfo) -> Unit),
    ) {
        if (transactionsToSync.isEmpty()) {
            log(LogIntent.DEBUG) { PurchaseStrings.NO_PENDING_PURCHASES_TO_SYNC }
            onNoTransactionsToSync()
        } else {
            val results: MutableList<Result<CustomerInfo, PurchasesError>> = mutableListOf()
            postTransactionWithProductDetailsHelper.postTransactions(
                transactionsToSync,
                allowSharingPlayStoreAccount,
                appUserID,
                PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
                sdkOriginated = false,
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
        onSuccess: ((CustomerInfo) -> Unit)? = null,
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
