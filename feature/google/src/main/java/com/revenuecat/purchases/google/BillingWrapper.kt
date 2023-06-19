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
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.ReplaceProductInfo
import com.revenuecat.purchases.common.StoreProductsCallback
import com.revenuecat.purchases.common.between
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.firstProductId
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.common.sha256
import com.revenuecat.purchases.common.toHumanReadableDescription
import com.revenuecat.purchases.models.GoogleProrationMode
import com.revenuecat.purchases.models.GooglePurchasingData
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.strings.BillingStrings
import com.revenuecat.purchases.strings.OfferingStrings
import com.revenuecat.purchases.strings.PurchaseStrings
import com.revenuecat.purchases.strings.RestoreStrings
import com.revenuecat.purchases.utils.Result
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Date
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.min
import kotlin.time.Duration

private const val RECONNECT_TIMER_START_MILLISECONDS = 1L * 1000L
private const val RECONNECT_TIMER_MAX_TIME_MILLISECONDS = 1000L * 60L * 15L // 15 minutes

@Suppress("LargeClass", "TooManyFunctions")
class BillingWrapper(
    private val clientFactory: ClientFactory,
    private val mainHandler: Handler,
    private val deviceCache: DeviceCache,
    @Suppress("unused")
    private val diagnosticsTrackerIfEnabled: DiagnosticsTracker?,
    private val dateProvider: DateProvider = DefaultDateProvider(),
) : BillingAbstract(), PurchasesUpdatedListener, BillingClientStateListener {

    @get:Synchronized
    @set:Synchronized
    @Volatile
    var billingClient: BillingClient? = null

    private val purchaseContext = mutableMapOf<String, PurchaseContext>()

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
            delayMilliseconds,
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
        onError: PurchasesErrorCallback,
    ) {
        val nonEmptyProductIds = productIds.filter { it.isNotEmpty() }.toSet()

        if (nonEmptyProductIds.isEmpty()) {
            log(LogIntent.DEBUG, OfferingStrings.EMPTY_PRODUCT_ID_LIST)
            onReceive(emptyList())
            return
        }

        log(LogIntent.DEBUG, OfferingStrings.FETCHING_PRODUCTS.format(productIds.joinToString()))
        executeRequestOnUIThread { connectionError ->
            if (connectionError == null) {
                val googleType = productType.toGoogleProductType() ?: BillingClient.ProductType.INAPP
                val params = googleType.buildQueryProductDetailsParams(nonEmptyProductIds)

                withConnectedClient {
                    queryProductDetailsAsyncEnsuringOneResponse(
                        googleType,
                        params,
                    ) { billingResult, productDetailsList ->
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            log(
                                LogIntent.DEBUG,
                                OfferingStrings.FETCHING_PRODUCTS_FINISHED
                                    .format(productIds.joinToString()),
                            )
                            log(
                                LogIntent.PURCHASE,
                                OfferingStrings.RETRIEVED_PRODUCTS
                                    .format(productDetailsList.joinToString { it.toString() }),
                            )
                            productDetailsList.takeUnless { it.isEmpty() }?.forEach {
                                log(LogIntent.PURCHASE, OfferingStrings.LIST_PRODUCTS.format(it.productId, it))
                            }

                            val storeProducts = productDetailsList.toStoreProducts()
                            onReceive(storeProducts)
                        } else {
                            log(
                                LogIntent.GOOGLE_ERROR,
                                OfferingStrings.FETCHING_PRODUCTS_ERROR
                                    .format(billingResult.toHumanReadableDescription()),
                            )
                            onError(
                                billingResult.responseCode.billingResponseToPurchasesError(
                                    "Error when fetching products. ${billingResult.toHumanReadableDescription()}",
                                ).also { errorLog(it) },
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
        purchasingData: PurchasingData,
        replaceProductInfo: ReplaceProductInfo?,
        presentedOfferingIdentifier: String?,
        isPersonalizedPrice: Boolean?,
    ) {
        val googlePurchasingData = purchasingData as? GooglePurchasingData
        if (googlePurchasingData == null) {
            val error = PurchasesError(
                PurchasesErrorCode.UnknownError,
                PurchaseStrings.INVALID_PURCHASE_TYPE.format(
                    "Play",
                    "GooglePurchasingData",
                ),
            )
            errorLog(error)
            purchasesUpdatedListener?.onPurchasesFailedToUpdate(error)
            return
        }

        val subscriptionOptionId = when (googlePurchasingData) {
            is GooglePurchasingData.InAppProduct -> {
                null
            }

            is GooglePurchasingData.Subscription -> {
                googlePurchasingData.optionId
            }
        }

        if (replaceProductInfo != null) {
            log(
                LogIntent.PURCHASE,
                PurchaseStrings.UPGRADING_SKU
                    .format(replaceProductInfo.oldPurchase.productIds[0], googlePurchasingData.productId),
            )
        } else {
            log(LogIntent.PURCHASE, PurchaseStrings.PURCHASING_PRODUCT.format(googlePurchasingData.productId))
        }

        synchronized(this@BillingWrapper) {
            // When using DEFERRED proration mode, callback needs to be associated with the *old* product we are
            // switching from, because the transaction we receive on successful purchase is for the old product.
            val productId =
                if (replaceProductInfo?.prorationMode == GoogleProrationMode.DEFERRED) {
                    replaceProductInfo.oldPurchase.productIds.first()
                } else googlePurchasingData.productId
            purchaseContext[productId] = PurchaseContext(
                googlePurchasingData.productType,
                presentedOfferingIdentifier,
                subscriptionOptionId,
                replaceProductInfo?.prorationMode as? GoogleProrationMode?,
            )
        }
        executeRequestOnUIThread {
            val result = buildPurchaseParams(
                purchasingData,
                replaceProductInfo,
                appUserID,
                isPersonalizedPrice,
            )
            when (result) {
                is Result.Success -> launchBillingFlow(activity, result.value)
                is Result.Error -> purchasesUpdatedListener?.onPurchasesFailedToUpdate(result.value)
            }
        }
    }

    @UiThread
    private fun launchBillingFlow(
        activity: Activity,
        params: BillingFlowParams,
    ) {
        if (activity.intent == null) {
            log(LogIntent.WARNING, BillingStrings.NULL_ACTIVITY_INTENT)
        }
        withConnectedClient {
            launchBillingFlow(activity, params)
                .takeIf { billingResult -> billingResult.responseCode != BillingClient.BillingResponseCode.OK }
                ?.let { billingResult ->
                    log(
                        LogIntent.GOOGLE_ERROR,
                        BillingStrings.BILLING_INTENT_FAILED
                            .format(billingResult.toHumanReadableDescription()),
                    )
                }
        }
    }

    fun queryPurchaseHistoryAsync(
        @BillingClient.ProductType productType: String,
        onReceivePurchaseHistory: (List<PurchaseHistoryRecord>) -> Unit,
        onReceivePurchaseHistoryError: (PurchasesError) -> Unit,
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
                                    LogIntent.RC_PURCHASE_SUCCESS,
                                    RestoreStrings.PURCHASE_HISTORY_RETRIEVED
                                        .format(it.toHumanReadableDescription()),
                                )
                            } ?: log(LogIntent.DEBUG, RestoreStrings.PURCHASE_HISTORY_EMPTY)
                            onReceivePurchaseHistory(purchaseHistoryRecordList ?: emptyList())
                        } else {
                            onReceivePurchaseHistoryError(
                                billingResult.responseCode.billingResponseToPurchasesError(
                                    "Error receiving purchase history. ${billingResult.toHumanReadableDescription()}",
                                ).also { errorLog(it) },
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
        onReceivePurchaseHistoryError: (PurchasesError) -> Unit,
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
                            },
                        )
                    },
                    onReceivePurchaseHistoryError,
                )
            },
            onReceivePurchaseHistoryError,
        )
    }

    override fun consumeAndSave(
        shouldTryToConsume: Boolean,
        purchase: StoreTransaction,
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
                        LogIntent.GOOGLE_ERROR,
                        PurchaseStrings.CONSUMING_PURCHASE_ERROR
                            .format(billingResult.toHumanReadableDescription()),
                    )
                }
            }
        } else if (shouldTryToConsume && !alreadyAcknowledged) {
            acknowledge(purchase.purchaseToken) { billingResult, purchaseToken ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    deviceCache.addSuccessfullyPostedToken(purchaseToken)
                } else {
                    log(
                        LogIntent.GOOGLE_ERROR,
                        PurchaseStrings.ACKNOWLEDGING_PURCHASE_ERROR
                            .format(billingResult.toHumanReadableDescription()),
                    )
                }
            }
        } else {
            deviceCache.addSuccessfullyPostedToken(purchase.purchaseToken)
        }
    }

    internal fun consumePurchase(
        token: String,
        onConsumed: (billingResult: BillingResult, purchaseToken: String) -> Unit,
    ) {
        log(LogIntent.PURCHASE, PurchaseStrings.CONSUMING_PURCHASE.format(token))
        executeRequestOnUIThread { connectionError ->
            if (connectionError == null) {
                withConnectedClient {
                    consumeAsync(
                        ConsumeParams.newBuilder().setPurchaseToken(token).build(),
                        onConsumed,
                    )
                }
            }
        }
    }

    internal fun acknowledge(
        token: String,
        onAcknowledged: (billingResult: BillingResult, purchaseToken: String) -> Unit,
    ) {
        log(LogIntent.PURCHASE, PurchaseStrings.ACKNOWLEDGING_PURCHASE.format(token))
        executeRequestOnUIThread { connectionError ->
            if (connectionError == null) {
                withConnectedClient {
                    acknowledgePurchase(
                        AcknowledgePurchaseParams.newBuilder().setPurchaseToken(token).build(),
                    ) { billingResult ->
                        onAcknowledged(billingResult, token)
                    }
                }
            }
        }
    }

    @Suppress("ReturnCount", "LongMethod")
    override fun queryPurchases(
        appUserID: String,
        onSuccess: (Map<String, StoreTransaction>) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        executeRequestOnUIThread { connectionError ->
            if (connectionError != null) {
                onError(connectionError)
                return@executeRequestOnUIThread
            }
            withConnectedClient {
                log(LogIntent.DEBUG, RestoreStrings.QUERYING_PURCHASE)

                val querySubsPurchasesParams = BillingClient.ProductType.SUBS.buildQueryPurchasesParams()
                if (querySubsPurchasesParams == null) {
                    onError(
                        PurchasesError(
                            PurchasesErrorCode.PurchaseInvalidError,
                            PurchaseStrings.INVALID_PRODUCT_TYPE.format("queryPurchases"),
                        ),
                    )
                    return@withConnectedClient
                }
                this.queryPurchasesAsyncWithTracking(
                    BillingClient.ProductType.SUBS,
                    querySubsPurchasesParams,
                ) querySubPurchasesAsync@{ activeSubsResult, activeSubsPurchases ->

                    if (!activeSubsResult.isSuccessful()) {
                        val purchasesError = activeSubsResult.responseCode.billingResponseToPurchasesError(
                            RestoreStrings.QUERYING_SUBS_ERROR.format(activeSubsResult.toHumanReadableDescription()),
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
                                PurchaseStrings.INVALID_PRODUCT_TYPE.format("queryPurchases"),
                            ),
                        )
                        return@querySubPurchasesAsync
                    }

                    this.queryPurchasesAsyncWithTracking(
                        BillingClient.ProductType.INAPP,
                        queryInAppsPurchasesParams,
                    ) queryInAppsPurchasesAsync@{ unconsumedInAppsResult, unconsumedInAppsPurchases ->

                        if (!unconsumedInAppsResult.isSuccessful()) {
                            val purchasesError =
                                unconsumedInAppsResult.responseCode.billingResponseToPurchasesError(
                                    RestoreStrings.QUERYING_INAPP_ERROR.format(
                                        unconsumedInAppsResult.toHumanReadableDescription(),
                                    ),
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
    }

    private fun List<Purchase>.toMapOfGooglePurchaseWrapper(
        @BillingClient.ProductType productType: String,
    ): Map<String, StoreTransaction> {
        return this.associate { purchase ->
            val hash = purchase.purchaseToken.sha1()
            hash to purchase.toStoreTransaction(productType.toRevenueCatProductType())
        }
    }

    override fun findPurchaseInPurchaseHistory(
        appUserID: String,
        productType: ProductType,
        productId: String,
        onCompletion: (StoreTransaction) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        withConnectedClient {
            log(LogIntent.DEBUG, RestoreStrings.QUERYING_PURCHASE_WITH_TYPE.format(productId, productType.name))
            productType.toGoogleProductType()?.let { googleProductType ->
                queryPurchaseHistoryAsyncEnsuringOneResponse(googleProductType) { result, purchasesList ->
                    if (result.isSuccessful()) {
                        val purchaseHistoryRecordWrapper =
                            purchasesList?.firstOrNull { it.products.contains(productId) }
                                ?.toStoreTransaction(productType)

                        if (purchaseHistoryRecordWrapper != null) {
                            onCompletion(purchaseHistoryRecordWrapper)
                        } else {
                            val message = PurchaseStrings.NO_EXISTING_PURCHASE.format(productId)
                            val error = PurchasesError(PurchasesErrorCode.PurchaseInvalidError, message)
                            onError(error)
                        }
                    } else {
                        val underlyingErrorMessage = PurchaseStrings.ERROR_FINDING_PURCHASE.format(productId)
                        val error =
                            result.responseCode.billingResponseToPurchasesError(underlyingErrorMessage)
                        onError(error)
                    }
                }
            } ?: onError(
                PurchasesError(PurchasesErrorCode.PurchaseInvalidError, PurchaseStrings.NOT_RECOGNIZED_PRODUCT_TYPE),
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

            client.queryPurchasesAsyncWithTracking(
                BillingClient.ProductType.SUBS,
                querySubsPurchasesParams,
            ) querySubPurchasesAsync@{ querySubsResult, subsPurchasesList ->

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
                client.queryPurchasesAsyncWithTracking(
                    BillingClient.ProductType.INAPP,
                    queryInAppsPurchasesParams,
                ) queryInAppPurchasesAsync@{ queryInAppsResult, inAppPurchasesList ->

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
        purchases: List<Purchase>?,
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
        } else {
            log(
                LogIntent.GOOGLE_ERROR,
                BillingStrings.BILLING_WRAPPER_PURCHASES_ERROR
                    .format(billingResult.toHumanReadableDescription()) +
                    "${
                        notNullPurchasesList.takeUnless { it.isEmpty() }?.let { purchase ->
                            "Purchases:" + purchase.joinToString(
                                ", ",
                                transform = { it.toHumanReadableDescription() },
                            )
                        }
                    }",
            )

            var message = "Error updating purchases. ${billingResult.toHumanReadableDescription()}"
            var responseCode = billingResult.responseCode

            if (purchases == null && billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                // Result being ok but with a Null purchase used to happen when doing a product change
                // in DEFERRED proration mode in Billing Client <= 4. Should not happen in Billing Client 5+ since
                // we get the transaction for the previous product.
                message = "Error: onPurchasesUpdated received an OK BillingResult with a Null purchases list."
                responseCode = BillingClient.BillingResponseCode.ERROR
            }

            val purchasesError = responseCode.billingResponseToPurchasesError(message).also { errorLog(it) }

            purchasesUpdatedListener?.onPurchasesFailedToUpdate(purchasesError)
        }
    }

    @Suppress("LongMethod")
    override fun onBillingSetupFinished(billingResult: BillingResult) {
        mainHandler.post {
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    log(
                        LogIntent.DEBUG,
                        BillingStrings.BILLING_SERVICE_SETUP_FINISHED
                            .format(billingClient?.toString()),
                    )
                    stateListener?.onConnected()
                    executePendingRequests()
                    reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS
                }
                BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED,
                BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
                -> {
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
                                            .also { errorLog(it) },
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
                BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
                -> {
                    log(
                        LogIntent.GOOGLE_WARNING,
                        BillingStrings.BILLING_CLIENT_ERROR
                            .format(billingResult.toHumanReadableDescription()),
                    )
                    retryBillingServiceConnectionWithExponentialBackoff()
                }
                BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED,
                BillingClient.BillingResponseCode.ITEM_NOT_OWNED,
                -> {
                    log(
                        LogIntent.GOOGLE_WARNING,
                        BillingStrings.BILLING_CLIENT_ERROR
                            .format(billingResult.toHumanReadableDescription()),
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
            RECONNECT_TIMER_MAX_TIME_MILLISECONDS,
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
        completion: (storeTxn: StoreTransaction) -> Unit,
    ) {
        log(
            LogIntent.DEBUG,
            BillingStrings.BILLING_WRAPPER_PURCHASES_UPDATED
                .format(purchase.toHumanReadableDescription()),
        )

        synchronized(this@BillingWrapper) {
            val context = purchaseContext[purchase.firstProductId]
            context?.productType?.let { productType ->
                completion(
                    purchase.toStoreTransaction(context),
                )
                return
            }

            getPurchaseType(purchase.purchaseToken) { type ->
                completion(
                    purchase.toStoreTransaction(
                        type,
                        context?.presentedOfferingId,
                    ),
                )
            }
        }
    }

    private fun BillingClient.queryProductDetailsAsyncEnsuringOneResponse(
        @BillingClient.ProductType productType: String,
        params: QueryProductDetailsParams,
        listener: ProductDetailsResponseListener,
    ) {
        var hasResponded = false
        val requestStartTime = dateProvider.now
        queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            synchronized(this@BillingWrapper) {
                if (hasResponded) {
                    log(
                        LogIntent.GOOGLE_ERROR,
                        OfferingStrings.EXTRA_QUERY_PRODUCT_DETAILS_RESPONSE.format(billingResult.responseCode),
                    )
                    return@queryProductDetailsAsync
                }
                hasResponded = true
            }
            trackGoogleQueryProductDetailsRequestIfNeeded(productType, billingResult, requestStartTime)
            listener.onProductDetailsResponse(billingResult, productDetailsList)
        }
    }

    private fun BillingClient.queryPurchaseHistoryAsyncEnsuringOneResponse(
        @BillingClient.ProductType productType: String,
        listener: PurchaseHistoryResponseListener,
    ) {
        var hasResponded = false
        val requestStartTime = dateProvider.now

        productType.buildQueryPurchaseHistoryParams()?.let { queryPurchaseHistoryParams ->
            queryPurchaseHistoryAsync(queryPurchaseHistoryParams) { billingResult, purchaseHistory ->
                synchronized(this@BillingWrapper) {
                    if (hasResponded) {
                        log(
                            LogIntent.GOOGLE_ERROR,
                            RestoreStrings.EXTRA_QUERY_PURCHASE_HISTORY_RESPONSE.format(billingResult.responseCode),
                        )
                        return@queryPurchaseHistoryAsync
                    }
                    hasResponded = true
                }
                trackGoogleQueryPurchaseHistoryRequestIfNeeded(productType, billingResult, requestStartTime)
                listener.onPurchaseHistoryResponse(billingResult, purchaseHistory)
            }
        } ?: run {
            errorLog(PurchaseStrings.INVALID_PRODUCT_TYPE.format("getPurchaseType"))
            val devErrorResponseCode =
                BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.DEVELOPER_ERROR).build()
            listener.onPurchaseHistoryResponse(devErrorResponseCode, null)
        }
    }

    private fun BillingClient.queryPurchasesAsyncWithTracking(
        @BillingClient.ProductType productType: String,
        queryParams: QueryPurchasesParams,
        listener: PurchasesResponseListener,
    ) {
        val requestStartTime = dateProvider.now
        queryPurchasesAsync(queryParams) { billingResult, purchases ->
            trackGoogleQueryPurchasesRequestIfNeeded(productType, billingResult, requestStartTime)
            listener.onQueryPurchasesResponse(billingResult, purchases)
        }
    }

    private fun trackGoogleQueryProductDetailsRequestIfNeeded(
        @BillingClient.ProductType productType: String,
        billingResult: BillingResult,
        requestStartTime: Date,
    ) {
        diagnosticsTrackerIfEnabled?.trackGoogleQueryProductDetailsRequest(
            productType,
            billingResult.responseCode,
            billingResult.debugMessage,
            responseTime = Duration.between(requestStartTime, dateProvider.now),
        )
    }

    private fun trackGoogleQueryPurchasesRequestIfNeeded(
        @BillingClient.ProductType productType: String,
        billingResult: BillingResult,
        requestStartTime: Date,
    ) {
        diagnosticsTrackerIfEnabled?.trackGoogleQueryPurchasesRequest(
            productType,
            billingResult.responseCode,
            billingResult.debugMessage,
            responseTime = Duration.between(requestStartTime, dateProvider.now),
        )
    }

    private fun trackGoogleQueryPurchaseHistoryRequestIfNeeded(
        @BillingClient.ProductType productType: String,
        billingResult: BillingResult,
        requestStartTime: Date,
    ) {
        diagnosticsTrackerIfEnabled?.trackGoogleQueryPurchaseHistoryRequest(
            productType,
            billingResult.responseCode,
            billingResult.debugMessage,
            responseTime = Duration.between(requestStartTime, dateProvider.now),
        )
    }

    private fun buildPurchaseParams(
        purchaseInfo: GooglePurchasingData,
        replaceProductInfo: ReplaceProductInfo?,
        appUserID: String,
        isPersonalizedPrice: Boolean?,
    ): Result<BillingFlowParams, PurchasesError> {
        return when (purchaseInfo) {
            is GooglePurchasingData.InAppProduct -> {
                buildOneTimePurchaseParams(purchaseInfo, appUserID, isPersonalizedPrice)
            }
            is GooglePurchasingData.Subscription -> {
                buildSubscriptionPurchaseParams(purchaseInfo, replaceProductInfo, appUserID, isPersonalizedPrice)
            }
        }
    }

    private fun buildOneTimePurchaseParams(
        purchaseInfo: GooglePurchasingData.InAppProduct,
        appUserID: String,
        isPersonalizedPrice: Boolean?,
    ): Result<BillingFlowParams, PurchasesError> {
        val productDetailsParamsList = BillingFlowParams.ProductDetailsParams.newBuilder().apply {
            setProductDetails(purchaseInfo.productDetails)
        }.build()

        return Result.Success(
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParamsList))
                .setObfuscatedAccountId(appUserID.sha256())
                .apply {
                    isPersonalizedPrice?.let {
                        setIsOfferPersonalized(it)
                    }
                }
                .build(),
        )
    }

    private fun buildSubscriptionPurchaseParams(
        purchaseInfo: GooglePurchasingData.Subscription,
        replaceProductInfo: ReplaceProductInfo?,
        appUserID: String,
        isPersonalizedPrice: Boolean?,
    ): Result<BillingFlowParams, PurchasesError> {
        val productDetailsParamsList = BillingFlowParams.ProductDetailsParams.newBuilder().apply {
            setOfferToken(purchaseInfo.token)
            setProductDetails(purchaseInfo.productDetails)
        }.build()

        return Result.Success(
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParamsList))
                .apply {
                    // only setObfuscatedAccountId for non-upgrade/downgrades until google issue is fixed:
                    // https://issuetracker.google.com/issues/155005449
                    replaceProductInfo?.let {
                        setUpgradeInfo(it)
                    } ?: setObfuscatedAccountId(appUserID.sha256())

                    isPersonalizedPrice?.let {
                        setIsOfferPersonalized(it)
                    }
                }
                .build(),
        )
    }
}
