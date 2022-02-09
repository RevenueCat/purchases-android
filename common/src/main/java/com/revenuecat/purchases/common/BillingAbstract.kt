package com.revenuecat.purchases.common

import android.app.Activity
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.models.ProductDetails
import com.revenuecat.purchases.models.PurchaseDetails

typealias ProductDetailsListCallback = (List<ProductDetails>) -> Unit

@SuppressWarnings("TooManyFunctions")
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
        appUserID: String,
        onReceivePurchaseHistory: (List<PurchaseDetails>) -> Unit,
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
        purchase: PurchaseDetails
    )

    abstract fun findPurchaseInPurchaseHistory(
        appUserID: String,
        productType: ProductType,
        sku: String,
        onCompletion: (PurchaseDetails) -> Unit,
        onError: (PurchasesError) -> Unit
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
        appUserID: String,
        onSuccess: (Map<String, PurchaseDetails>) -> Unit,
        onError: (PurchasesError) -> Unit
    )

    /**
     * Amazon has the concept of term and parent product ID. This function will return
     * the correct product ID the RevenueCat backend expects for a specific purchase.
     * Google doesn't need normalization so we return the productID by default
     */
    open fun normalizePurchaseData(
        productID: String,
        purchaseToken: String,
        storeUserID: String,
        onSuccess: (normalizedProductID: String) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        onSuccess(productID)
    }

    interface PurchasesUpdatedListener {
        fun onPurchasesUpdated(purchases: List<PurchaseDetails>)
        fun onPurchasesFailedToUpdate(purchasesError: PurchasesError)
    }
}
