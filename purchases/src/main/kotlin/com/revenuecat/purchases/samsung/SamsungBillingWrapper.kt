package com.revenuecat.purchases.samsung

import android.app.Activity
import android.content.Context
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
import com.samsung.android.sdk.iap.lib.helper.IapHelper

@Suppress("TooManyFunctions")
internal class SamsungBillingWrapper(
    private val applicationContext: Context,
    val billingMode: SamsungBillingMode,
    private val mainHandler: Handler,
    stateProvider: PurchasesStateProvider,
) : BillingAbstract(purchasesStateProvider = stateProvider) {

    private var connected = false
    private lateinit var iapHelper: IapHelper

    override fun startConnectionOnMainThread(delayMilliseconds: Long) {
        runOnUIThread {
            startConnection()
        }
    }

    override fun startConnection() {
        if (connected) { return }
        this.iapHelper = IapHelper.getInstance(this.applicationContext)
        this.iapHelper.setOperationMode(this.billingMode.toSamsungOperationMode())
        connected = true
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
        TODO("Not yet implemented")
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

    private fun runOnUIThread(runnable: Runnable) {
        if (Looper.getMainLooper().thread == Thread.currentThread()) {
            runnable.run()
        } else {
            mainHandler.post(runnable)
        }
    }
}
