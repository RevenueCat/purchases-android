package com.revenuecat.purchases.galaxy

import android.app.Activity
import android.content.Context
import com.revenuecat.purchases.PostReceiptInitiationSource
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesStateProvider
import com.revenuecat.purchases.amazon.AmazonStrings
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.ReplaceProductInfo
import com.revenuecat.purchases.common.StoreProductsCallback
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.galaxy.handler.ProductDataHandler
import com.revenuecat.purchases.galaxy.handler.PurchaseHandler
import com.revenuecat.purchases.galaxy.listener.ProductDataResponseListener
import com.revenuecat.purchases.galaxy.listener.PurchaseResponseListener
import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import com.revenuecat.purchases.models.InAppMessageType
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.strings.PurchaseStrings
import com.revenuecat.purchases.utils.SerialRequestExecutor
import com.samsung.android.sdk.iap.lib.helper.IapHelper
import com.samsung.android.sdk.iap.lib.vo.PurchaseVo

@Suppress("TooManyFunctions")
internal class GalaxyBillingWrapper(
    stateProvider: PurchasesStateProvider,
    private val context: Context,
    private val finishTransactions: Boolean,
    val billingMode: GalaxyBillingMode,
    private val iapHelperProvider: IAPHelperProvider = DefaultIAPHelperProvider(
        iapHelper = IapHelper.getInstance(context),
    ),
    private val productDataHandler: ProductDataResponseListener =
        ProductDataHandler(
            iapHelper = iapHelperProvider,
        ),
    private val purchaseHandler: PurchaseResponseListener =
        PurchaseHandler(
            iapHelper = iapHelperProvider,
        ),
) : BillingAbstract(purchasesStateProvider = stateProvider) {

    private val serialRequestExecutor = SerialRequestExecutor()

    override fun startConnectionOnMainThread(delayMilliseconds: Long) {
        // No-op
    }

    override fun startConnection() {
        // No-op
    }

    override fun endConnection() {
        // No-op
    }

    override fun queryAllPurchases(
        appUserID: String,
        onReceivePurchaseHistory: (List<StoreTransaction>) -> Unit,
        onReceivePurchaseHistoryError: PurchasesErrorCallback,
    ) {
        warnLog { "Unimplemented: GalaxyBillingWrapper.queryAllPurchases" }
        onReceivePurchaseHistory(emptyList())
    }

    @OptIn(GalaxySerialOperation::class)
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

    @OptIn(GalaxySerialOperation::class)
    override fun makePurchaseAsync(
        activity: Activity,
        appUserID: String,
        purchasingData: PurchasingData,
        replaceProductInfo: ReplaceProductInfo?,
        presentedOfferingContext: PresentedOfferingContext?,
        isPersonalizedPrice: Boolean?,
    ) {
        val galaxyPurchaseInfo = purchasingData as? GalaxyPurchasingData.Product
        if (galaxyPurchaseInfo == null) {
            val error = PurchasesError(
                PurchasesErrorCode.UnknownError,
                PurchaseStrings.INVALID_PURCHASE_TYPE.format(
                    "Galaxy",
                    "GalaxyPurchasingData",
                ),
            )
            errorLog(error)
            purchasesUpdatedListener?.onPurchasesFailedToUpdate(error)
            return
        }
        val storeProduct = galaxyPurchaseInfo.storeProduct

        if (!shouldFinishTransactions()) { return }

        if (replaceProductInfo != null) {
            log(LogIntent.GALAXY_WARNING) { GalaxyStrings.PRODUCT_CHANGES_NOT_SUPPORTED }
            return
        }

        serialRequestExecutor.executeSerially { finish ->
            purchaseHandler.purchase(
                appUserID = appUserID,
                storeProduct = storeProduct,
                onSuccess = { receipt ->
                    handleReceipt(
                        receipt = receipt,
                        storeProduct = storeProduct,
                        presentedOfferingContext = presentedOfferingContext,
                    )
                    finish()
                },
                onError = { purchasesError ->
                    onPurchaseError(error = purchasesError)
                    finish()
                }
            )
        }
    }

    private fun handleReceipt(
        receipt: PurchaseVo,
        storeProduct: StoreProduct,
        presentedOfferingContext: PresentedOfferingContext?,
    ) {
        val storeTransaction = receipt.toStoreTransaction(
            productId = storeProduct.id,
            presentedOfferingContext = presentedOfferingContext,
            purchaseState = PurchaseState.PURCHASED,
        )

        purchasesUpdatedListener?.onPurchasesUpdated(purchases = listOf(storeTransaction))
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

    private fun onPurchaseError(error: PurchasesError) {
        purchasesUpdatedListener?.onPurchasesFailedToUpdate(error)
    }

    private fun shouldFinishTransactions(): Boolean {
        return if (finishTransactions) {
            true
        } else {
            log(LogIntent.AMAZON_WARNING) { AmazonStrings.WARNING_AMAZON_NOT_FINISHING_TRANSACTIONS }
            false
        }
    }
}
