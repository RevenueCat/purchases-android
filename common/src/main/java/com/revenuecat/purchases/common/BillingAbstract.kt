package com.revenuecat.purchases.common

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.ProductDetails

typealias ProductDetailsListCallback = (List<ProductDetails>) -> Unit

typealias PurchasesErrorCallback = (PurchasesError) -> Unit

abstract class BillingAbstract {

    @get:Synchronized
    @set:Synchronized
    @Volatile
    var stateListener: StateListener? = null

    @get:Synchronized
    @Volatile
    var purchasesUpdatedListener: PurchasesUpdatedListener? = null
        set(value) {
            synchronized(this@BillingAbstract) {
                field = value
            }
            if (value != null) {
                startConnection()
            } else {
                endConnection()
            }
        }

    interface StateListener {
        fun onConnected()
    }

    abstract fun startConnection()

    abstract fun endConnection()

    abstract fun queryAllPurchases(
        onReceivePurchaseHistory: (List<PurchaseHistoryRecordWrapper>) -> Unit,
        onReceivePurchaseHistoryError: PurchasesErrorCallback
    )

    abstract fun querySkuDetailsAsync(
        productType: ProductType,
        skus: Set<String>,
        onReceive: ProductDetailsListCallback,
        onError: PurchasesErrorCallback
    )

    abstract fun consumeAndSave(
        shouldTryToConsume: Boolean,
        purchase: PurchaseWrapper
    )

    abstract fun consumeAndSave(
        shouldTryToConsume: Boolean,
        purchase: PurchaseHistoryRecordWrapper
    )

    abstract fun consumePurchase(
        token: String,
        onConsumed: (billingResult: BillingResult, purchaseToken: String) -> Unit
    )

    abstract fun acknowledge(
        token: String,
        onAcknowledged: (billingResult: BillingResult, purchaseToken: String) -> Unit
    )

    abstract fun findPurchaseInPurchaseHistory(
        skuType: ProductType,
        sku: String,
        completion: (BillingResult, PurchaseHistoryRecordWrapper?) -> Unit
    )

    abstract fun makePurchaseAsync(
        activity: Activity,
        appUserID: String,
        productDetails: ProductDetails,
        replaceSkuInfo: ReplaceSkuInfo?,
        presentedOfferingIdentifier: String?
    )

    abstract fun isConnected(): Boolean

    abstract fun queryPurchases(
        @BillingClient.SkuType skuType: String // TODO: change
    ): QueryPurchasesResult?

    abstract class QueryPurchasesResult(
        val responseCode: Int,
        val purchasesByHashedToken: Map<String, PurchaseWrapper>
    ) {

        abstract fun isSuccessful(): Boolean
    }

    interface PurchasesUpdatedListener {
        fun onPurchasesUpdated(purchases: List<PurchaseWrapper>)
        fun onPurchasesFailedToUpdate(purchasesError: PurchasesError)
    }
}
