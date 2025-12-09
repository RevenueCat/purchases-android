package com.revenuecat.purchases.galaxy

import android.app.Activity
import android.content.Context
import android.os.Handler
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
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.galaxy.handler.ProductDataHandler
import com.revenuecat.purchases.galaxy.listener.ProductDataResponseListener
import com.revenuecat.purchases.models.InAppMessageType
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.StoreTransaction
import com.samsung.android.sdk.iap.lib.helper.IapHelper

@Suppress("TooManyFunctions")
internal class GalaxyBillingWrapper(
    stateProvider: PurchasesStateProvider,
    private val context: Context,
    private val mainHandler: Handler,
    val billingMode: GalaxyBillingMode,
    private val iapHelperProvider: IAPHelperProvider = DefaultIAPHelperProvider(
        iapHelper = IapHelper.getInstance(context),
    ),
    private val productDataHandler: ProductDataResponseListener =
        ProductDataHandler(
            iapHelper = iapHelperProvider,
            mainHandler = mainHandler,
        ),
) : BillingAbstract(purchasesStateProvider = stateProvider) {

    private val serialRequestExecutor = GalaxySerialRequestExecutor()

    override fun startConnectionOnMainThread(delayMilliseconds: Long) {
        warnLog { "Unimplemented: GalaxyBillingWrapper.startConnectionOnMainThread" }
    }

    override fun startConnection() {
        warnLog { "Unimplemented: GalaxyBillingWrapper.startConnection" }
    }

    override fun endConnection() {
        warnLog { "Unimplemented: GalaxyBillingWrapper.endConnection" }
    }

    override fun queryAllPurchases(
        appUserID: String,
        onReceivePurchaseHistory: (List<StoreTransaction>) -> Unit,
        onReceivePurchaseHistoryError: PurchasesErrorCallback,
    ) {
        warnLog { "Unimplemented: GalaxyBillingWrapper.queryAllPurchases" }
        onReceivePurchaseHistory(emptyList())
    }

    override fun queryProductDetailsAsync(
        productType: ProductType,
        productIds: Set<String>,
        onReceive: StoreProductsCallback,
        onError: PurchasesErrorCallback,
    ) {
        if (purchasesUpdatedListener == null) return

        serialRequestExecutor.executeSerially { finish ->
            @Suppress("ForbiddenComment")
            // TODO: Record diagnostics
            productDataHandler.getProductDetails(
                productIds = productIds,
                productType = productType,
                onReceive = {
                    onReceive(it)
                    finish()
                },
                onError = {
                    onError(it)
                    finish()
                },
            )
        }
    }

    override fun consumeAndSave(
        finishTransactions: Boolean,
        purchase: StoreTransaction,
        shouldConsume: Boolean,
        initiationSource: PostReceiptInitiationSource,
    ) {
        warnLog { "Unimplemented: GalaxyBillingWrapper.consumeAndSave" }
    }

    override fun findPurchaseInPurchaseHistory(
        appUserID: String,
        productType: ProductType,
        productId: String,
        onCompletion: (StoreTransaction) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        warnLog { "Unimplemented: GalaxyBillingWrapper.findPurchaseInPurchaseHistory" }
        onError(PurchasesError(code = PurchasesErrorCode.UnknownError))
    }

    override fun makePurchaseAsync(
        activity: Activity,
        appUserID: String,
        purchasingData: PurchasingData,
        replaceProductInfo: ReplaceProductInfo?,
        presentedOfferingContext: PresentedOfferingContext?,
        isPersonalizedPrice: Boolean?,
    ) {
        warnLog { "Unimplemented: GalaxyBillingWrapper.makePurchaseAsync" }
    }

    override fun isConnected(): Boolean = true

    override fun queryPurchases(
        appUserID: String,
        onSuccess: (Map<String, StoreTransaction>) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        warnLog { "Unimplemented: GalaxyBillingWrapper.queryPurchases" }
        onSuccess(emptyMap())
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
