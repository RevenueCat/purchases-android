package com.revenuecat.purchases

import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.PostReceiptDataErrorCallback
import com.revenuecat.purchases.common.PostReceiptDataSuccessCallback
import com.revenuecat.purchases.common.ReceiptInfo
import com.revenuecat.purchases.common.caching.DeviceCache
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
    private val customerInfoHelper: CustomerInfoHelper,
    private val deviceCache: DeviceCache,
    private val subscriberAttributesManager: SubscriberAttributesManager
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
        onSuccess: () -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        postReceiptAndSubscriberAttributes(
            appUserID,
            purchaseToken,
            isRestore,
            receiptInfo,
            storeUserID,
            marketplace,
            onSuccess = { _, _ ->
                deviceCache.addSuccessfullyPostedToken(purchaseToken)
                onSuccess()
            },
            onError = { error, shouldConsumePurchase, _, _ ->
                if (shouldConsumePurchase) {
                    deviceCache.addSuccessfullyPostedToken(purchaseToken)
                }
                onError(error)
            }
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
        onError: (ErrorPurchaseCallback)? = null
    ) {
        val receiptInfo = ReceiptInfo(
            productIDs = purchase.productIds,
            offeringIdentifier = purchase.presentedOfferingIdentifier,
            storeProduct = storeProduct,
            subscriptionOptionId = purchase.subscriptionOptionId,
            prorationMode = purchase.prorationMode
        )
        postReceiptAndSubscriberAttributes(
            appUserID = appUserID,
            purchaseToken = purchase.purchaseToken,
            isRestore = isRestore,
            receiptInfo = receiptInfo,
            storeUserID = purchase.storeUserID,
            marketplace = purchase.marketplace,
            onSuccess = { info, _ ->
                billing.consumeAndSave(finishTransactions, purchase)
                onSuccess?.let { it(purchase, info) }
            },
            onError = { error, shouldConsumePurchase, _, _ ->
                if (shouldConsumePurchase) {
                    billing.consumeAndSave(finishTransactions, purchase)
                }
                onError?.let { it(purchase, error) }
            }
        )
    }

    private fun postReceiptAndSubscriberAttributes(
        appUserID: String,
        purchaseToken: String,
        isRestore: Boolean,
        receiptInfo: ReceiptInfo,
        storeUserID: String?,
        marketplace: String?,
        onSuccess: PostReceiptDataSuccessCallback,
        onError: PostReceiptDataErrorCallback
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
                    subscriberAttributesManager.markAsSynced(
                        appUserID,
                        unsyncedSubscriberAttributesByKey,
                        responseBody.getAttributeErrors()
                    )
                    customerInfoHelper.cacheCustomerInfo(customerInfo)
                    customerInfoHelper.sendUpdatedCustomerInfoToDelegateIfChanged(customerInfo)
                    onSuccess(customerInfo, responseBody)
                },
                onError = { error, shouldConsumePurchase, isServerError, responseBody ->
                    if (shouldConsumePurchase) {
                        subscriberAttributesManager.markAsSynced(
                            appUserID,
                            unsyncedSubscriberAttributesByKey,
                            responseBody.getAttributeErrors()
                        )
                    }
                    onError(error, shouldConsumePurchase, isServerError, responseBody)
                }
            )
        }
    }
}
