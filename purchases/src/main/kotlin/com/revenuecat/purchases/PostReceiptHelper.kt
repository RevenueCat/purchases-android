package com.revenuecat.purchases

import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.PostReceiptDataErrorCallback
import com.revenuecat.purchases.common.PostReceiptErrorHandlingBehavior
import com.revenuecat.purchases.common.ReceiptInfo
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
import com.revenuecat.purchases.subscriberattributes.getAttributeErrors
import com.revenuecat.purchases.subscriberattributes.toBackendMap

@Suppress("LongParameterList")
internal class PostReceiptHelper(
    private val appConfig: AppConfig,
    private val backend: Backend,
    private val billing: BillingAbstract,
    private val customerInfoUpdater: CustomerInfoUpdater,
    private val deviceCache: DeviceCache,
    private val subscriberAttributesManager: SubscriberAttributesManager,
    private val offlineEntitlementsManager: OfflineEntitlementsManager,
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
            onSuccess = {
                deviceCache.addSuccessfullyPostedToken(purchaseToken)
                onSuccess(it)
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
        onSuccess: (SuccessfulPurchaseCallback)? = null,
        onError: (ErrorPurchaseCallback)? = null,
    ) {
        val receiptInfo = ReceiptInfo(
            productIDs = purchase.productIds,
            offeringIdentifier = purchase.presentedOfferingIdentifier,
            storeProduct = storeProduct,
            subscriptionOptionId = purchase.subscriptionOptionId,
            prorationMode = purchase.prorationMode,
        )
        postReceiptAndSubscriberAttributes(
            appUserID = appUserID,
            purchaseToken = purchase.purchaseToken,
            isRestore = isRestore,
            receiptInfo = receiptInfo,
            storeUserID = purchase.storeUserID,
            marketplace = purchase.marketplace,
            onSuccess = { info ->
                billing.consumeAndSave(finishTransactions, purchase)
                onSuccess?.let { it(purchase, info) }
            },
            onError = { backendError, errorHandlingBehavior, _ ->
                if (errorHandlingBehavior == PostReceiptErrorHandlingBehavior.SHOULD_BE_CONSUMED) {
                    billing.consumeAndSave(finishTransactions, purchase)
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

    private fun postReceiptAndSubscriberAttributes(
        appUserID: String,
        purchaseToken: String,
        isRestore: Boolean,
        receiptInfo: ReceiptInfo,
        storeUserID: String?,
        marketplace: String?,
        onSuccess: (CustomerInfo) -> Unit,
        onError: PostReceiptDataErrorCallback,
    ) {
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
                onSuccess = { customerInfo, responseBody ->
                    offlineEntitlementsManager.resetOfflineCustomerInfoCache()
                    subscriberAttributesManager.markAsSynced(
                        appUserID,
                        unsyncedSubscriberAttributesByKey,
                        responseBody.getAttributeErrors(),
                    )
                    customerInfoUpdater.cacheAndNotifyListeners(customerInfo)
                    onSuccess(customerInfo)
                },
                onError = { error, errorHandlingBehavior, responseBody ->
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
                customerInfoUpdater.notifyListeners(customerInfo)
                onSuccess(customerInfo)
            },
            onError = { error ->
                onError(error)
            },
        )
    }
}
