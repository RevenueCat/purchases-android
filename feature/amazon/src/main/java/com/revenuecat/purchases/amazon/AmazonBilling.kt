package com.revenuecat.purchases.amazon

import android.app.Activity
import android.content.Context
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.FulfillmentResult
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.amazon.device.iap.model.UserDataResponse
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.amazon.handler.ProductDataHandler
import com.revenuecat.purchases.amazon.handler.PurchaseHandler
import com.revenuecat.purchases.amazon.handler.PurchaseUpdatesHandler
import com.revenuecat.purchases.amazon.handler.UserDataHandler
import com.revenuecat.purchases.amazon.listener.ProductDataResponseListener
import com.revenuecat.purchases.amazon.listener.PurchaseResponseListener
import com.revenuecat.purchases.amazon.listener.PurchaseUpdatesResponseListener
import com.revenuecat.purchases.amazon.listener.UserDataResponseListener
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.ProductDetailsListCallback
import com.revenuecat.purchases.common.PurchaseHistoryRecordWrapper
import com.revenuecat.purchases.common.PurchaseWrapper
import com.revenuecat.purchases.common.PurchasesErrorCallback
import com.revenuecat.purchases.common.ReplaceSkuInfo
import com.revenuecat.purchases.common.RevenueCatPurchaseState
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.models.ProductDetails

class AmazonBilling constructor(
    private val applicationContext: Context,
    private val backend: AmazonBackend,
    private val productDataHandler: ProductDataResponseListener = ProductDataHandler(),
    private val purchaseHandler: PurchaseResponseListener = PurchaseHandler(),
    private val purchaseUpdatesHandler: PurchaseUpdatesResponseListener = PurchaseUpdatesHandler(),
    private val userDataHandler: UserDataResponseListener = UserDataHandler()
) : BillingAbstract(),
    ProductDataResponseListener by productDataHandler,
    PurchaseResponseListener by purchaseHandler,
    PurchaseUpdatesResponseListener by purchaseUpdatesHandler,
    UserDataResponseListener by userDataHandler {

    // Used for constructing the class via Reflection. Make sure to update any call if updating this constructor
    @Suppress("unused")
    constructor(
        applicationContext: Context,
        backend: Backend
    ) : this(applicationContext, AmazonBackend(backend))

    var connected = false

    override fun startConnection() {
        PurchasingService.registerListener(applicationContext, this)
        connected = true
    }

    override fun endConnection() { }

    override fun queryAllPurchases(
        onReceivePurchaseHistory: (List<PurchaseHistoryRecordWrapper>) -> Unit,
        onReceivePurchaseHistoryError: PurchasesErrorCallback
    ) {
        // TODO
    }

    // region Product Data

    override fun querySkuDetailsAsync(
        productType: ProductType,
        skuList: List<String>,
        onReceive: ProductDetailsListCallback,
        onError: PurchasesErrorCallback
    ) {
        userDataHandler.getUserData { userData ->
            productDataHandler.getProductData(skuList, userData.marketplace, onReceive, onError)
        }
    }

    // endregion

    override fun consumeAndSave(
        shouldTryToConsume: Boolean,
        purchase: PurchaseWrapper
    ) {
        if (purchase !is AmazonPurchaseWrapper) return

        if (purchase.type == ProductType.UNKNOWN) {
            return
        }

        if (purchase.purchaseState != RevenueCatPurchaseState.PURCHASED) {
            // PENDING purchases should not be fulfilled
            return
        }

        if (shouldTryToConsume) {
            PurchasingService.notifyFulfillment(purchase.purchaseToken, FulfillmentResult.FULFILLED)
        }

        // TODO: add logic for Unavailable
    }

    override fun consumeAndSave(
        shouldTryToConsume: Boolean,
        purchase: PurchaseHistoryRecordWrapper
    ) {
        // TODO
    }

    override fun findPurchaseInPurchaseHistory(
        skuType: ProductType,
        sku: String,
        completion: (BillingResult, PurchaseHistoryRecordWrapper?) -> Unit
    ) {
        // TODO
    }

    override fun makePurchaseAsync(
        activity: Activity,
        appUserID: String,
        productDetails: ProductDetails,
        replaceSkuInfo: ReplaceSkuInfo?,
        presentedOfferingIdentifier: String?
    ) {
        if (replaceSkuInfo != null) {
            errorLog("Amazon doesn't support product changes")
            return
        }
        purchaseHandler.purchase(
            appUserID,
            productDetails,
            presentedOfferingIdentifier,
            onSuccess = { receipt, userData ->
                backend.postAmazonReceiptData(
                    receipt.receiptId,
                    appUserID,
                    userData.userId,
                    productDetails,
                    onSuccessHandler = { response ->
                        val termSku = response["termSku"] as String
                        val amazonPurchaseWrapper = AmazonPurchaseWrapper(
                            sku = termSku,
                            containedReceipt = receipt,
                            presentedOfferingIdentifier = presentedOfferingIdentifier,
                            purchaseState = RevenueCatPurchaseState.PURCHASED
                        )
                        purchasesUpdatedListener?.onPurchasesUpdated(listOf(amazonPurchaseWrapper))
                    },
                    onErrorHandler = { error ->
                        purchasesUpdatedListener?.onPurchasesFailedToUpdate(error)
                    }
                )
            },
            onError = { error ->
                purchasesUpdatedListener?.onPurchasesFailedToUpdate(error)
            }
        )
    }

    override fun isConnected(): Boolean = connected

    override fun queryPurchases(skuType: String): BillingAbstract.QueryPurchasesResult? {
        // TODO()
        return AmazonQueryPurchasesResult(0, emptyMap())
    }

    // AmazonBilling delegates functionality to interfaces that have a common parent interface, it will only
    // compile as long as all of the functions are implemented, otherwise it doesn't know which delegated
    // implementation to take
    override fun onUserDataResponse(response: UserDataResponse) {
        userDataHandler.onUserDataResponse(response)
    }

    override fun onProductDataResponse(response: ProductDataResponse) {
        productDataHandler.onProductDataResponse(response)
    }

    override fun onPurchaseResponse(response: PurchaseResponse) {
        purchaseHandler.onPurchaseResponse(response)
    }

    override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse) {
        purchaseUpdatesHandler.onPurchaseUpdatesResponse(response)
    }

    class AmazonQueryPurchasesResult(
        responseCode: Int, // TODO convert from AmazonQueryPurchasesResult
        purchasesByHashedToken: Map<String, PurchaseWrapper>
    ) : BillingAbstract.QueryPurchasesResult(responseCode, purchasesByHashedToken) {

        override fun isSuccessful(): Boolean = responseCode == BillingClient.BillingResponseCode.OK
    }
}
