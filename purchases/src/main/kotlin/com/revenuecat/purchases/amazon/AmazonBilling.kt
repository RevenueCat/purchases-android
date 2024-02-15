package com.revenuecat.purchases.amazon

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.amazon.device.iap.model.FulfillmentResult
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.ProductType
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.amazon.device.iap.model.Receipt
import com.amazon.device.iap.model.UserData
import com.amazon.device.iap.model.UserDataResponse
import com.revenuecat.purchases.PostReceiptInitiationSource
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesStateProvider
import com.revenuecat.purchases.amazon.handler.ProductDataHandler
import com.revenuecat.purchases.amazon.handler.PurchaseHandler
import com.revenuecat.purchases.amazon.handler.PurchaseUpdatesHandler
import com.revenuecat.purchases.amazon.handler.UserDataHandler
import com.revenuecat.purchases.amazon.listener.ProductDataResponseListener
import com.revenuecat.purchases.amazon.listener.PurchaseResponseListener
import com.revenuecat.purchases.amazon.listener.PurchaseUpdatesResponseListener
import com.revenuecat.purchases.amazon.listener.UserDataResponseListener
import com.revenuecat.purchases.common.BackendHelper
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.ReplaceProductInfo
import com.revenuecat.purchases.common.StoreProductsCallback
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.models.InAppMessageType
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.strings.BillingStrings
import com.revenuecat.purchases.strings.PurchaseStrings
import com.revenuecat.purchases.strings.RestoreStrings
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.ConcurrentLinkedQueue
import com.revenuecat.purchases.ProductType as RevenueCatProductType

private const val TERM_SKU_JSON_KEY = "termSku"

@SuppressWarnings("LongParameterList", "TooManyFunctions")
internal class AmazonBilling(
    private val applicationContext: Context,
    private val amazonBackend: AmazonBackend,
    private val cache: AmazonCache,
    private val observerMode: Boolean,
    private val mainHandler: Handler,
    stateProvider: PurchasesStateProvider,
    private val purchasingServiceProvider: PurchasingServiceProvider = DefaultPurchasingServiceProvider(),
    private val productDataHandler: ProductDataResponseListener =
        ProductDataHandler(purchasingServiceProvider, mainHandler),
    private val purchaseHandler: PurchaseResponseListener =
        PurchaseHandler(purchasingServiceProvider, applicationContext),
    private val purchaseUpdatesHandler: PurchaseUpdatesResponseListener = PurchaseUpdatesHandler(
        purchasingServiceProvider,
    ),
    private val userDataHandler: UserDataResponseListener = UserDataHandler(purchasingServiceProvider, mainHandler),
    private val dateProvider: DateProvider = DefaultDateProvider(),
) : BillingAbstract(stateProvider),
    ProductDataResponseListener by productDataHandler,
    PurchaseResponseListener by purchaseHandler,
    PurchaseUpdatesResponseListener by purchaseUpdatesHandler,
    UserDataResponseListener by userDataHandler {

    // Used for constructing the class via Reflection. Make sure to update any call if updating this constructor
    @Suppress("unused")
    constructor(
        applicationContext: Context,
        cache: DeviceCache,
        observerMode: Boolean,
        mainHandler: Handler,
        backendHelper: BackendHelper,
        stateProvider: PurchasesStateProvider,
    ) : this(
        applicationContext,
        AmazonBackend(backendHelper),
        AmazonCache(cache),
        observerMode,
        mainHandler,
        stateProvider,
    )

    private var connected = false

    override fun startConnection() {
        if (checkObserverMode()) return

        purchasingServiceProvider.registerListener(applicationContext, this)
        connected = true
        stateListener?.onConnected()
        executePendingRequests()
    }

    override fun startConnectionOnMainThread(delayMilliseconds: Long) {
        // Start connection has to be called on onCreate, otherwise Amazon fails to detect the foregrounded Activity
        // Be careful with doing mainHandler.post since that will not guarantee that it's called in onCreate
        // runOnUIThread checks if the function is called in the UI thread and doesn't do post, so we are good since
        // startConnectionOnMainThread is called on the main thread
        runOnUIThread {
            startConnection()
        }
    }

    @SuppressWarnings("EmptyFunctionBlock")
    override fun endConnection() { }

    override fun queryAllPurchases(
        appUserID: String,
        onReceivePurchaseHistory: (List<StoreTransaction>) -> Unit,
        onReceivePurchaseHistoryError: PurchasesErrorCallback,
    ) {
        queryPurchases(
            filterOnlyActivePurchases = false,
            onSuccess = {
                onReceivePurchaseHistory(it.values.toList())
            },
            onReceivePurchaseHistoryError,
        )
    }

    override fun normalizePurchaseData(
        productID: String,
        purchaseToken: String,
        storeUserID: String,
        onSuccess: (correctProductID: String) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        val currentlyCachedTokensToSkus = cache.getReceiptSkus()

        currentlyCachedTokensToSkus[purchaseToken]?.let { sku ->
            onSuccess(sku)
            return
        }

        amazonBackend.getAmazonReceiptData(
            purchaseToken,
            storeUserID,
            onSuccess = { response ->
                log(LogIntent.DEBUG, AmazonStrings.RECEIPT_DATA_RECEIVED.format(response.toString()))

                val termSku = getTermSkuFromJSON(response)
                if (termSku == null) {
                    onError(missingTermSkuError(response))
                    return@getAmazonReceiptData
                }
                cache.cacheSkusByToken(mapOf(purchaseToken to termSku))
                onSuccess(termSku)
            },
            onError = { error ->
                onError(errorGettingReceiptInfo(error))
            },
        )
    }

    // region Product Data

    override fun queryProductDetailsAsync(
        productType: RevenueCatProductType,
        productIds: Set<String>,
        onReceive: StoreProductsCallback,
        onError: PurchasesErrorCallback,
    ) {
        if (checkObserverMode()) return
        executeRequestOnUIThread { connectionError ->
            if (connectionError == null) {
                userDataHandler.getUserData(
                    onSuccess = { userData ->
                        productDataHandler.getProductData(productIds, userData.marketplace, onReceive, onError)
                    },
                    onError,
                )
            } else {
                onError(connectionError)
            }
        }
    }

    // endregion

    override fun consumeAndSave(
        shouldTryToConsume: Boolean,
        purchase: StoreTransaction,
        initiationSource: PostReceiptInitiationSource,
    ) {
        if (checkObserverMode() || purchase.type == RevenueCatProductType.UNKNOWN) return

        // PENDING purchases should not be fulfilled
        if (purchase.purchaseState == PurchaseState.PENDING) return

        if (shouldTryToConsume) {
            executeRequestOnUIThread { connectionError ->
                if (connectionError == null) {
                    purchasingServiceProvider.notifyFulfillment(purchase.purchaseToken, FulfillmentResult.FULFILLED)
                } else {
                    errorLog(connectionError)
                }
            }
        }

        cache.addSuccessfullyPostedToken(purchase.purchaseToken)
    }

    override fun findPurchaseInPurchaseHistory(
        appUserID: String,
        productType: RevenueCatProductType,
        productId: String,
        onCompletion: (StoreTransaction) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        log(LogIntent.DEBUG, RestoreStrings.QUERYING_PURCHASE_WITH_TYPE.format(productId, productType.name))
        queryAllPurchases(
            appUserID,
            onReceivePurchaseHistory = {
                // We get productIds[0] because the list is guaranteed to have just one item in Amazon's case.
                val record: StoreTransaction? = it.firstOrNull { record -> productId == record.productIds[0] }
                if (record != null) {
                    onCompletion(record)
                } else {
                    val message = PurchaseStrings.NO_EXISTING_PURCHASE.format(productId)
                    val error = PurchasesError(PurchasesErrorCode.PurchaseInvalidError, message)
                    onError(error)
                }
            },
            onError,
        )
    }

    @Suppress("ReturnCount")
    override fun makePurchaseAsync(
        activity: Activity,
        appUserID: String,
        purchasingData: PurchasingData,
        replaceProductInfo: ReplaceProductInfo?,
        presentedOfferingContext: PresentedOfferingContext?,
        isPersonalizedPrice: Boolean?,
    ) {
        val amazonPurchaseInfo = purchasingData as? AmazonPurchasingData.Product
        if (amazonPurchaseInfo == null) {
            val error = PurchasesError(
                PurchasesErrorCode.UnknownError,
                PurchaseStrings.INVALID_PURCHASE_TYPE.format(
                    "Amazon",
                    "AmazonPurchaseInfo",
                ),
            )
            errorLog(error)
            purchasesUpdatedListener?.onPurchasesFailedToUpdate(error)
            return
        }
        val storeProduct = amazonPurchaseInfo.storeProduct

        if (checkObserverMode()) return

        if (replaceProductInfo != null) {
            log(LogIntent.AMAZON_WARNING, AmazonStrings.PRODUCT_CHANGES_NOT_SUPPORTED)
            return
        }
        executeRequestOnUIThread { connectionError ->
            if (connectionError == null) {
                purchaseHandler.purchase(
                    mainHandler,
                    activity,
                    appUserID,
                    storeProduct,
                    presentedOfferingContext,
                    onSuccess = { receipt, userData ->
                        handleReceipt(receipt, userData, storeProduct, presentedOfferingContext)
                    },
                    onError = {
                        onPurchaseError(it)
                    },
                )
            } else {
                onPurchaseError(connectionError)
            }
        }
    }

    override fun isConnected(): Boolean = connected

    override fun queryPurchases(
        appUserID: String,
        onSuccess: (Map<String, StoreTransaction>) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        if (checkObserverMode()) return
        queryPurchases(
            filterOnlyActivePurchases = true,
            onSuccess,
            onError,
        )
    }

    override fun showInAppMessagesIfNeeded(
        activity: Activity,
        inAppMessageTypes: List<InAppMessageType>,
        subscriptionStatusChange: () -> Unit,
    ) {
        // No-op: Amazon doesn't have in-app messages
    }

    override fun getStorefront(
        onSuccess: (String) -> Unit,
        onError: PurchasesErrorCallback,
    ) {
        executeRequestOnUIThread { connectionError ->
            if (connectionError == null) {
                userDataHandler.getUserData(
                    onSuccess = { userData ->
                        val marketplace = userData.marketplace ?: run {
                            onError(
                                PurchasesError(
                                    PurchasesErrorCode.StoreProblemError,
                                    AmazonStrings.ERROR_USER_DATA_MARKETPLACE_NULL_STORE_PROBLEM,
                                ),
                            )
                            return@getUserData
                        }
                        onSuccess(marketplace)
                    },
                    onError = { error ->
                        errorLog(BillingStrings.BILLING_AMAZON_ERROR_STOREFRONT.format(error))
                        onError(error)
                    },
                )
            } else {
                errorLog(BillingStrings.BILLING_CONNECTION_ERROR_STORE_COUNTRY.format(connectionError))
                onError(connectionError)
            }
        }
    }

    private fun List<Receipt>.toMapOfReceiptHashesToRestoredPurchases(
        tokensToSkusMap: Map<String, String>,
        userData: UserData,
    ) = mapNotNull { receipt ->
        val sku = tokensToSkusMap[receipt.receiptId]
        if (sku == null) {
            log(LogIntent.AMAZON_ERROR, AmazonStrings.ERROR_FINDING_RECEIPT_SKU)
            return@mapNotNull null
        }
        val amazonPurchaseWrapper = receipt.toStoreTransaction(
            productId = sku,
            presentedOfferingContext = null,
            purchaseState = PurchaseState.UNSPECIFIED_STATE,
            userData,
        )
        val hash = receipt.receiptId.sha1()
        hash to amazonPurchaseWrapper
    }.toMap()

    // AmazonBilling delegates functionality to interfaces that have a common parent interface, it will only
    // compile as long as all of the functions are implemented, otherwise it doesn't know which delegated
    // implementation to take
    override fun onUserDataResponse(response: UserDataResponse) {
        if (checkObserverMode()) return
        userDataHandler.onUserDataResponse(response)
    }

    override fun onProductDataResponse(response: ProductDataResponse) {
        if (checkObserverMode()) return
        productDataHandler.onProductDataResponse(response)
    }

    override fun onPurchaseResponse(response: PurchaseResponse) {
        if (checkObserverMode()) return
        purchaseHandler.onPurchaseResponse(response)
    }

    override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse) {
        if (checkObserverMode()) return
        purchaseUpdatesHandler.onPurchaseUpdatesResponse(response)
    }

    @SuppressWarnings("SwallowedException")
    private fun getTermSkuFromJSON(response: JSONObject): String? {
        return try {
            response.getString(TERM_SKU_JSON_KEY)
        } catch (exception: JSONException) {
            null
        }
    }

    private fun logErrorsIfAny(errors: Map<String, PurchasesError>) {
        if (errors.isNotEmpty()) {
            val receiptsWithErrors = errors.keys.joinToString("\n")
            log(
                LogIntent.AMAZON_ERROR,
                AmazonStrings.ERROR_FETCHING_RECEIPTS.format(receiptsWithErrors),
            )
        }
    }

    private fun queryPurchases(
        filterOnlyActivePurchases: Boolean,
        onSuccess: (Map<String, StoreTransaction>) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        executeRequestOnUIThread { connectionError ->
            if (connectionError == null) {
                purchaseUpdatesHandler.queryPurchases(
                    onSuccess = onSuccess@{ receipts, userData ->
                        val filteredReceipts = if (filterOnlyActivePurchases) {
                            // This filters out expired receipts according to the current date.
                            // Note that this is not calculating the expiration date of the purchase,
                            // where we would use the backend requestDate as part of the calculation.
                            receipts.filter { it.cancelDate == null || it.cancelDate > dateProvider.now }
                        } else {
                            receipts
                        }
                        if (filteredReceipts.isEmpty()) {
                            onSuccess(emptyMap())
                            return@onSuccess
                        }

                        getMissingSkusForReceipts(
                            userData.userId,
                            filteredReceipts,
                        ) { tokensToSkusMap, errors ->
                            logErrorsIfAny(errors)

                            if (tokensToSkusMap.isEmpty()) {
                                val error = PurchasesError(
                                    PurchasesErrorCode.InvalidReceiptError,
                                    AmazonStrings.ERROR_FETCHING_PURCHASE_HISTORY_ALL_RECEIPTS_INVALID,
                                )
                                onError(error)
                                return@getMissingSkusForReceipts
                            }

                            val purchasesByHashedToken =
                                filteredReceipts.toMapOfReceiptHashesToRestoredPurchases(tokensToSkusMap, userData)

                            onSuccess(purchasesByHashedToken)
                        }
                    },
                    onError,
                )
            } else {
                onError(connectionError)
            }
        }
    }

    private fun getMissingSkusForReceipts(
        amazonUserID: String,
        receipts: List<Receipt>,
        onCompletion: (tokensToSkusMap: Map<String, String>, errors: Map<String, PurchasesError>) -> Unit,
    ) {
        val currentlyCachedTokensToSkus: Map<String, String> = cache.getReceiptSkus()

        val successMap: MutableMap<String, String> = currentlyCachedTokensToSkus.toMutableMap()
        val errorMap: MutableMap<String, PurchasesError> = mutableMapOf()

        val nonSubscriptionReceiptsToSku =
            receipts.filterNot { it.productType == ProductType.SUBSCRIPTION }
                .map { it.receiptId to it.sku }

        successMap.putAll(nonSubscriptionReceiptsToSku)

        val subscriptionReceiptsToFetchTermSku: List<Receipt> =
            receipts
                .filter { it.productType == ProductType.SUBSCRIPTION }
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

                    successMap[receipt.receiptId] = response[TERM_SKU_JSON_KEY] as String

                    receiptsLeft--
                    if (receiptsLeft == 0) {
                        cache.cacheSkusByToken(successMap)
                        onCompletion(successMap, errorMap)
                    }
                },
                onError = { error ->
                    log(LogIntent.AMAZON_ERROR, AmazonStrings.ERROR_FETCHING_RECEIPT_INFO.format(error))

                    errorMap[receipt.receiptId] = error

                    receiptsLeft--
                    if (receiptsLeft == 0) {
                        onCompletion(successMap, errorMap)
                    }
                },
            )
        }
    }

    private fun handleReceipt(
        receipt: Receipt,
        userData: UserData,
        storeProduct: StoreProduct,
        presentedOfferingContext: PresentedOfferingContext?,
    ) {
        if (receipt.productType != ProductType.SUBSCRIPTION) {
            /**
             * For subscriptions we need to get the termSku of the receipt.
             * We have to hit our backend for that, since the only way to get it is using Amazon RVS.
             * For consumables and entitlements, we don't need to fetch de termSku,
             * since there's no terms and we can just use the sku
             */
            val amazonPurchaseWrapper = receipt.toStoreTransaction(
                productId = storeProduct.id,
                presentedOfferingContext = presentedOfferingContext,
                purchaseState = PurchaseState.PURCHASED,
                userData,
            )
            purchasesUpdatedListener?.onPurchasesUpdated(listOf(amazonPurchaseWrapper))
            return
        }

        amazonBackend.getAmazonReceiptData(
            receipt.receiptId,
            userData.userId,
            onSuccess = { response ->
                val termSku = response[TERM_SKU_JSON_KEY] as String
                val amazonPurchaseWrapper = receipt.toStoreTransaction(
                    productId = termSku,
                    presentedOfferingContext = presentedOfferingContext,
                    purchaseState = PurchaseState.PURCHASED,
                    userData,
                )
                purchasesUpdatedListener?.onPurchasesUpdated(listOf(amazonPurchaseWrapper))
            },
            onError = ::onPurchaseError,
        )
    }

    private fun onPurchaseError(error: PurchasesError) {
        purchasesUpdatedListener?.onPurchasesFailedToUpdate(error)
    }

    private fun checkObserverMode(): Boolean {
        return if (observerMode) {
            log(LogIntent.AMAZON_WARNING, AmazonStrings.WARNING_AMAZON_OBSERVER_MODE)
            true
        } else {
            false
        }
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
        synchronized(this@AmazonBilling) {
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
