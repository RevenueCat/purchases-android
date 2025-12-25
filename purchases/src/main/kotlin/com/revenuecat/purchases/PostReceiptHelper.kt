package com.revenuecat.purchases

import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.PostReceiptDataErrorCallback
import com.revenuecat.purchases.common.PostReceiptErrorHandlingBehavior
import com.revenuecat.purchases.common.ReceiptInfo
import com.revenuecat.purchases.common.caching.CachedPurchaseData
import com.revenuecat.purchases.common.caching.CachedPurchaseDataCache
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.networking.PostReceiptResponse
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.paywalls.PaywallPresentedCache
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
import com.revenuecat.purchases.subscriberattributes.getAttributeErrors
import com.revenuecat.purchases.subscriberattributes.toBackendMap

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
    private val cachedPurchaseDataCache: CachedPurchaseDataCache,
) {
    private val finishTransactions: Boolean
        get() = appConfig.finishTransactions

    /**
     * This method will post a token and receiptInfo to the backend without consuming any purchases.
     * It will store that the token was sent to the backend so it doesn't send it again.
     */
    fun postTokenWithoutConsuming(
        purchaseToken: String,
        storeUserID: String?,
        receiptInfo: ReceiptInfo,
        isRestore: Boolean,
        appUserID: String,
        marketplace: String?,
        initiationSource: PostReceiptInitiationSource,
        onSuccess: (CustomerInfo) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        postReceiptAndSubscriberAttributes(
            appUserID,
            purchaseToken,
            isRestore,
            receiptInfo,
            storeUserID,
            marketplace,
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
        onSuccess: (SuccessfulPurchaseCallback)? = null,
        onError: (ErrorPurchaseCallback)? = null,
    ) {
        val receiptInfo = ReceiptInfo.from(
            storeTransaction = purchase,
            storeProduct = storeProduct,
            subscriptionOptionsForProductIDs = subscriptionOptionForProductIDs,
        )
        postReceiptAndSubscriberAttributes(
            appUserID = appUserID,
            purchaseToken = purchase.purchaseToken,
            isRestore = isRestore,
            receiptInfo = receiptInfo,
            storeUserID = purchase.storeUserID,
            marketplace = purchase.marketplace,
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

    @Suppress("LongMethod")
    @OptIn(InternalRevenueCatAPI::class)
    private fun postReceiptAndSubscriberAttributes(
        appUserID: String,
        purchaseToken: String,
        isRestore: Boolean,
        receiptInfo: ReceiptInfo,
        storeUserID: String?,
        marketplace: String?,
        initiationSource: PostReceiptInitiationSource,
        purchaseState: PurchaseState,
        onSuccess: (PostReceiptResponse) -> Unit,
        onError: PostReceiptDataErrorCallback,
    ) {
        val cachedData = cachedPurchaseDataCache.getCachedPurchaseData(purchaseToken)

        val presentedPaywall = paywallPresentedCache.getAndRemovePresentedEvent()
        val effectivePaywallData = presentedPaywall?.toPaywallPostReceiptData()
            ?: cachedData?.paywallPostReceiptData
        val effectiveReceiptInfo = cachedData?.receiptInfo?.let { receiptInfo.merge(it) } ?: receiptInfo

        if (cachedData == null) {
            val dataToCache = CachedPurchaseData(
                receiptInfo = receiptInfo,
                paywallPostReceiptData = effectivePaywallData,
                observerMode = !finishTransactions,
            )
            cachedPurchaseDataCache.cachePurchaseData(purchaseToken, dataToCache)
        }

        val originalObserverMode = cachedData?.observerMode

        if (purchaseState == PurchaseState.PENDING) {
            onError(
                PurchasesError(PurchasesErrorCode.PaymentPendingError).also { errorLog(it) },
                PostReceiptErrorHandlingBehavior.SHOULD_NOT_CONSUME,
                null,
            )
            return
        }

        subscriberAttributesManager.getUnsyncedSubscriberAttributes(appUserID) { unsyncedSubscriberAttributesByKey ->
            backend.postReceiptData(
                purchaseToken = purchaseToken,
                appUserID = appUserID,
                isRestore = isRestore,
                finishTransactions = finishTransactions,
                subscriberAttributes = unsyncedSubscriberAttributesByKey.toBackendMap(),
                receiptInfo = effectiveReceiptInfo,
                storeAppUserID = storeUserID,
                marketplace = marketplace,
                initiationSource = initiationSource,
                paywallPostReceiptData = effectivePaywallData,
                originalObserverMode = originalObserverMode,
                onSuccess = { postReceiptResponse ->
                    cachedPurchaseDataCache.clearCachedPurchaseData(purchaseToken)

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
                    presentedPaywall?.let { paywallPresentedCache.cachePresentedPaywall(it) }
                    if (errorHandlingBehavior == PostReceiptErrorHandlingBehavior.SHOULD_BE_MARKED_SYNCED) {
                        cachedPurchaseDataCache.clearCachedPurchaseData(purchaseToken)
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
