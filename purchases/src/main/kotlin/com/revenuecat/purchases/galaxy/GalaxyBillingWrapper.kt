package com.revenuecat.purchases.galaxy

import android.app.Activity
import android.os.Handler
import android.os.Looper
import com.revenuecat.purchases.PostReceiptInitiationSource
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.PurchasesStateProvider
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.ReplaceProductInfo
import com.revenuecat.purchases.common.StoreProductsCallback
import com.revenuecat.purchases.models.InAppMessageType
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.StoreTransaction
import java.util.concurrent.ConcurrentLinkedQueue

@Suppress("TooManyFunctions")
internal class GalaxyBillingWrapper(
    stateProvider: PurchasesStateProvider,
    private val mainHandler: Handler,
    val billingMode: GalaxyBillingMode,
) : BillingAbstract(purchasesStateProvider = stateProvider) {
    override fun startConnectionOnMainThread(delayMilliseconds: Long) {
        TODO("Not yet implemented")
    }

    override fun startConnection() {
        TODO("Not yet implemented")
    }

    override fun endConnection() {
        TODO("Not yet implemented")
    }

    override fun queryAllPurchases(
        appUserID: String,
        onReceivePurchaseHistory: (List<StoreTransaction>) -> Unit,
        onReceivePurchaseHistoryError: PurchasesErrorCallback,
    ) {
        TODO("Not yet implemented")
    }

    override fun queryProductDetailsAsync(
        productType: ProductType,
        productIds: Set<String>,
        onReceive: StoreProductsCallback,
        onError: PurchasesErrorCallback,
    ) {
        executeRequestOnUIThread { connectionError ->
            if (connectionError == null) {
                // TODO: Diagnostics tracking
            } else {
                onError(connectionError)
            }
        }
    }

    override fun consumeAndSave(
        finishTransactions: Boolean,
        purchase: StoreTransaction,
        shouldConsume: Boolean,
        initiationSource: PostReceiptInitiationSource,
    ) {
        TODO("Not yet implemented")
    }

    override fun findPurchaseInPurchaseHistory(
        appUserID: String,
        productType: ProductType,
        productId: String,
        onCompletion: (StoreTransaction) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        TODO("Not yet implemented")
    }

    override fun makePurchaseAsync(
        activity: Activity,
        appUserID: String,
        purchasingData: PurchasingData,
        replaceProductInfo: ReplaceProductInfo?,
        presentedOfferingContext: PresentedOfferingContext?,
        isPersonalizedPrice: Boolean?,
    ) {
        TODO("Not yet implemented")
    }

    override fun isConnected(): Boolean {
        TODO("Not yet implemented")
    }

    override fun queryPurchases(
        appUserID: String,
        onSuccess: (Map<String, StoreTransaction>) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        TODO("Not yet implemented")
    }

    override fun showInAppMessagesIfNeeded(
        activity: Activity,
        inAppMessageTypes: List<InAppMessageType>,
        subscriptionStatusChange: () -> Unit,
    ) {
        TODO("Not yet implemented")
    }

    override fun getStorefront(
        onSuccess: (String) -> Unit,
        onError: PurchasesErrorCallback,
    ) {
        TODO("Not yet implemented")
    }

    private val serviceRequests = ConcurrentLinkedQueue<(connectionError: PurchasesError?) -> Unit>()
    @Synchronized
    private fun executeRequestOnUIThread(request: (PurchasesError?) -> Unit) {
        if (purchasesUpdatedListener != null) {
            serviceRequests.add(request)
            if (!isConnected()) {
                startConnectionOnMainThread()
            } else {
                executePendingRequests()
            }
        }
    }

    private fun executePendingRequests() {
        synchronized(this@GalaxyBillingWrapper) {
            while (isConnected() && !serviceRequests.isEmpty()) {
                val serviceRequest = serviceRequests.remove()
                runOnUIThread { serviceRequest(null) }
            }
        }
    }

    private fun runOnUIThread(runnable: Runnable) {
        if (Looper.getMainLooper().thread == Thread.currentThread()) {
            runnable.run()
        } else {
            mainHandler.post(runnable)
        }
    }
}
