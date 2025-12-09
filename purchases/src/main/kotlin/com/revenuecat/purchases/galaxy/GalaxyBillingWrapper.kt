package com.revenuecat.purchases.galaxy

import android.app.Activity
import com.revenuecat.purchases.PostReceiptInitiationSource
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesStateProvider
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.ReplaceProductInfo
import com.revenuecat.purchases.common.StoreProductsCallback
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.models.InAppMessageType
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.StoreTransaction

@Suppress("TooManyFunctions")
internal class GalaxyBillingWrapper(
    stateProvider: PurchasesStateProvider,
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
        // No-op: Galaxy Store doesn't have in-app messages
    }

    override fun getStorefront(
        onSuccess: (String) -> Unit,
        onError: PurchasesErrorCallback,
    ) {
        log(LogIntent.GALAXY_ERROR) { GalaxyStrings.STOREFRONT_NOT_SUPPORTED }
        onError(
            PurchasesError(
                code = PurchasesErrorCode.UnsupportedError,
                underlyingErrorMessage = GalaxyStrings.STOREFRONT_NOT_SUPPORTED,
            ),
        )
    }
}
