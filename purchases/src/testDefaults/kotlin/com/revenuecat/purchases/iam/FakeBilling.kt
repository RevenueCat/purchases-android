package com.revenuecat.purchases.iam

import android.app.Activity
import com.revenuecat.purchases.PostReceiptInitiationSource
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesStateProvider
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.ReplaceProductInfo
import com.revenuecat.purchases.common.StoreProductsCallback
import com.revenuecat.purchases.models.InAppMessageType
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.utils.stubStoreProduct
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The store stand-in for the IAM end-to-end suite: it only replays purchases and purchase history. Products are
 * stubs for whatever IDs are asked for; nothing is ever bought through it.
 */
internal class FakeBilling(stateProvider: PurchasesStateProvider) : BillingAbstract(stateProvider) {

    /** What `queryPurchases` / `queryAllPurchases` return (restores, syncs, pending purchases). */
    val ownedPurchases = CopyOnWriteArrayList<StoreTransaction>()
    val consumed = CopyOnWriteArrayList<StoreTransaction>()

    @Volatile
    private var connected = false

    /** A purchase arrives from the store, as `onPurchasesUpdated` does after a purchase flow. */
    fun deliverPurchase(transaction: StoreTransaction) {
        ownedPurchases.add(transaction)
        purchasesUpdatedListener?.onPurchasesUpdated(listOf(transaction))
    }

    override fun startConnection(delayMilliseconds: Long) {
        connected = true
        stateListener?.onConnected()
    }

    override fun endConnection() {
        connected = false
    }

    override fun isConnected(): Boolean = connected

    override fun queryAllPurchases(
        appUserID: String,
        onReceivePurchaseHistory: (List<StoreTransaction>) -> Unit,
        onReceivePurchaseHistoryError: PurchasesErrorCallback,
    ) = onReceivePurchaseHistory(ownedPurchases.toList())

    override fun queryPurchases(
        appUserID: String,
        onSuccess: (Map<String, StoreTransaction>) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) = onSuccess(ownedPurchases.associateBy { it.purchaseToken })

    override fun queryProductDetailsAsync(
        productType: ProductType,
        productIds: Set<String>,
        onReceive: StoreProductsCallback,
        onError: PurchasesErrorCallback,
    ) = onReceive(productIds.map { stubStoreProduct(it) })

    override fun consumeAndSave(
        finishTransactions: Boolean,
        purchase: StoreTransaction,
        shouldConsume: Boolean,
        initiationSource: PostReceiptInitiationSource,
    ) {
        consumed.add(purchase)
    }

    override fun findPurchaseInPurchaseHistory(
        appUserID: String,
        productType: ProductType,
        productId: String,
        onCompletion: (StoreTransaction) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        ownedPurchases.firstOrNull { productId in it.productIds }?.let(onCompletion)
            ?: onError(PurchasesError(PurchasesErrorCode.PurchaseInvalidError))
    }

    @Suppress("LongParameterList")
    override fun makePurchaseAsync(
        activity: Activity,
        appUserID: String,
        purchasingData: PurchasingData,
        replaceProductInfo: ReplaceProductInfo?,
        presentedOfferingContext: PresentedOfferingContext?,
        isPersonalizedPrice: Boolean?,
    ) = error("The IAM suite never starts a purchase flow; use deliverPurchase")

    override fun showInAppMessagesIfNeeded(
        activity: Activity,
        inAppMessageTypes: List<InAppMessageType>,
        subscriptionStatusChange: () -> Unit,
    ) = Unit

    override fun getStorefront(onSuccess: (String) -> Unit, onError: PurchasesErrorCallback) = onSuccess("US")
}
