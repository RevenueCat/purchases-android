package com.revenuecat.purchases

import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.PostReceiptDataErrorCallback
import com.revenuecat.purchases.common.PostReceiptErrorHandlingBehavior
import com.revenuecat.purchases.common.ReceiptInfo
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.networking.PostReceiptResponse
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
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
            onSuccess = { postReceiptResponse ->
                deviceCache.addSuccessfullyPostedToken(purchaseToken)
                onSuccess(postReceiptResponse.customerInfo)
            },
            onError = { backendError, errorHandlingBehavior, _ ->
                if (errorHandlingBehavior == PostReceiptErrorHandlingBehavior.SHOULD_BE_CONSUMED) {
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
        isRestore: Boolean,
        appUserID: String,
        initiationSource: PostReceiptInitiationSource,
        onSuccess: (SuccessfulPurchaseCallback)? = null,
        onError: (ErrorPurchaseCallback)? = null,
    ) {
        val receiptInfo = ReceiptInfo(
            productIDs = purchase.productIds,
            presentedOfferingContext = purchase.presentedOfferingContext,
            storeProduct = storeProduct,
            subscriptionOptionId = purchase.subscriptionOptionId,
            replacementMode = purchase.replacementMode,
        )
        postReceiptAndSubscriberAttributes(
            appUserID = appUserID,
            purchaseToken = purchase.purchaseToken,
            isRestore = isRestore,
            receiptInfo = receiptInfo,
            storeUserID = purchase.storeUserID,
            marketplace = purchase.marketplace,
            initiationSource = initiationSource,
            onSuccess = { postReceiptResponse ->
                // Currently we only support a single token per postReceipt call but multiple product Ids.
                // The backend would fail if given more than one product id (multiline purchases which are
                // not supported) so it's safe to pickup the first one.
                // We would need to refactor this if/when we support multiple tokens per call.
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
                if (errorHandlingBehavior == PostReceiptErrorHandlingBehavior.SHOULD_BE_CONSUMED) {
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

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    private fun postReceiptAndSubscriberAttributes(
        appUserID: String,
        purchaseToken: String,
        isRestore: Boolean,
        receiptInfo: ReceiptInfo,
        storeUserID: String?,
        marketplace: String?,
        initiationSource: PostReceiptInitiationSource,
        onSuccess: (PostReceiptResponse) -> Unit,
        onError: PostReceiptDataErrorCallback,
    ) {
        val presentedPaywall = paywallPresentedCache.getAndRemovePresentedEvent()
        subscriberAttributesManager.getUnsyncedSubscriberAttributes(appUserID) { unsyncedSubscriberAttributesByKey ->
            backend.postReceiptData(
                purchaseToken = purchaseToken,
                appUserID = appUserID,
                isRestore = isRestore,
                observerMode = !finishTransactions,
                subscriberAttributes = unsyncedSubscriberAttributesByKey.toBackendMap(),
                receiptInfo = receiptInfo,
                storeAppUserID = storeUserID,
                marketplace = marketplace,
                initiationSource = initiationSource,
                paywallPostReceiptData = presentedPaywall?.toPaywallPostReceiptData(),
                onSuccess = { postReceiptResponse ->
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
                    if (errorHandlingBehavior == PostReceiptErrorHandlingBehavior.SHOULD_BE_CONSUMED) {
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
