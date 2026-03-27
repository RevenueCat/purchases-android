package com.revenuecat.purchases.common

import android.app.Activity
import com.revenuecat.purchases.AmazonLWAConsentStatus
import com.revenuecat.purchases.InternalRevenueCatAPI
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

@InternalRevenueCatAPI
public typealias StoreProductsCallback = (List<StoreProduct>) -> Unit

@SuppressWarnings("TooManyFunctions")
@InternalRevenueCatAPI
public abstract class BillingAbstract(
    protected val purchasesStateProvider: PurchasesStateProvider,
) {

    @get:Synchronized
    @set:Synchronized
    @Volatile
    public var stateListener: StateListener? = null

    @get:Synchronized
    @Volatile
    public var purchasesUpdatedListener: PurchasesUpdatedListener? = null

    public interface StateListener {
        public fun onConnected()
    }

    public abstract fun startConnectionOnMainThread(delayMilliseconds: Long = 0)

    public abstract fun startConnection()

    protected abstract fun endConnection()

    public fun close() {
        purchasesUpdatedListener = null
        endConnection()
    }

    public abstract fun queryAllPurchases(
        appUserID: String,
        onReceivePurchaseHistory: (List<StoreTransaction>) -> Unit,
        onReceivePurchaseHistoryError: PurchasesErrorCallback,
    )

    public abstract fun queryProductDetailsAsync(
        productType: ProductType,
        productIds: Set<String>,
        onReceive: StoreProductsCallback,
        onError: PurchasesErrorCallback,
    )

    public abstract fun consumeAndSave(
        finishTransactions: Boolean,
        purchase: StoreTransaction,
        shouldConsume: Boolean,
        initiationSource: PostReceiptInitiationSource,
    )

    @SuppressWarnings("LongParameterList")
    public abstract fun findPurchaseInPurchaseHistory(
        appUserID: String,
        productType: ProductType,
        productId: String,
        onCompletion: (StoreTransaction) -> Unit,
        onError: (PurchasesError) -> Unit,
    )

    @SuppressWarnings("LongParameterList")
    public abstract fun makePurchaseAsync(
        activity: Activity,
        appUserID: String,
        purchasingData: PurchasingData,
        replaceProductInfo: ReplaceProductInfo?,
        presentedOfferingContext: PresentedOfferingContext?,
        isPersonalizedPrice: Boolean? = null,
    )

    public abstract fun isConnected(): Boolean

    public abstract fun queryPurchases(
        appUserID: String,
        onSuccess: (Map<String, StoreTransaction>) -> Unit,
        onError: (PurchasesError) -> Unit,
    )

    /**
     * Amazon has the concept of term and parent product ID. This function will return
     * the correct product ID the RevenueCat backend expects for a specific purchase.
     * Google doesn't need normalization so we return the productID by default
     */
    public open fun normalizePurchaseData(
        productID: String,
        purchaseToken: String,
        storeUserID: String,
        onSuccess: (normalizedProductID: String) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        onSuccess(productID)
    }

    public abstract fun showInAppMessagesIfNeeded(
        activity: Activity,
        inAppMessageTypes: List<InAppMessageType>,
        subscriptionStatusChange: () -> Unit,
    )

    /**
     * Obtain store country code in ISO 3166-1-alpha-2 standard format.
     * Null if there has been an error.
     */
    public abstract fun getStorefront(
        onSuccess: (String) -> Unit,
        onError: PurchasesErrorCallback,
    )

    public open fun getAmazonLWAConsentStatus(
        onSuccess: (AmazonLWAConsentStatus) -> Unit,
        onError: PurchasesErrorCallback,
    ) {
        onSuccess(AmazonLWAConsentStatus.UNAVAILABLE)
    }

    public interface PurchasesUpdatedListener {
        public fun onPurchasesUpdated(purchases: List<StoreTransaction>)
        public fun onPurchasesFailedToUpdate(purchasesError: PurchasesError)
    }
}
