package com.revenuecat.purchases.common

import android.app.Activity
import com.revenuecat.purchases.PostReceiptInitiationSource
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.PurchasesStateProvider
import com.revenuecat.purchases.models.InAppMessageType
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction

internal typealias StoreProductsCallback = (List<StoreProduct>) -> Unit

@SuppressWarnings("TooManyFunctions")
internal abstract class BillingAbstract(
    protected val purchasesStateProvider: PurchasesStateProvider,
) {

    @get:Synchronized
    @set:Synchronized
    @Volatile
    var stateListener: StateListener? = null

    @get:Synchronized
    @Volatile
    var purchasesUpdatedListener: PurchasesUpdatedListener? = null

    interface StateListener {
        fun onConnected()
    }

    abstract fun startConnectionOnMainThread(delayMilliseconds: Long = 0)

    abstract fun startConnection()

    protected abstract fun endConnection()

    fun close() {
        purchasesUpdatedListener = null
        endConnection()
    }

    abstract fun queryAllPurchases(
        appUserID: String,
        onReceivePurchaseHistory: (List<StoreTransaction>) -> Unit,
        onReceivePurchaseHistoryError: PurchasesErrorCallback,
    )

    abstract fun queryProductDetailsAsync(
        productType: ProductType,
        productIds: Set<String>,
        onReceive: StoreProductsCallback,
        onError: PurchasesErrorCallback,
    )

    abstract fun consumeAndSave(
        shouldTryToConsume: Boolean,
        purchase: StoreTransaction,
        initiationSource: PostReceiptInitiationSource,
    )

    @SuppressWarnings("LongParameterList")
    abstract fun findPurchaseInPurchaseHistory(
        appUserID: String,
        productType: ProductType,
        productId: String,
        onCompletion: (StoreTransaction) -> Unit,
        onError: (PurchasesError) -> Unit,
    )

    @SuppressWarnings("LongParameterList")
    abstract fun makePurchaseAsync(
        activity: Activity,
        appUserID: String,
        purchasingData: PurchasingData,
        replaceProductInfo: ReplaceProductInfo?,
        presentedOfferingContext: PresentedOfferingContext?,
        isPersonalizedPrice: Boolean? = null,
    )

    abstract fun isConnected(): Boolean

    abstract fun queryPurchases(
        appUserID: String,
        onSuccess: (Map<String, StoreTransaction>) -> Unit,
        onError: (PurchasesError) -> Unit,
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
        onError: (PurchasesError) -> Unit,
    ) {
        onSuccess(productID)
    }

    abstract fun showInAppMessagesIfNeeded(
        activity: Activity,
        inAppMessageTypes: List<InAppMessageType>,
        subscriptionStatusChange: () -> Unit,
    )

    /**
     * Obtain store country code in ISO 3166-1-alpha-2 standard format.
     * Null if there has been an error.
     */
    abstract fun getStorefront(
        onSuccess: (String) -> Unit,
        onError: PurchasesErrorCallback,
    )

    interface PurchasesUpdatedListener {
        fun onPurchasesUpdated(purchases: List<StoreTransaction>)
        fun onPurchasesFailedToUpdate(purchasesError: PurchasesError)
    }
}
