package com.revenuecat.purchases

import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.PostReceiptDataErrorCallback
import com.revenuecat.purchases.common.PostReceiptErrorHandlingBehavior
import com.revenuecat.purchases.common.ReceiptInfo
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.caching.LocalTransactionMetadata
import com.revenuecat.purchases.common.caching.LocalTransactionMetadataStore
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.networking.PostReceiptResponse
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.paywalls.PaywallPresentedCache
import com.revenuecat.purchases.paywalls.events.PaywallEvent
import com.revenuecat.purchases.paywalls.events.PaywallPostReceiptData
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
import com.revenuecat.purchases.subscriberattributes.getAttributeErrors
import com.revenuecat.purchases.subscriberattributes.toBackendMap
import com.revenuecat.purchases.utils.Result
import java.util.concurrent.ConcurrentLinkedQueue

@Suppress("LongParameterList")
internal class PostReceiptHelper(
    private val appConfig: AppConfig,
    private val backend: Backend,
    private val billing: BillingAbstract,
    private val customerInfoUpdateHandler: CustomerInfoUpdateHandler,
    private val deviceCache: DeviceCache,
    private val subscriberAttributesManager: SubscriberAttributesManager,
    private val offlineEntitlementsManager: OfflineEntitlementsManager,
    private val paywallPresentedCache: PaywallPresentedCache,
    private val localTransactionMetadataStore: LocalTransactionMetadataStore,
) {
    private val finishTransactions: Boolean
        get() = appConfig.finishTransactions
    private val purchasesAreCompletedBy: PurchasesAreCompletedBy
        get() = appConfig.purchasesAreCompletedBy

    /**
     * This method will post a token and receiptInfo to the backend without consuming any purchases.
     * It will store that the token was sent to the backend so it doesn't send it again.
     */
    fun postTokenWithoutConsuming(
        purchaseToken: String,
        receiptInfo: ReceiptInfo,
        isRestore: Boolean,
        appUserID: String,
        initiationSource: PostReceiptInitiationSource,
        onSuccess: (CustomerInfo) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        postReceiptAndSubscriberAttributes(
            appUserID,
            purchaseToken,
            isRestore,
            receiptInfo,
            initiationSource,
            purchaseState = PurchaseState.UNSPECIFIED_STATE,
            onSuccess = { postReceiptResponse ->
                deviceCache.addSuccessfullyPostedToken(purchaseToken)
                onSuccess(postReceiptResponse.customerInfo)
            },
            onError = { backendError, errorHandlingBehavior, _ ->
                if (errorHandlingBehavior == PostReceiptErrorHandlingBehavior.SHOULD_BE_MARKED_SYNCED) {
                    deviceCache.addSuccessfullyPostedToken(purchaseToken)
                }
                useOfflineEntitlementsCustomerInfoIfNeeded(
                    errorHandlingBehavior,
                    appUserID,
                    onSuccess = {
                        onSuccess(it)
                    },
                    onError = {
                        onError(backendError)
                    },
                )
            },
        )
    }

    /**
     * This method will post a StoreTransaction and optionally a StoreProduct info to the backend.
     * It will consume the purchase if finishTransactions is true.
     */
    fun postTransactionAndConsumeIfNeeded(
        purchase: StoreTransaction,
        storeProduct: StoreProduct?,
        subscriptionOptionForProductIDs: Map<String, SubscriptionOption>?,
        isRestore: Boolean,
        appUserID: String,
        initiationSource: PostReceiptInitiationSource,
        sdkOriginated: Boolean = false,
        onSuccess: (SuccessfulPurchaseCallback)? = null,
        onError: (ErrorPurchaseCallback)? = null,
    ) {
        val receiptInfo = ReceiptInfo.from(
            storeTransaction = purchase,
            storeProduct = storeProduct,
            subscriptionOptionsForProductIDs = subscriptionOptionForProductIDs,
            sdkOriginated = sdkOriginated,
        )
        postReceiptAndSubscriberAttributes(
            appUserID = appUserID,
            purchaseToken = purchase.purchaseToken,
            isRestore = isRestore,
            receiptInfo = receiptInfo,
            initiationSource = initiationSource,
            purchaseState = purchase.purchaseState,
            onSuccess = { postReceiptResponse ->
                // Currently we only support a single token per postReceipt call but multiple product Ids
                // (for multi-line subscriptions).
                val shouldConsume = postReceiptResponse.productInfoByProductId
                    ?.filterKeys { it in purchase.productIds }
                    ?.values
                    ?.firstOrNull()
                    ?.shouldConsume
                    ?: true
                billing.consumeAndSave(finishTransactions, purchase, shouldConsume, initiationSource)
                onSuccess?.let { it(purchase, postReceiptResponse.customerInfo) }
            },
            onError = { backendError, errorHandlingBehavior, _ ->
                if (errorHandlingBehavior == PostReceiptErrorHandlingBehavior.SHOULD_BE_MARKED_SYNCED) {
                    billing.consumeAndSave(finishTransactions, purchase, shouldConsume = false, initiationSource)
                }
                useOfflineEntitlementsCustomerInfoIfNeeded(
                    errorHandlingBehavior,
                    appUserID,
                    onSuccess = { customerInfo ->
                        onSuccess?.let { it(purchase, customerInfo) }
                    },
                    onError = {
                        onError?.let { it(purchase, backendError) }
                    },
                )
            },
        )
    }

    @OptIn(InternalRevenueCatAPI::class)
    fun postRemainingCachedTransactionMetadata(
        appUserID: String,
        allowSharingPlayStoreAccount: Boolean,
        pendingTransactionsTokens: Set<String>,
        onNoTransactionsToSync: () -> Unit,
        onError: ((PurchasesError) -> Unit),
        onSuccess: ((CustomerInfo) -> Unit),
    ) {
        val results: ConcurrentLinkedQueue<Result<CustomerInfo, PurchasesError>> = ConcurrentLinkedQueue()
        val transactionMetadataToSync = localTransactionMetadataStore.getAllLocalTransactionMetadata()
            .filterNot { pendingTransactionsTokens.contains(it.token) }
        if (transactionMetadataToSync.isEmpty()) {
            onNoTransactionsToSync()
            return
        }
        transactionMetadataToSync.forEach { transactionMetadata ->
            // Cached paywall data is retrieved from the cache when posting the receipt.
            performPostReceipt(
                appUserID = appUserID,
                purchaseToken = transactionMetadata.token,
                isRestore = allowSharingPlayStoreAccount,
                receiptInfo = transactionMetadata.receiptInfo,
                initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
                paywallData = transactionMetadata.paywallPostReceiptData,
                purchasesAreCompletedBy = transactionMetadata.purchasesAreCompletedBy,
                hasCachedTransactionMetadata = true,
                onSuccess = {
                    results.add(Result.Success(it.customerInfo))
                    callTransactionMetadataCompletionFromResults(
                        transactionMetadataToSync,
                        results,
                        onError,
                        onSuccess,
                    )
                },
                onError = { backendError, _, _ ->
                    results.add(Result.Error(backendError))
                    callTransactionMetadataCompletionFromResults(
                        transactionMetadataToSync,
                        results,
                        onError,
                        onSuccess,
                    )
                },
            )
        }
    }

    private fun callTransactionMetadataCompletionFromResults(
        transactionMetadataToSync: List<LocalTransactionMetadata>,
        results: ConcurrentLinkedQueue<Result<CustomerInfo, PurchasesError>>,
        onError: ((PurchasesError) -> Unit)? = null,
        onSuccess: ((CustomerInfo) -> Unit)? = null,
    ) {
        if (transactionMetadataToSync.size == results.size) {
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

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    @OptIn(InternalRevenueCatAPI::class)
    private fun postReceiptAndSubscriberAttributes(
        appUserID: String,
        purchaseToken: String,
        isRestore: Boolean,
        receiptInfo: ReceiptInfo,
        initiationSource: PostReceiptInitiationSource,
        purchaseState: PurchaseState,
        onSuccess: (PostReceiptResponse) -> Unit,
        onError: PostReceiptDataErrorCallback,
    ) {
        val (
            cachedTransactionMetadata,
            presentedPaywall,
            didCacheData,
        ) = localTransactionMetadataStore.getOrPutDataToPost(
            purchaseToken,
            receiptInfo,
            initiationSource,
        )

        val effectivePaywallData = cachedTransactionMetadata?.paywallPostReceiptData
            ?: presentedPaywall?.toPaywallPostReceiptData()
        val effectiveReceiptInfo = cachedTransactionMetadata?.receiptInfo
            ?: receiptInfo
        val effectivePurchasesAreCompletedBy = cachedTransactionMetadata?.purchasesAreCompletedBy
            ?: purchasesAreCompletedBy

        if (purchaseState == PurchaseState.PENDING) {
            onError(
                PurchasesError(PurchasesErrorCode.PaymentPendingError).also { errorLog(it) },
                PostReceiptErrorHandlingBehavior.SHOULD_NOT_CONSUME,
                null,
            )
            return
        }

        performPostReceipt(
            appUserID = appUserID,
            purchaseToken = purchaseToken,
            isRestore = isRestore,
            receiptInfo = effectiveReceiptInfo,
            initiationSource = initiationSource,
            paywallData = effectivePaywallData,
            purchasesAreCompletedBy = effectivePurchasesAreCompletedBy,
            hasCachedTransactionMetadata = cachedTransactionMetadata != null || didCacheData,
            onSuccess = onSuccess,
            onError = onError,
        )
    }

    @OptIn(InternalRevenueCatAPI::class)
    private data class CachedDataToPost(
        val localTransactionMetadata: LocalTransactionMetadata?,
        val paywallEvent: PaywallEvent?,
        val didCacheData: Boolean,
    )

    /**
     * Gets cached data for a purchase token, if any cached.
     * If not, it caches the information, as long as conditions are valid.
     */
    @OptIn(InternalRevenueCatAPI::class)
    @Synchronized
    private fun LocalTransactionMetadataStore.getOrPutDataToPost(
        purchaseToken: String,
        receiptInfo: ReceiptInfo,
        initiationSource: PostReceiptInitiationSource,
    ): CachedDataToPost {
        val cachedTransactionMetadata = getLocalTransactionMetadata(purchaseToken)
        val shouldCacheTransactionMetadata = cachedTransactionMetadata == null &&
            initiationSource == PostReceiptInitiationSource.PURCHASE

        val presentedPaywall = if (cachedTransactionMetadata == null) {
            paywallPresentedCache.getAndRemovePurchaseInitiatedEventIfNeeded(
                receiptInfo.productIDs,
                receiptInfo.purchaseTime,
            )
        } else {
            null
        }

        if (shouldCacheTransactionMetadata) {
            val dataToCache = LocalTransactionMetadata(
                token = purchaseToken,
                receiptInfo = receiptInfo,
                paywallPostReceiptData = presentedPaywall?.toPaywallPostReceiptData(),
                purchasesAreCompletedBy = purchasesAreCompletedBy,
            )
            cacheLocalTransactionMetadata(purchaseToken, dataToCache)
        }

        return CachedDataToPost(
            localTransactionMetadata = cachedTransactionMetadata,
            paywallEvent = presentedPaywall,
            didCacheData = shouldCacheTransactionMetadata,
        )
    }

    @OptIn(InternalRevenueCatAPI::class)
    private fun performPostReceipt(
        appUserID: String,
        purchaseToken: String,
        isRestore: Boolean,
        receiptInfo: ReceiptInfo,
        initiationSource: PostReceiptInitiationSource,
        paywallData: PaywallPostReceiptData?,
        purchasesAreCompletedBy: PurchasesAreCompletedBy,
        hasCachedTransactionMetadata: Boolean,
        onSuccess: (PostReceiptResponse) -> Unit,
        onError: PostReceiptDataErrorCallback,
    ) {
        subscriberAttributesManager.getUnsyncedSubscriberAttributes(appUserID) { unsyncedSubscriberAttributesByKey ->
            backend.postReceiptData(
                purchaseToken = purchaseToken,
                appUserID = appUserID,
                isRestore = isRestore,
                finishTransactions = finishTransactions,
                subscriberAttributes = unsyncedSubscriberAttributesByKey.toBackendMap(),
                receiptInfo = receiptInfo,
                initiationSource = initiationSource,
                paywallPostReceiptData = paywallData,
                purchasesAreCompletedBy = purchasesAreCompletedBy,
                onSuccess = { postReceiptResponse ->
                    if (hasCachedTransactionMetadata) {
                        localTransactionMetadataStore.clearLocalTransactionMetadata(setOf(purchaseToken))
                    }

                    offlineEntitlementsManager.resetOfflineCustomerInfoCache()
                    subscriberAttributesManager.markAsSynced(
                        appUserID,
                        unsyncedSubscriberAttributesByKey,
                        postReceiptResponse.body.getAttributeErrors(),
                    )
                    customerInfoUpdateHandler.cacheAndNotifyListeners(postReceiptResponse.customerInfo)
                    onSuccess(postReceiptResponse)
                },
                onError = { error, errorHandlingBehavior, responseBody ->
                    if (errorHandlingBehavior == PostReceiptErrorHandlingBehavior.SHOULD_BE_MARKED_SYNCED) {
                        if (hasCachedTransactionMetadata) {
                            localTransactionMetadataStore.clearLocalTransactionMetadata(setOf(purchaseToken))
                        }
                        subscriberAttributesManager.markAsSynced(
                            appUserID,
                            unsyncedSubscriberAttributesByKey,
                            responseBody.getAttributeErrors(),
                        )
                    }
                    onError(error, errorHandlingBehavior, responseBody)
                },
            )
        }
    }

    private fun useOfflineEntitlementsCustomerInfoIfNeeded(
        errorHandlingBehavior: PostReceiptErrorHandlingBehavior,
        appUserID: String,
        onSuccess: (CustomerInfo) -> Unit,
        onError: () -> Unit,
    ) {
        val isServerError =
            errorHandlingBehavior == PostReceiptErrorHandlingBehavior.SHOULD_USE_OFFLINE_ENTITLEMENTS_AND_NOT_CONSUME
        if (offlineEntitlementsManager.shouldCalculateOfflineCustomerInfoInPostReceipt(isServerError)) {
            calculateOfflineCustomerInfo(
                appUserID,
                onSuccess = onSuccess,
                onError = {
                    onError()
                },
            )
        } else {
            onError()
        }
    }

    private fun calculateOfflineCustomerInfo(
        appUserID: String,
        onSuccess: (CustomerInfo) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        offlineEntitlementsManager.calculateAndCacheOfflineCustomerInfo(
            appUserID,
            onSuccess = { customerInfo ->
                customerInfoUpdateHandler.notifyListeners(customerInfo)
                onSuccess(customerInfo)
            },
            onError = { error ->
                onError(error)
            },
        )
    }
}
