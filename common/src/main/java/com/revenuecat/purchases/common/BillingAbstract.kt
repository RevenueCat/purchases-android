package com.revenuecat.purchases.common

import android.app.Activity
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction

typealias StoreProductsCallback = (List<StoreProduct>) -> Unit

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
        onReceivePurchaseHistory: (List<StoreTransaction>) -> Unit,
        onReceivePurchaseHistoryError: PurchasesErrorCallback
    )

    abstract fun querySkuDetailsAsync(
        productType: ProductType,
        skus: Set<String>,
        onReceive: StoreProductsCallback,
        onError: PurchasesErrorCallback
    )

    abstract fun consumeAndSave(
        shouldTryToConsume: Boolean,
        purchase: StoreTransaction
    )

    abstract fun findPurchaseInPurchaseHistory(
        appUserID: String,
        productType: ProductType,
        sku: String,
        onCompletion: (StoreTransaction) -> Unit,
        onError: (PurchasesError) -> Unit
    )

    abstract fun makePurchaseAsync(
        activity: Activity,
        appUserID: String,
        storeProduct: StoreProduct,
        replaceSkuInfo: ReplaceSkuInfo?,
        presentedOfferingIdentifier: String?
    )

    abstract fun isConnected(): Boolean

    @SuppressWarnings("ForbiddenComment")
    abstract fun queryPurchases(
        appUserID: String,
        onSuccess: (Map<String, StoreTransaction>) -> Unit,
        onError: (PurchasesError) -> Unit
    )

    interface PurchasesUpdatedListener {
        fun onPurchasesUpdated(purchases: List<StoreTransaction>)
        fun onPurchasesFailedToUpdate(purchasesError: PurchasesError)
    }
}
