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
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.InAppMessageParams
import com.android.billingclient.api.InAppMessageResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.PurchasesUpdatedListener
import com.revenuecat.purchases.PostReceiptInitiationSource
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesStateProvider
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.ReplaceProductInfo
import com.revenuecat.purchases.common.StoreProductsCallback
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.firstProductId
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.sha256
import com.revenuecat.purchases.common.toHumanReadableDescription
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.google.usecase.AcknowledgePurchaseUseCase
import com.revenuecat.purchases.google.usecase.AcknowledgePurchaseUseCaseParams
import com.revenuecat.purchases.google.usecase.ConsumePurchaseUseCase
import com.revenuecat.purchases.google.usecase.ConsumePurchaseUseCaseParams
import com.revenuecat.purchases.google.usecase.GetBillingConfigUseCase
import com.revenuecat.purchases.google.usecase.GetBillingConfigUseCaseParams
import com.revenuecat.purchases.google.usecase.QueryProductDetailsUseCase
import com.revenuecat.purchases.google.usecase.QueryProductDetailsUseCaseParams
import com.revenuecat.purchases.google.usecase.QueryPurchaseHistoryUseCase
import com.revenuecat.purchases.google.usecase.QueryPurchaseHistoryUseCaseParams
import com.revenuecat.purchases.google.usecase.QueryPurchasesByTypeUseCase
import com.revenuecat.purchases.google.usecase.QueryPurchasesByTypeUseCaseParams
import com.revenuecat.purchases.google.usecase.QueryPurchasesUseCase
import com.revenuecat.purchases.google.usecase.QueryPurchasesUseCaseParams
import com.revenuecat.purchases.models.GooglePurchasingData
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.InAppMessageType
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
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.min

private const val RECONNECT_TIMER_START_MILLISECONDS = 1L * 1000L
private const val RECONNECT_TIMER_MAX_TIME_MILLISECONDS = 1000L * 60L * 15L // 15 minutes

@Suppress("LargeClass", "TooManyFunctions")
internal class BillingWrapper(
    private val clientFactory: ClientFactory,
    private val mainHandler: Handler,
    private val deviceCache: DeviceCache,
    @Suppress("unused")
    private val diagnosticsTrackerIfEnabled: DiagnosticsTracker?,
    purchasesStateProvider: PurchasesStateProvider,
    private val dateProvider: DateProvider = DefaultDateProvider(),
) : BillingAbstract(purchasesStateProvider), PurchasesUpdatedListener, BillingClientStateListener {

    @get:Synchronized
    @set:Synchronized
    @Volatile
    var billingClient: BillingClient? = null

    private val purchaseContext = mutableMapOf<String, PurchaseContext>()

    private val serviceRequests =
        ConcurrentLinkedQueue<Pair<(connectionError: PurchasesError?) -> Unit, Long?>>()

    // how long before the data source tries to reconnect to Google play
    private var reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS

    @get:Synchronized
    @set:Synchronized
    private var reconnectionAlreadyScheduled = false

    val appInBackground: Boolean
        get() = purchasesStateProvider.purchasesState.appInBackground

    class ClientFactory(private val context: Context) {
        @UiThread
        fun buildClient(listener: com.android.billingclient.api.PurchasesUpdatedListener): BillingClient {
            return BillingClient.newBuilder(context).enablePendingPurchases().setListener(listener)
                .build()
        }
    }

    private fun executePendingRequests() {
        synchronized(this@BillingWrapper) {
            while (billingClient?.isReady == true) {
                serviceRequests.poll()?.let { (request, delayMilliseconds) ->
                    if (delayMilliseconds != null) {
                        mainHandler.postDelayed(
                            { request(null) },
                            delayMilliseconds,
                        )
                    } else {
                        mainHandler.post { request(null) }
                    }
                } ?: break
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

            reconnectionAlreadyScheduled = false

            billingClient?.let {
                if (!it.isReady) {
                    log(LogIntent.DEBUG, BillingStrings.BILLING_CLIENT_STARTING.format(it))
                    try {
                        it.startConnection(this)
                    } catch (e: IllegalStateException) {
                        log(
                            LogIntent.GOOGLE_ERROR,
                            BillingStrings.ILLEGAL_STATE_EXCEPTION_WHEN_CONNECTING.format(e),
                        )
                        val error = PurchasesError(PurchasesErrorCode.StoreProblemError, e.message)
                        sendErrorsToAllPendingRequests(error)
                    }
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
    private fun executeRequestOnUIThread(delayMilliseconds: Long? = null, request: (PurchasesError?) -> Unit) {
        if (purchasesUpdatedListener != null) {
            serviceRequests.add(request to delayMilliseconds)
            if (billingClient?.isReady == false) {
                startConnectionOnMainThread()
            } else {
                executePendingRequests()
            }
        } else {
            // This shouldn't happen, but if it does, we want to propagate an error instead of hanging.
            request(PurchasesError(PurchasesErrorCode.UnknownError, "BillingWrapper is not attached to a listener"))
        }
    }

    @Suppress("LongMethod")
    override fun queryProductDetailsAsync(
        productType: ProductType,
        productIds: Set<String>,
        appInBackground: Boolean,
        onReceive: StoreProductsCallback,
        onError: PurchasesErrorCallback,
    ) {
        log(LogIntent.DEBUG, OfferingStrings.FETCHING_PRODUCTS.format(productIds.joinToString()))
        val useCase = QueryProductDetailsUseCase(
            QueryProductDetailsUseCaseParams(
                dateProvider,
                diagnosticsTrackerIfEnabled,
                productIds,
                productType,
                appInBackground,
            ),
            onReceive,
            onError,
            ::withConnectedClient,
            ::executeRequestOnUIThread,
        )
        useCase.run()
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
            val productId = googlePurchasingData.productId
            purchaseContext[productId] = PurchaseContext(
                googlePurchasingData.productType,
                presentedOfferingIdentifier,
                subscriptionOptionId,
                replaceProductInfo?.replacementMode as? GoogleReplacementMode?,
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
        appInBackground: Boolean,
        onReceivePurchaseHistory: (List<PurchaseHistoryRecord>) -> Unit,
        onReceivePurchaseHistoryError: (PurchasesError) -> Unit,
    ) {
        log(LogIntent.DEBUG, RestoreStrings.QUERYING_PURCHASE_HISTORY.format(productType))
        QueryPurchaseHistoryUseCase(
            QueryPurchaseHistoryUseCaseParams(
                dateProvider,
                diagnosticsTrackerIfEnabled,
                productType,
                appInBackground,
            ),
            onReceivePurchaseHistory,
            onReceivePurchaseHistoryError,
            ::withConnectedClient,
            ::executeRequestOnUIThread,
        ).run()
    }

    override fun queryAllPurchases(
        appUserID: String,
        onReceivePurchaseHistory: (List<StoreTransaction>) -> Unit,
        onReceivePurchaseHistoryError: (PurchasesError) -> Unit,
    ) {
        queryPurchaseHistoryAsync(
            BillingClient.ProductType.SUBS,
            appInBackground,
            { subsPurchasesList ->
                queryPurchaseHistoryAsync(
                    BillingClient.ProductType.INAPP,
                    appInBackground,
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
        initiationSource: PostReceiptInitiationSource,
        appInBackground: Boolean,
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
            consumePurchase(
                purchase.purchaseToken,
                initiationSource,
                appInBackground,
                onConsumed = deviceCache::addSuccessfullyPostedToken,
            )
        } else if (shouldTryToConsume && !alreadyAcknowledged) {
            acknowledge(
                purchase.purchaseToken,
                initiationSource,
                appInBackground,
                onAcknowledged = deviceCache::addSuccessfullyPostedToken,
            )
        } else {
            deviceCache.addSuccessfullyPostedToken(purchase.purchaseToken)
        }
    }

    internal fun consumePurchase(
        token: String,
        initiationSource: PostReceiptInitiationSource,
        appInBackground: Boolean,
        onConsumed: (purchaseToken: String) -> Unit,
    ) {
        log(LogIntent.PURCHASE, PurchaseStrings.CONSUMING_PURCHASE.format(token))
        ConsumePurchaseUseCase(
            ConsumePurchaseUseCaseParams(
                token,
                initiationSource,
                appInBackground,
            ),
            onReceive = onConsumed,
            onError = { error ->
                // TODO-retry: if ITEM_NOT_OWNED queryPurchasesAsync
                log(
                    LogIntent.GOOGLE_ERROR,
                    PurchaseStrings.CONSUMING_PURCHASE_ERROR
                        .format(error.underlyingErrorMessage),
                )
            },
            ::withConnectedClient,
            ::executeRequestOnUIThread,
        ).run()
    }

    internal fun acknowledge(
        token: String,
        initiationSource: PostReceiptInitiationSource,
        appInBackground: Boolean,
        onAcknowledged: (purchaseToken: String) -> Unit,
    ) {
        log(LogIntent.PURCHASE, PurchaseStrings.ACKNOWLEDGING_PURCHASE.format(token))
        AcknowledgePurchaseUseCase(
            AcknowledgePurchaseUseCaseParams(
                token,
                initiationSource,
                appInBackground,
            ),
            onReceive = onAcknowledged,
            { error ->
                // TODO-retry: if ITEM_NOT_OWNED queryPurchasesAsync
                log(
                    LogIntent.GOOGLE_ERROR,
                    PurchaseStrings.ACKNOWLEDGING_PURCHASE_ERROR
                        .format(error.underlyingErrorMessage),
                )
            },
            ::withConnectedClient,
            ::executeRequestOnUIThread,
        ).run()
    }

    @Suppress("ReturnCount", "LongMethod")
    override fun queryPurchases(
        appUserID: String,
        appInBackground: Boolean,
        onSuccess: (Map<String, StoreTransaction>) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        log(LogIntent.DEBUG, RestoreStrings.QUERYING_PURCHASE)
        QueryPurchasesUseCase(
            QueryPurchasesUseCaseParams(
                dateProvider,
                diagnosticsTrackerIfEnabled,
                appInBackground,
            ),
            onSuccess,
            onError,
            ::withConnectedClient,
            ::executeRequestOnUIThread,
        ).run()
    }

    override fun findPurchaseInPurchaseHistory(
        appUserID: String,
        productType: ProductType,
        productId: String,
        appInBackground: Boolean,
        onCompletion: (StoreTransaction) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        log(LogIntent.DEBUG, RestoreStrings.QUERYING_PURCHASE_WITH_TYPE.format(productId, productType.name))
        productType.toGoogleProductType()?.let { googleProductType ->
            QueryPurchaseHistoryUseCase(
                QueryPurchaseHistoryUseCaseParams(
                    dateProvider,
                    diagnosticsTrackerIfEnabled,
                    googleProductType,
                    appInBackground,
                ),
                { purchasesList ->
                    val purchaseHistoryRecordWrapper =
                        purchasesList.firstOrNull { it.products.contains(productId) }?.toStoreTransaction(productType)
                    if (purchaseHistoryRecordWrapper != null) {
                        onCompletion(purchaseHistoryRecordWrapper)
                    } else {
                        val message = PurchaseStrings.NO_EXISTING_PURCHASE.format(productId)
                        val error = PurchasesError(PurchasesErrorCode.PurchaseInvalidError, message)
                        onError(error)
                    }
                },
                onError,
                ::withConnectedClient,
                ::executeRequestOnUIThread,
            ).run()
        } ?: onError(
            PurchasesError(PurchasesErrorCode.PurchaseInvalidError, PurchaseStrings.NOT_RECOGNIZED_PRODUCT_TYPE),
        )
    }

    @VisibleForTesting(otherwise = PRIVATE)
    internal fun getPurchaseType(purchaseToken: String, listener: (ProductType) -> Unit) {
        queryPurchaseType(BillingClient.ProductType.SUBS, purchaseToken, listener) { subFound ->
            if (subFound) {
                listener(ProductType.SUBS)
            } else {
                queryPurchaseType(BillingClient.ProductType.INAPP, purchaseToken, listener) { inAppFound ->
                    if (inAppFound) {
                        listener(ProductType.INAPP)
                    } else {
                        listener(ProductType.UNKNOWN)
                    }
                }
            }
        }
    }

    private fun queryPurchaseType(
        productType: String,
        purchaseToken: String,
        listener: (ProductType) -> Unit,
        resultHandler: (Boolean) -> Unit,
    ) {
        QueryPurchasesByTypeUseCase(
            QueryPurchasesByTypeUseCaseParams(
                dateProvider,
                diagnosticsTrackerIfEnabled,
                appInBackground = appInBackground,
                productType = productType,
            ),
            onSuccess = { purchases ->
                resultHandler(purchases.values.any { it.purchaseToken == purchaseToken })
            },
            onError = { error ->
                errorLog(error)
                listener(ProductType.UNKNOWN)
            },
            withConnectedClient = ::withConnectedClient,
            executeRequestOnUIThread = ::executeRequestOnUIThread,
        ).run()
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
                    trackProductDetailsNotSupportedIfNeeded()
                }

                BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED,
                BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
                -> {
                    val originalErrorMessage = billingResult.toHumanReadableDescription()

                    /**
                     * We check for cases when Google sends Google Play In-app Billing API version is less than 3
                     * as a debug message. Version 3 is from 2012, so the message is a bit useless.
                     * We have detected this message in several cases:
                     *
                     * - When there's no Google account configured in the device
                     * - When there's no Play Store (this happens in incorrectly configured emulators)
                     * - When language is changed in the device and Play Store caches get corrupted. Opening the
                     * Play Store or clearing its caches would fix this case.
                     * See https://github.com/RevenueCat/purchases-android/issues/1288
                     */
                    val error = if (billingResult.debugMessage == IN_APP_BILLING_LESS_THAN_3_ERROR_MESSAGE) {
                        val underlyingErrorMessage =
                            BillingStrings.BILLING_UNAVAILABLE_LESS_THAN_3.format(originalErrorMessage)
                        PurchasesError(PurchasesErrorCode.StoreProblemError, underlyingErrorMessage)
                            .also { errorLog(it) }
                    } else {
                        val underlyingErrorMessage = BillingStrings.BILLING_UNAVAILABLE.format(originalErrorMessage)
                        billingResult.responseCode
                            .billingResponseToPurchasesError(underlyingErrorMessage)
                            .also { errorLog(it) }
                    }

                    // The calls will fail with an error that will be surfaced. We want to surface these errors
                    // Can't call executePendingRequests because it will not do anything since it checks for isReady()
                    sendErrorsToAllPendingRequests(error)
                }

                BillingClient.BillingResponseCode.ERROR,
                BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
                BillingClient.BillingResponseCode.USER_CANCELED,
                BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
                BillingClient.BillingResponseCode.NETWORK_ERROR,
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
        log(LogIntent.WARNING, BillingStrings.BILLING_SERVICE_DISCONNECTED_INSTANCE.format(billingClient?.toString()))
    }

    /**
     * Retries the billing service connection with exponential backoff, maxing out at the time
     * specified by RECONNECT_TIMER_MAX_TIME_MILLISECONDS.
     *
     * This prevents ANRs, see https://github.com/android/play-billing-samples/issues/310
     */
    private fun retryBillingServiceConnectionWithExponentialBackoff() {
        if (reconnectionAlreadyScheduled) {
            log(LogIntent.WARNING, BillingStrings.BILLING_CLIENT_RETRY_ALREADY_SCHEDULED)
        } else {
            log(LogIntent.WARNING, BillingStrings.BILLING_CLIENT_RETRY.format(reconnectMilliseconds))
            reconnectionAlreadyScheduled = true
            startConnectionOnMainThread(reconnectMilliseconds)
            reconnectMilliseconds = min(
                reconnectMilliseconds * 2,
                RECONNECT_TIMER_MAX_TIME_MILLISECONDS,
            )
        }
    }

    override fun isConnected(): Boolean = billingClient?.isReady ?: false

    override fun showInAppMessagesIfNeeded(
        activity: Activity,
        inAppMessageTypes: List<InAppMessageType>,
        subscriptionStatusChange: () -> Unit,
    ) {
        if (inAppMessageTypes.isEmpty()) {
            errorLog(BillingStrings.BILLING_UNSPECIFIED_INAPP_MESSAGE_TYPES)
            return
        }

        val inAppMessageParamsBuilder = InAppMessageParams.newBuilder()
        for (inAppMessageType in inAppMessageTypes) {
            inAppMessageParamsBuilder.addInAppMessageCategoryToShow(inAppMessageType.inAppMessageCategoryId)
        }
        val inAppMessageParams = inAppMessageParamsBuilder.build()
        val weakActivity = WeakReference(activity)

        executeRequestOnUIThread { error ->
            if (error != null) {
                errorLog(BillingStrings.BILLING_CONNECTION_ERROR_INAPP_MESSAGES.format(error))
                return@executeRequestOnUIThread
            }
            withConnectedClient {
                val activity = weakActivity.get() ?: run {
                    debugLog("Activity is null, not showing Google Play in-app message.")
                    return@withConnectedClient
                }
                showInAppMessages(activity, inAppMessageParams) { inAppMessageResult ->
                    when (val responseCode = inAppMessageResult.responseCode) {
                        InAppMessageResult.InAppMessageResponseCode.NO_ACTION_NEEDED -> {
                            verboseLog(BillingStrings.BILLING_INAPP_MESSAGE_NONE)
                        }

                        InAppMessageResult.InAppMessageResponseCode.SUBSCRIPTION_STATUS_UPDATED -> {
                            debugLog(BillingStrings.BILLING_INAPP_MESSAGE_UPDATE)
                            subscriptionStatusChange()
                        }

                        else -> errorLog(BillingStrings.BILLING_INAPP_MESSAGE_UNEXPECTED_CODE.format(responseCode))
                    }
                }
            }
        }
    }

    override fun getStorefront(
        appInBackground: Boolean,
        onSuccess: (String) -> Unit,
        onError: PurchasesErrorCallback,
    ) {
        verboseLog(BillingStrings.BILLING_INITIATE_GETTING_COUNTRY_CODE)
        GetBillingConfigUseCase(
            GetBillingConfigUseCaseParams(appInBackground),
            deviceCache = deviceCache,
            onReceive = { billingConfig -> onSuccess(billingConfig.countryCode) },
            onError = onError,
            withConnectedClient = ::withConnectedClient,
            executeRequestOnUIThread = ::executeRequestOnUIThread,
        ).run()
    }

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

    private fun trackProductDetailsNotSupportedIfNeeded() {
        if (diagnosticsTrackerIfEnabled == null) return
        val billingResult = billingClient?.isFeatureSupported(BillingClient.FeatureType.PRODUCT_DETAILS)
        if (
            billingResult != null &&
            billingResult.responseCode == BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED
        ) {
            diagnosticsTrackerIfEnabled.trackProductDetailsNotSupported(
                billingResult.responseCode,
                billingResult.debugMessage,
            )
        }
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

    @Synchronized
    private fun sendErrorsToAllPendingRequests(error: PurchasesError) {
        while (true) {
            serviceRequests.poll()?.let { (serviceRequest, _) ->
                mainHandler.post {
                    serviceRequest(error)
                }
            } ?: break
        }
    }
}
