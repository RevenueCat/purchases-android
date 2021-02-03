package com.revenuecat.purchases.amazon

import android.app.Activity
import android.content.Context
import android.service.autofill.UserData
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.FulfillmentResult
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.ProductType
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.amazon.device.iap.model.Receipt
import com.amazon.device.iap.model.UserData
import com.amazon.device.iap.model.UserDataResponse
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.amazon.handler.ProductDataHandler
import com.revenuecat.purchases.amazon.handler.PurchaseHandler
import com.revenuecat.purchases.amazon.handler.PurchaseUpdatesHandler
import com.revenuecat.purchases.amazon.handler.UserDataHandler
import com.revenuecat.purchases.amazon.listener.ProductDataResponseListener
import com.revenuecat.purchases.amazon.listener.PurchaseResponseListener
import com.revenuecat.purchases.amazon.listener.PurchaseUpdatesResponseListener
import com.revenuecat.purchases.amazon.listener.UserDataResponseListener
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.ProductDetailsListCallback
import com.revenuecat.purchases.common.PurchaseHistoryRecordWrapper
import com.revenuecat.purchases.common.PurchaseWrapper
import com.revenuecat.purchases.common.PurchasesErrorCallback
import com.revenuecat.purchases.common.ReplaceSkuInfo
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.models.ProductDetails
import com.revenuecat.purchases.models.PurchaseDetails
import com.revenuecat.purchases.models.RevenueCatPurchaseState
import com.revenuecat.purchases.strings.PurchaseStrings
import com.revenuecat.purchases.strings.RestoreStrings
import com.revenuecat.purchases.ProductType as RevenueCatProductType

@SuppressWarnings("LongParameterList")
internal class AmazonBilling constructor(
    private val applicationContext: Context,
    private val amazonBackend: AmazonBackend,
    private val cache: AmazonCache,
    private val productDataHandler: ProductDataResponseListener = ProductDataHandler(),
    private val purchaseHandler: PurchaseResponseListener = PurchaseHandler(),
    private val purchaseUpdatesHandler: PurchaseUpdatesResponseListener = PurchaseUpdatesHandler(),
    private val userDataHandler: UserDataResponseListener = UserDataHandler()
) : BillingAbstract(),
    ProductDataResponseListener by productDataHandler,
    PurchaseResponseListener by purchaseHandler,
    PurchaseUpdatesResponseListener by purchaseUpdatesHandler,
    UserDataResponseListener by userDataHandler {

    // Used for constructing the class via Reflection. Make sure to update any call if updating this constructor
    @Suppress("unused")
    constructor(
        applicationContext: Context,
        backend: Backend,
        cache: DeviceCache
    ) : this(applicationContext, AmazonBackend(backend), AmazonCache(cache))

    var connected = false

    override fun startConnection() {
        PurchasingService.registerListener(applicationContext, this)
        connected = true
    }

    @SuppressWarnings("EmptyFunctionBlock")
    override fun endConnection() { }

    override fun queryAllPurchases(
        appUserID: String,
        onReceivePurchaseHistory: (List<PurchaseDetails>) -> Unit,
        onReceivePurchaseHistoryError: PurchasesErrorCallback
    ) {
        purchaseUpdatesHandler.queryPurchases(
            onSuccess = { receipts, userData ->
                if (receipts.isEmpty()) {
                    onReceivePurchaseHistory(emptyList())
                    return@queryPurchases
                }
                getMissingSkusForReceipts(
                    amazonUserID = userData.userId,
                    receipts
                ) { tokensToSkusMap, errors ->
                    logErrorsIfAny(errors)

                    if (tokensToSkusMap.isEmpty()) {
                        val error = PurchasesError(
                            PurchasesErrorCode.InvalidReceiptError,
                            AmazonStrings.ERROR_FETCHING_PURCHASE_HISTORY_ALL_RECEIPTS_INVALID
                        )
                        onReceivePurchaseHistoryError(error)
                        return@getMissingSkusForReceipts
                    }

                    val purchaseHistoryRecordWrappers =
                    receipts.toPurchaseHistoryRecordWrappers(tokensToSkusMap, userData)

                    onReceivePurchaseHistory(purchaseHistoryRecordWrappers)
                }
            },
            onReceivePurchaseHistoryError
        )
    }

    private fun List<Receipt>.toPurchaseHistoryRecordWrappers(
        tokensToSkusMap: Map<String, String>,
        userData: UserData
    ): List<PurchaseHistoryRecordWrapper> {
        return this.mapNotNull { receipt ->
            val sku = tokensToSkusMap[receipt.receiptId]
            if (sku == null) {
                log(LogIntent.AMAZON_ERROR, AmazonStrings.ERROR_FINDING_RECEIPT_SKU)
                return@mapNotNull null
            }
            receipt.toRevenueCatPurchaseDetails(
                sku = sku,
                presentedOfferingIdentifier = null,
                purchaseState = RevenueCatPurchaseState.UNSPECIFIED_STATE,
                storeUserID = userData.userId
            )
        }
    }

    // region Product Data

    override fun querySkuDetailsAsync(
        productType: RevenueCatProductType,
        skus: Set<String>,
        onReceive: ProductDetailsListCallback,
        onError: PurchasesErrorCallback
    ) {
        userDataHandler.getUserData { userData ->
            productDataHandler.getProductData(skus, userData.marketplace, onReceive, onError)
        }
    }

    // endregion

    override fun consumeAndSave(
        shouldTryToConsume: Boolean,
        purchase: PurchaseDetails
    ) {
        if (purchase.type == RevenueCatProductType.UNKNOWN) return

        // PENDING purchases should not be fulfilled
        if (purchase.purchaseState == RevenueCatPurchaseState.PENDING) return

        if (shouldTryToConsume) {
            PurchasingService.notifyFulfillment(purchase.purchaseToken, FulfillmentResult.FULFILLED)
        }

        cache.addSuccessfullyPostedToken(purchase.purchaseToken)
    }

    override fun findPurchaseInPurchaseHistory(
        appUserID: String,
        productType: RevenueCatProductType,
        sku: String,
        onCompletion: (PurchaseDetails) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        log(LogIntent.DEBUG, RestoreStrings.QUERYING_PURCHASE_WITH_TYPE.format(sku, productType.name))
        queryAllPurchases(
            appUserID,
            onReceivePurchaseHistory = {
                val record: PurchaseHistoryRecordWrapper? = it.firstOrNull { record -> sku == record.sku }
                if (record != null) {
                    onCompletion(record)
                } else {
                    val message = PurchaseStrings.NO_EXISTING_PURCHASE.format(sku)
                    val error = PurchasesError(PurchasesErrorCode.PurchaseInvalidError, message)
                    onError(error)
                }
            },
            onError
        )
    }

    override fun makePurchaseAsync(
        activity: Activity,
        appUserID: String,
        productDetails: ProductDetails,
        replaceSkuInfo: ReplaceSkuInfo?,
        presentedOfferingIdentifier: String?
    ) {
        if (replaceSkuInfo != null) {
            log(LogIntent.AMAZON_WARNING, AmazonStrings.PRODUCT_CHANGES_NOT_SUPPORTED)
            return
        }
        purchaseHandler.purchase(
            appUserID,
            productDetails,
            presentedOfferingIdentifier,
            onSuccess = { receipt, userData ->
                handleReceipt(receipt, userData, productDetails, presentedOfferingIdentifier)
            },
            onError = ::onPurchaseError
        )
    }

    override fun isConnected(): Boolean = connected

    override fun queryPurchases(
        appUserID: String,
        onSuccess: (Map<String, PurchaseDetails>) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        purchaseUpdatesHandler.queryPurchases(
            onSuccess = onSuccess@{ receipts, userData ->
                if (receipts.isEmpty()) {
                    onSuccess(emptyMap())
                    return@onSuccess
                }

                getMissingSkusForReceipts(
                    userData.userId,
                    receipts
                ) { tokensToSkusMap, errors ->
                    logErrorsIfAny(errors)

                    if (tokensToSkusMap.isEmpty()) {
                        val error = PurchasesError(
                            PurchasesErrorCode.InvalidReceiptError,
                            AmazonStrings.ERROR_FETCHING_PURCHASE_HISTORY_ALL_RECEIPTS_INVALID
                        )
                        onError(error)
                        return@getMissingSkusForReceipts
                    }

                    val purchasesByHashedToken = receipts.toHashMapOfPurchases(tokensToSkusMap, userData)

                    onSuccess(purchasesByHashedToken)
                }
            },
            onError
        )
    }

    private fun List<Receipt>.toHashMapOfPurchases(
        tokensToSkusMap: Map<String, String>,
        userData: UserData
    ) = mapNotNull { receipt ->
        val sku = tokensToSkusMap[receipt.receiptId]
        if (sku == null) {
            log(LogIntent.AMAZON_ERROR, AmazonStrings.ERROR_FINDING_RECEIPT_SKU)
            return@mapNotNull null
        }
        val amazonPurchaseWrapper = receipt.toRevenueCatPurchaseDetails(
            sku = sku,
            presentedOfferingIdentifier = null,
            purchaseState = RevenueCatPurchaseState.PURCHASED,
            storeUserID = userData.userId
        )
        val hash = receipt.receiptId.sha1()
        hash to amazonPurchaseWrapper
    }.toMap()

    // AmazonBilling delegates functionality to interfaces that have a common parent interface, it will only
    // compile as long as all of the functions are implemented, otherwise it doesn't know which delegated
    // implementation to take
    override fun onUserDataResponse(response: UserDataResponse) {
        userDataHandler.onUserDataResponse(response)
    }

    override fun onProductDataResponse(response: ProductDataResponse) {
        productDataHandler.onProductDataResponse(response)
    }

    override fun onPurchaseResponse(response: PurchaseResponse) {
        purchaseHandler.onPurchaseResponse(response)
    }

    override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse) {
        purchaseUpdatesHandler.onPurchaseUpdatesResponse(response)
    }

    private fun logErrorsIfAny(errors: Map<String, PurchasesError>) {
        if (errors.isNotEmpty()) {
            val receiptsWithErrors = errors.keys.joinToString("\n")
            log(
                LogIntent.AMAZON_ERROR,
                AmazonStrings.ERROR_FETCHING_RECEIPTS.format(receiptsWithErrors)
            )
        }
    }

    private fun getMissingSkusForReceipts(
        amazonUserID: String,
        receipts: List<Receipt>,
        onCompletion: (tokensToSkusMap: Map<String, String>, errors: Map<String, PurchasesError>) -> Unit
    ) {
        val currentlyCachedTokensToSkus: Map<String, String> = cache.getReceiptSkus()

        val successMap: MutableMap<String, String> = currentlyCachedTokensToSkus.toMutableMap()
        val errorMap: MutableMap<String, PurchasesError> = mutableMapOf()

        val nonSubscriptionReceiptsToSku =
            receipts.filterNot { it.productType == RevenueCatProductType.SUBSCRIPTION }
                .map { it.receiptId to it.sku }

        successMap.putAll(nonSubscriptionReceiptsToSku)

        val subscriptionReceiptsToFetchTermSku: List<Receipt> =
            receipts
                .filter { it.productType == RevenueCatProductType.SUBSCRIPTION }
                .filterNot { currentlyCachedTokensToSkus.containsKey(it.receiptId) }

        if (subscriptionReceiptsToFetchTermSku.isEmpty()) {
            onCompletion(successMap, errorMap)
            return
        }

        var receiptsLeft = subscriptionReceiptsToFetchTermSku.count()
        subscriptionReceiptsToFetchTermSku.forEach { receipt ->
            amazonBackend.getAmazonReceiptData(
                receipt.receiptId,
                amazonUserID,
                onSuccess = { response ->
                    log(LogIntent.DEBUG, AmazonStrings.RECEIPT_DATA_RECEIVED.format(response.toString()))

                    successMap[receipt.receiptId] = response["termSku"] as String

                    receiptsLeft--
                    if (receiptsLeft == 0) {
                        cache.setReceiptSkus(successMap)
                        onCompletion(successMap, errorMap)
                    }
                }, onError = { error ->
                    log(LogIntent.AMAZON_ERROR, AmazonStrings.ERROR_FETCHING_RECEIPT_INFO.format(error))

                    errorMap[receipt.receiptId] = error

                    receiptsLeft--
                    if (receiptsLeft == 0) {
                        onCompletion(successMap, errorMap)
                    }
                }
            )
        }
    }

    private fun handleReceipt(
        receipt: Receipt,
        userData: UserData,
        productDetails: ProductDetails,
        presentedOfferingIdentifier: String?
    ) {
        if (receipt.productType != RevenueCatProductType.SUBSCRIPTION) {
            /**
             * For subscriptions we need to get the termSku of the receipt.
             * We have to hit our backend for that, since the only way to get it is using Amazon RVS.
             * For consumables and entitlements, we don't need to fetch de termSku,
             * since there's no terms and we can just use the sku
             */
            val amazonPurchaseWrapper = receipt.toRevenueCatPurchaseDetails(
                sku = productDetails.sku,
                presentedOfferingIdentifier = presentedOfferingIdentifier,
                purchaseState = RevenueCatPurchaseState.PURCHASED,
                storeUserID = userData.userId
            )
            purchasesUpdatedListener?.onPurchasesUpdated(listOf(amazonPurchaseWrapper))
            return
        }

        amazonBackend.getAmazonReceiptData(
            receipt.receiptId,
            userData.userId,
            onSuccess = { response ->
                val termSku = response["termSku"] as String
                val amazonPurchaseWrapper = receipt.toRevenueCatPurchaseDetails(
                    sku = termSku,
                    presentedOfferingIdentifier = presentedOfferingIdentifier,
                    purchaseState = RevenueCatPurchaseState.PURCHASED,
                    storeUserID = userData.userId
                )
                purchasesUpdatedListener?.onPurchasesUpdated(listOf(amazonPurchaseWrapper))
            },
            onError = ::onPurchaseError
        )
    }

    private fun onPurchaseError(error: PurchasesError) {
        purchasesUpdatedListener?.onPurchasesFailedToUpdate(error)
    }
}
