//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.google

import android.app.Activity
import android.content.Context
import android.os.Handler
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PRIVATE
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.PurchaseHistoryResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.ReplaceSkuInfo
import com.revenuecat.purchases.common.StoreProductsCallback
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.firstSku
import com.revenuecat.purchases.common.listOfSkus
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.common.sha256
import com.revenuecat.purchases.common.toHumanReadableDescription
import com.revenuecat.purchases.models.GooglePurchaseOption
import com.revenuecat.purchases.models.GoogleStoreProduct
import com.revenuecat.purchases.models.PurchaseOption
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.models.googleProduct
import com.revenuecat.purchases.strings.BillingStrings
import com.revenuecat.purchases.strings.OfferingStrings
import com.revenuecat.purchases.strings.PurchaseStrings
import com.revenuecat.purchases.strings.RestoreStrings
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.min

private const val RECONNECT_TIMER_START_MILLISECONDS = 1L * 1000L
private const val RECONNECT_TIMER_MAX_TIME_MILLISECONDS = 1000L * 60L * 15L // 15 minutes

@Suppress("LargeClass")
class BillingWrapper(
    private val clientFactory: ClientFactory,
    private val mainHandler: Handler,
    private val deviceCache: DeviceCache
) : BillingAbstract(), PurchasesUpdatedListener, BillingClientStateListener {

    @get:Synchronized
    @set:Synchronized
    @Volatile
    var billingClient: BillingClient? = null

    private val productTypes = mutableMapOf<String, ProductType>()
    private val presentedOfferingsByProductIdentifier = mutableMapOf<String, String?>()

    private val serviceRequests =
        ConcurrentLinkedQueue<(connectionError: PurchasesError?) -> Unit>()

    // how long before the data source tries to reconnect to Google play
    private var reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS

    class ClientFactory(private val context: Context) {
        @UiThread
        fun buildClient(listener: com.android.billingclient.api.PurchasesUpdatedListener): BillingClient {
            return BillingClient.newBuilder(context).enablePendingPurchases().setListener(listener)
                .build()
        }
    }

    private fun executePendingRequests() {
        synchronized(this@BillingWrapper) {
            while (billingClient?.isReady == true && !serviceRequests.isEmpty()) {
                serviceRequests.remove().let { mainHandler.post { it(null) } }
            }
        }
    }

    override fun startConnectionOnMainThread(delayMilliseconds: Long) {
        mainHandler.postDelayed(
            { startConnection() },
            delayMilliseconds
        )
    }

    override fun startConnection() {
        synchronized(this@BillingWrapper) {
            if (billingClient == null) {
                billingClient = clientFactory.buildClient(this)
            }

            billingClient?.let {
                if (!it.isReady) {
                    log(LogIntent.DEBUG, BillingStrings.BILLING_CLIENT_STARTING.format(it))
                    it.startConnection(this)
                }
            }
        }
    }

    override fun endConnection() {
        mainHandler.post {
            synchronized(this@BillingWrapper) {
                billingClient?.let {
                    log(LogIntent.DEBUG, BillingStrings.BILLING_CLIENT_ENDING.format(it))
                    it.endConnection()
                }
                billingClient = null
            }
        }
    }

    @Synchronized
    private fun executeRequestOnUIThread(request: (PurchasesError?) -> Unit) {
        if (purchasesUpdatedListener != null) {
            serviceRequests.add(request)
            if (billingClient?.isReady == false) {
                startConnectionOnMainThread()
            } else {
                executePendingRequests()
            }
        }
    }

    override fun queryProductDetailsAsync(
        productType: ProductType,
        productIds: Set<String>,
        onReceive: StoreProductsCallback,
        onError: PurchasesErrorCallback
    ) {
        val nonEmptyProductIds = productIds.filter { it.isNotEmpty() }.toSet()

        if (nonEmptyProductIds.isEmpty()) {
            log(LogIntent.DEBUG, OfferingStrings.EMPTY_SKU_LIST)
            onReceive(emptyList())
            return
        }

        log(LogIntent.DEBUG, OfferingStrings.FETCHING_PRODUCTS.format(productIds.joinToString()))
        executeRequestOnUIThread { connectionError ->
            if (connectionError == null) {
                val googleType = productType.toGoogleProductType() ?: BillingClient.ProductType.INAPP
                val params = googleType.buildQueryProductDetailsParams(nonEmptyProductIds)

                withConnectedClient {
                    queryProductDetailsAsyncEnsuringOneResponse(params) { billingResult, productDetailsList ->
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            log(
                                LogIntent.DEBUG, OfferingStrings.FETCHING_PRODUCTS_FINISHED
                                    .format(productIds.joinToString())
                            )
                            log(
                                LogIntent.PURCHASE, OfferingStrings.RETRIEVED_PRODUCTS
                                    .format(productDetailsList.joinToString { it.toString() })
                            )
                            productDetailsList.takeUnless { it.isEmpty() }?.forEach {
                                log(LogIntent.PURCHASE, OfferingStrings.LIST_PRODUCTS.format(it.productId, it))
                            }

                            val storeProducts = productDetailsList.toStoreProducts()
                            onReceive(storeProducts)
                        } else {
                            log(
                                LogIntent.GOOGLE_ERROR, OfferingStrings.FETCHING_PRODUCTS_ERROR
                                    .format(billingResult.toHumanReadableDescription())
                            )
                            onError(
                                billingResult.responseCode.billingResponseToPurchasesError(
                                    "Error when fetching products. ${billingResult.toHumanReadableDescription()}"
                                ).also { errorLog(it) }
                            )
                        }
                    }
                }
            } else {
                onError(connectionError)
            }
        }
    }

    override fun makePurchaseAsync(
        activity: Activity,
        appUserID: String,
        storeProduct: StoreProduct,
        purchaseOption: PurchaseOption,
        replaceSkuInfo: ReplaceSkuInfo?,
        presentedOfferingIdentifier: String?
    ) {
        val googleProduct = storeProduct.googleProduct

        if (googleProduct == null) {
            val error = PurchasesError(
                PurchasesErrorCode.UnknownError,
                PurchaseStrings.INVALID_STORE_PRODUCT_TYPE.format(
                    "Play",
                    "GoogleStoreProduct"
                )
            )
            errorLog(error)
            purchasesUpdatedListener?.onPurchasesFailedToUpdate(error)
            return
        }

        val googlePurchaseOption = purchaseOption as? GooglePurchaseOption
        if (googlePurchaseOption == null) {
            val error = PurchasesError(
                PurchasesErrorCode.UnknownError,
                PurchaseStrings.INVALID_PURCHASE_OPTION_TYPE.format(
                    "Play",
                    "GooglePurchaseOption"
                )
            )
            errorLog(error)
            purchasesUpdatedListener?.onPurchasesFailedToUpdate(error)
            return
        }

        if (replaceSkuInfo != null) {
            log(
                LogIntent.PURCHASE, PurchaseStrings.UPGRADING_SKU
                    .format(replaceSkuInfo.oldPurchase.skus[0], storeProduct.sku)
            )
        } else {
            log(LogIntent.PURCHASE, PurchaseStrings.PURCHASING_PRODUCT.format(storeProduct.sku))
        }

        synchronized(this@BillingWrapper) {
            productTypes[storeProduct.sku] = storeProduct.type
            presentedOfferingsByProductIdentifier[storeProduct.sku] = presentedOfferingIdentifier
        }
        executeRequestOnUIThread {
            val params = buildPurchaseParams(
                googleProduct,
                purchaseOption,
                replaceSkuInfo,
                appUserID
            )
            launchBillingFlow(activity, params)
        }
    }

    @UiThread
    private fun launchBillingFlow(
        activity: Activity,
        params: BillingFlowParams
    ) {
        if (activity.intent == null) {
            log(LogIntent.WARNING, BillingStrings.NULL_ACTIVITY_INTENT)
        }
        withConnectedClient {
            launchBillingFlow(activity, params)
                .takeIf { billingResult -> billingResult.responseCode != BillingClient.BillingResponseCode.OK }
                ?.let { billingResult ->
                    log(
                        LogIntent.GOOGLE_ERROR, BillingStrings.BILLING_INTENT_FAILED
                            .format(billingResult.toHumanReadableDescription())
                    )
                }
        }
    }

    fun queryPurchaseHistoryAsync(
        @BillingClient.ProductType productType: String,
        onReceivePurchaseHistory: (List<PurchaseHistoryRecord>) -> Unit,
        onReceivePurchaseHistoryError: (PurchasesError) -> Unit
    ) {
        log(LogIntent.DEBUG, RestoreStrings.QUERYING_PURCHASE_HISTORY.format(productType))
        executeRequestOnUIThread { connectionError ->
            if (connectionError == null) {
                withConnectedClient {
                    queryPurchaseHistoryAsyncEnsuringOneResponse(productType) {
                            billingResult, purchaseHistoryRecordList ->

                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            purchaseHistoryRecordList.takeUnless { it.isNullOrEmpty() }?.forEach {
                                log(
                                    LogIntent.RC_PURCHASE_SUCCESS, RestoreStrings.PURCHASE_HISTORY_RETRIEVED
                                        .format(it.toHumanReadableDescription())
                                )
                            } ?: log(LogIntent.DEBUG, RestoreStrings.PURCHASE_HISTORY_EMPTY)
                            onReceivePurchaseHistory(purchaseHistoryRecordList ?: emptyList())
                        } else {
                            onReceivePurchaseHistoryError(
                                billingResult.responseCode.billingResponseToPurchasesError(
                                    "Error receiving purchase history. ${billingResult.toHumanReadableDescription()}"
                                ).also { errorLog(it) }
                            )
                        }
                    }
                }
            } else {
                onReceivePurchaseHistoryError(connectionError)
            }
        }
    }

    override fun queryAllPurchases(
        appUserID: String,
        onReceivePurchaseHistory: (List<StoreTransaction>) -> Unit,
        onReceivePurchaseHistoryError: (PurchasesError) -> Unit
    ) {
        queryPurchaseHistoryAsync(
            BillingClient.ProductType.SUBS,
            { subsPurchasesList ->
                queryPurchaseHistoryAsync(
                    BillingClient.ProductType.INAPP,
                    { inAppPurchasesList ->
                        onReceivePurchaseHistory(
                            subsPurchasesList.map {
                                it.toStoreTransaction(ProductType.SUBS)
                            } + inAppPurchasesList.map {
                                it.toStoreTransaction(ProductType.INAPP)
                            }
                        )
                    },
                    onReceivePurchaseHistoryError
                )
            },
            onReceivePurchaseHistoryError
        )
    }

    override fun consumeAndSave(
        shouldTryToConsume: Boolean,
        purchase: StoreTransaction
    ) {
        if (purchase.type == ProductType.UNKNOWN) {
            // Would only get here if the purchase was triggered from outside of the app and there was
            // an issue getting the purchase type
            return
        }
        if (purchase.purchaseState == PurchaseState.PENDING) {
            // PENDING purchases should not be fulfilled
            return
        }

        val originalGooglePurchase = purchase.originalGooglePurchase
        val alreadyAcknowledged = originalGooglePurchase?.isAcknowledged ?: false
        if (shouldTryToConsume && purchase.type == ProductType.INAPP) {
            consumePurchase(purchase.purchaseToken) { billingResult, purchaseToken ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    deviceCache.addSuccessfullyPostedToken(purchaseToken)
                } else {
                    log(
                        LogIntent.GOOGLE_ERROR, PurchaseStrings.CONSUMING_PURCHASE_ERROR
                            .format(billingResult.toHumanReadableDescription())
                    )
                }
            }
        } else if (shouldTryToConsume && !alreadyAcknowledged) {
            acknowledge(purchase.purchaseToken) { billingResult, purchaseToken ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    deviceCache.addSuccessfullyPostedToken(purchaseToken)
                } else {
                    log(
                        LogIntent.GOOGLE_ERROR, PurchaseStrings.ACKNOWLEDGING_PURCHASE_ERROR
                            .format(billingResult.toHumanReadableDescription())
                    )
                }
            }
        } else {
            deviceCache.addSuccessfullyPostedToken(purchase.purchaseToken)
        }
    }

    internal fun consumePurchase(
        token: String,
        onConsumed: (billingResult: BillingResult, purchaseToken: String) -> Unit
    ) {
        log(LogIntent.PURCHASE, PurchaseStrings.CONSUMING_PURCHASE.format(token))
        executeRequestOnUIThread { connectionError ->
            if (connectionError == null) {
                withConnectedClient {
                    consumeAsync(
                        ConsumeParams.newBuilder().setPurchaseToken(token).build(), onConsumed
                    )
                }
            }
        }
    }

    internal fun acknowledge(
        token: String,
        onAcknowledged: (billingResult: BillingResult, purchaseToken: String) -> Unit
    ) {
        log(LogIntent.PURCHASE, PurchaseStrings.ACKNOWLEDGING_PURCHASE.format(token))
        executeRequestOnUIThread { connectionError ->
            if (connectionError == null) {
                withConnectedClient {
                    acknowledgePurchase(
                        AcknowledgePurchaseParams.newBuilder().setPurchaseToken(token).build()
                    ) { billingResult ->
                        onAcknowledged(billingResult, token)
                    }
                }
            }
        }
    }

    @Suppress("ReturnCount")
    override fun queryPurchases(
        appUserID: String,
        onSuccess: (Map<String, StoreTransaction>) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        withConnectedClient {
            log(LogIntent.DEBUG, RestoreStrings.QUERYING_PURCHASE)

            val querySubsPurchasesParams = BillingClient.ProductType.SUBS.buildQueryPurchasesParams()
            if (querySubsPurchasesParams == null) {
                onError(
                    PurchasesError(
                        PurchasesErrorCode.PurchaseInvalidError,
                        PurchaseStrings.INVALID_PRODUCT_TYPE.format("queryPurchases")
                    )
                )
                return@withConnectedClient
            }
            this.queryPurchasesAsync(querySubsPurchasesParams) querySubPurchasesAsync@{
                    activeSubsResult, activeSubsPurchases ->

                if (!activeSubsResult.isSuccessful()) {
                    val purchasesError = activeSubsResult.responseCode.billingResponseToPurchasesError(
                        RestoreStrings.QUERYING_SUBS_ERROR.format(activeSubsResult.toHumanReadableDescription())
                    )
                    onError(purchasesError)
                    return@querySubPurchasesAsync
                }

                val mapOfActiveSubscriptions =
                    activeSubsPurchases.toMapOfGooglePurchaseWrapper(BillingClient.ProductType.SUBS)

                val queryInAppsPurchasesParams = BillingClient.ProductType.INAPP.buildQueryPurchasesParams()
                if (queryInAppsPurchasesParams == null) {
                    onError(
                        PurchasesError(
                            PurchasesErrorCode.PurchaseInvalidError,
                            PurchaseStrings.INVALID_PRODUCT_TYPE.format("queryPurchases")
                        )
                    )
                    return@querySubPurchasesAsync
                }

                this.queryPurchasesAsync(queryInAppsPurchasesParams) queryInAppsPurchasesAsync@{
                        unconsumedInAppsResult, unconsumedInAppsPurchases ->

                    if (!unconsumedInAppsResult.isSuccessful()) {
                        val purchasesError =
                            unconsumedInAppsResult.responseCode.billingResponseToPurchasesError(
                                RestoreStrings.QUERYING_INAPP_ERROR.format(
                                    unconsumedInAppsResult.toHumanReadableDescription()
                                )
                            )
                        onError(purchasesError)
                        return@queryInAppsPurchasesAsync
                    }
                    val mapOfUnconsumedInApps =
                        unconsumedInAppsPurchases.toMapOfGooglePurchaseWrapper(BillingClient.ProductType.INAPP)
                    onSuccess(mapOfActiveSubscriptions + mapOfUnconsumedInApps)
                }
            }
        }
    }

    private fun List<Purchase>.toMapOfGooglePurchaseWrapper(
        @BillingClient.ProductType productType: String
    ): Map<String, StoreTransaction> {
        return this.associate { purchase ->
            val hash = purchase.purchaseToken.sha1()
            hash to purchase.toStoreTransaction(
                productType.toRevenueCatProductType(),
                presentedOfferingIdentifier = null
            )
        }
    }

    override fun findPurchaseInPurchaseHistory(
        appUserID: String,
        productType: ProductType,
        sku: String,
        onCompletion: (StoreTransaction) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        withConnectedClient {
            log(LogIntent.DEBUG, RestoreStrings.QUERYING_PURCHASE_WITH_TYPE.format(sku, productType.name))
            productType.toGoogleProductType()?.let { googleProductType ->
                queryPurchaseHistoryAsyncEnsuringOneResponse(googleProductType) { result, purchasesList ->
                    if (result.isSuccessful()) {
                        val purchaseHistoryRecordWrapper =
                            purchasesList?.firstOrNull { it.listOfSkus.contains(sku) }
                                ?.toStoreTransaction(productType)

                        if (purchaseHistoryRecordWrapper != null) {
                            onCompletion(purchaseHistoryRecordWrapper)
                        } else {
                            val message = PurchaseStrings.NO_EXISTING_PURCHASE.format(sku)
                            val error = PurchasesError(PurchasesErrorCode.PurchaseInvalidError, message)
                            onError(error)
                        }
                    } else {
                        val underlyingErrorMessage = PurchaseStrings.ERROR_FINDING_PURCHASE.format(sku)
                        val error =
                            result.responseCode.billingResponseToPurchasesError(underlyingErrorMessage)
                        onError(error)
                    }
                }
            } ?: onError(
                PurchasesError(PurchasesErrorCode.PurchaseInvalidError, PurchaseStrings.NOT_RECOGNIZED_PRODUCT_TYPE)
            )
        }
    }

    @VisibleForTesting(otherwise = PRIVATE)
    @Suppress("ReturnCount")
    internal fun getPurchaseType(purchaseToken: String, listener: (ProductType) -> Unit) {
        billingClient?.let { client ->

            val querySubsPurchasesParams = BillingClient.ProductType.SUBS.buildQueryPurchasesParams()
            if (querySubsPurchasesParams == null) {
                errorLog(PurchaseStrings.INVALID_PRODUCT_TYPE.format("getPurchaseType"))
                listener(ProductType.UNKNOWN)
                return
            }

            client.queryPurchasesAsync(querySubsPurchasesParams) querySubPurchasesAsync@{
                    querySubsResult, subsPurchasesList ->

                val subsResponseOK = querySubsResult.responseCode == BillingClient.BillingResponseCode.OK
                val subFound = subsPurchasesList.any { it.purchaseToken == purchaseToken }
                if (subsResponseOK && subFound) {
                    listener(ProductType.SUBS)
                    return@querySubPurchasesAsync
                }

                val queryInAppsPurchasesParams = BillingClient.ProductType.INAPP.buildQueryPurchasesParams()
                if (queryInAppsPurchasesParams == null) {
                    errorLog(PurchaseStrings.INVALID_PRODUCT_TYPE.format("getPurchaseType"))
                    listener(ProductType.UNKNOWN)
                    return@querySubPurchasesAsync
                }
                client.queryPurchasesAsync(queryInAppsPurchasesParams) queryInAppPurchasesAsync@{
                        queryInAppsResult, inAppPurchasesList ->

                    val inAppsResponseIsOK = queryInAppsResult.responseCode == BillingClient.BillingResponseCode.OK
                    val inAppFound = inAppPurchasesList.any { it.purchaseToken == purchaseToken }
                    if (inAppsResponseIsOK && inAppFound) {
                        listener(ProductType.INAPP)
                        return@queryInAppPurchasesAsync
                    }
                    listener(ProductType.UNKNOWN)
                    return@queryInAppPurchasesAsync
                }
            }
        } ?: listener(ProductType.UNKNOWN)
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: List<Purchase>?
    ) {
        val notNullPurchasesList = purchases ?: emptyList()
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && notNullPurchasesList.isNotEmpty()) {
            val storeTransactions = mutableListOf<StoreTransaction>()

            notNullPurchasesList.forEach { purchase ->
                getStoreTransaction(purchase) { storeTxn ->
                    storeTransactions.add(storeTxn)
                    if (storeTransactions.size == notNullPurchasesList.size) {
                        purchasesUpdatedListener?.onPurchasesUpdated(storeTransactions)
                    }
                }
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            // When doing a DEFERRED downgrade, the result is OK, but the list of purchases is null
            purchasesUpdatedListener?.onPurchasesUpdated(emptyList())
        } else {
            log(LogIntent.GOOGLE_ERROR, BillingStrings.BILLING_WRAPPER_PURCHASES_ERROR
                .format(billingResult.toHumanReadableDescription()) +
                "${
                    notNullPurchasesList.takeUnless { it.isEmpty() }?.let { purchase ->
                        "Purchases:" + purchase.joinToString(
                            ", ",
                            transform = { it.toHumanReadableDescription() }
                        )
                    }
                }"
            )

            val responseCode =
                if (purchases == null && billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    BillingClient.BillingResponseCode.ERROR
                } else {
                    billingResult.responseCode
                }

            val message = "Error updating purchases. ${billingResult.toHumanReadableDescription()}"

            val purchasesError = responseCode.billingResponseToPurchasesError(message).also { errorLog(it) }

            purchasesUpdatedListener?.onPurchasesFailedToUpdate(purchasesError)
        }
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        mainHandler.post {
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    log(
                        LogIntent.DEBUG, BillingStrings.BILLING_SERVICE_SETUP_FINISHED
                            .format(billingClient?.toString())
                    )
                    stateListener?.onConnected()
                    executePendingRequests()
                    reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS
                }
                BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED,
                BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                    val message =
                        BillingStrings.BILLING_UNAVAILABLE.format(billingResult.toHumanReadableDescription())
                    log(LogIntent.GOOGLE_WARNING, message)
                    // The calls will fail with an error that will be surfaced. We want to surface these errors
                    // Can't call executePendingRequests because it will not do anything since it checks for isReady()
                    synchronized(this@BillingWrapper) {
                        while (!serviceRequests.isEmpty()) {
                            serviceRequests.remove().let { serviceRequest ->
                                mainHandler.post {
                                    serviceRequest(
                                        billingResult.responseCode
                                            .billingResponseToPurchasesError(message)
                                            .also { errorLog(it) }
                                    )
                                }
                            }
                        }
                    }
                }
                BillingClient.BillingResponseCode.SERVICE_TIMEOUT,
                BillingClient.BillingResponseCode.ERROR,
                BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
                BillingClient.BillingResponseCode.USER_CANCELED,
                BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
                    log(
                        LogIntent.GOOGLE_WARNING, BillingStrings.BILLING_CLIENT_ERROR
                            .format(billingResult.toHumanReadableDescription())
                    )
                    retryBillingServiceConnectionWithExponentialBackoff()
                }
                BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED,
                BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> {
                    log(
                        LogIntent.GOOGLE_WARNING, BillingStrings.BILLING_CLIENT_ERROR
                            .format(billingResult.toHumanReadableDescription())
                    )
                }
                BillingClient.BillingResponseCode.DEVELOPER_ERROR -> {
                    // Billing service is already trying to connect. Don't do anything.
                }
            }
        }
    }

    override fun onBillingServiceDisconnected() {
        mainHandler.post {
            log(LogIntent.DEBUG, BillingStrings.BILLING_SERVICE_DISCONNECTED.format(billingClient.toString()))
        }
        retryBillingServiceConnectionWithExponentialBackoff()
    }

    /**
     * Retries the billing service connection with exponential backoff, maxing out at the time
     * specified by RECONNECT_TIMER_MAX_TIME_MILLISECONDS.
     *
     * This prevents ANRs, see https://github.com/android/play-billing-samples/issues/310
     */
    private fun retryBillingServiceConnectionWithExponentialBackoff() {
        log(LogIntent.DEBUG, BillingStrings.BILLING_CLIENT_RETRY.format(reconnectMilliseconds))
        startConnectionOnMainThread(reconnectMilliseconds)
        reconnectMilliseconds = min(
            reconnectMilliseconds * 2,
            RECONNECT_TIMER_MAX_TIME_MILLISECONDS
        )
    }

    override fun isConnected(): Boolean = billingClient?.isReady ?: false

    private fun withConnectedClient(receivingFunction: BillingClient.() -> Unit) {
        billingClient?.takeIf { it.isReady }?.let {
            it.receivingFunction()
        } ?: log(LogIntent.GOOGLE_WARNING, BillingStrings.BILLING_CLIENT_DISCONNECTED.format(getStackTrace()))
    }

    private fun getStackTrace(): String {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        Throwable().printStackTrace(printWriter)
        return stringWriter.toString()
    }

    private fun getStoreTransaction(
        purchase: Purchase,
        completion: (storeTxn: StoreTransaction) -> Unit
    ) {
        log(
            LogIntent.DEBUG, BillingStrings.BILLING_WRAPPER_PURCHASES_UPDATED
                .format(purchase.toHumanReadableDescription())
        )

        synchronized(this@BillingWrapper) {
            val presentedOffering = presentedOfferingsByProductIdentifier[purchase.firstSku]
            productTypes[purchase.firstSku]?.let { productType ->
                completion(
                    purchase.toStoreTransaction(
                        productType,
                        presentedOffering
                    )
                )
                return
            }

            getPurchaseType(purchase.purchaseToken) { type ->
                completion(
                    purchase.toStoreTransaction(
                        type,
                        presentedOffering
                    )
                )
            }
        }
    }

    private fun BillingClient.queryProductDetailsAsyncEnsuringOneResponse(
        params: QueryProductDetailsParams,
        listener: ProductDetailsResponseListener
    ) {
        var hasResponded = false
        queryProductDetailsAsync(params) { billingResult, skuDetailsList ->
            synchronized(this@BillingWrapper) {
                if (hasResponded) {
                    log(
                        LogIntent.GOOGLE_ERROR,
                        OfferingStrings.EXTRA_QUERY_SKU_DETAILS_RESPONSE.format(billingResult.responseCode)
                    )
                    return@queryProductDetailsAsync
                }
                hasResponded = true
            }
            listener.onProductDetailsResponse(billingResult, skuDetailsList)
        }
    }

    private fun BillingClient.queryPurchaseHistoryAsyncEnsuringOneResponse(
        @BillingClient.ProductType productType: String,
        listener: PurchaseHistoryResponseListener
    ) {
        var hasResponded = false

        productType.buildQueryPurchaseHistoryParams()?.let { queryPurchaseHistoryParams ->
            queryPurchaseHistoryAsync(queryPurchaseHistoryParams) { billingResult, purchaseHistory ->
                synchronized(this@BillingWrapper) {
                    if (hasResponded) {
                        log(
                            LogIntent.GOOGLE_ERROR,
                            RestoreStrings.EXTRA_QUERY_PURCHASE_HISTORY_RESPONSE.format(billingResult.responseCode)
                        )
                        return@queryPurchaseHistoryAsync
                    }
                    hasResponded = true
                }
                listener.onPurchaseHistoryResponse(billingResult, purchaseHistory)
            }
        } ?: run {
            errorLog(PurchaseStrings.INVALID_PRODUCT_TYPE.format("getPurchaseType"))
            val devErrorResponseCode =
                BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.DEVELOPER_ERROR).build()
            listener.onPurchaseHistoryResponse(devErrorResponseCode, null)
        }
    }

    private fun buildPurchaseParams(
        storeProduct: GoogleStoreProduct,
        purchaseOption: GooglePurchaseOption,
        replaceSkuInfo: ReplaceSkuInfo?,
        appUserID: String
    ): BillingFlowParams {
        val productDetailsParamsList = BillingFlowParams.ProductDetailsParams.newBuilder().apply {
            setOfferToken(purchaseOption.token)
            setProductDetails(storeProduct.productDetails)
        }.build()

        return BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParamsList))
            .apply {
                // only setObfuscatedAccountId for non-upgrade/downgrades until google issue is fixed:
                // https://issuetracker.google.com/issues/155005449
                replaceSkuInfo?.let {
                    setUpgradeInfo(it)
                } ?: setObfuscatedAccountId(appUserID.sha256())
            }.build()
    }
}
