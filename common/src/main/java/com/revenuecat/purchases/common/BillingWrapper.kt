//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import android.app.Activity
import android.content.Context
import android.os.Handler
import androidx.annotation.UiThread
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.SkuType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.strings.BillingStrings
import com.revenuecat.purchases.strings.OfferingStrings
import com.revenuecat.purchases.strings.PurchaseStrings
import com.revenuecat.purchases.strings.RestoreStrings
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.ConcurrentLinkedQueue

class BillingWrapper(
    private val clientFactory: ClientFactory,
    private val mainHandler: Handler
) : PurchasesUpdatedListener, BillingClientStateListener {

    @get:Synchronized
    @set:Synchronized
    @Volatile
    var stateListener: StateListener? = null

    @get:Synchronized
    @set:Synchronized
    @Volatile
    var billingClient: BillingClient? = null

    @get:Synchronized
    @Volatile
    var purchasesUpdatedListener: PurchasesUpdatedListener? = null
        set(value) {
            synchronized(this@BillingWrapper) {
                field = value
            }
            if (value != null) {
                startConnection()
            } else {
                endConnection()
            }
        }

    private val productTypes = mutableMapOf<String, PurchaseType>()
    private val presentedOfferingsByProductIdentifier = mutableMapOf<String, String?>()

    private val serviceRequests =
        ConcurrentLinkedQueue<(connectionError: PurchasesError?) -> Unit>()

    class ClientFactory(private val context: Context) {
        @UiThread
        fun buildClient(listener: com.android.billingclient.api.PurchasesUpdatedListener): BillingClient {
            return BillingClient.newBuilder(context).enablePendingPurchases().setListener(listener)
                .build()
        }
    }

    interface PurchasesUpdatedListener {
        fun onPurchasesUpdated(purchases: List<PurchaseWrapper>)
        fun onPurchasesFailedToUpdate(
            purchases: List<Purchase>?,
            @BillingClient.BillingResponseCode responseCode: Int,
            message: String
        )
    }

    interface StateListener {
        fun onConnected()
    }

    private fun executePendingRequests() {
        synchronized(this@BillingWrapper) {
            while (billingClient?.isReady == true && !serviceRequests.isEmpty()) {
                serviceRequests.remove().let { mainHandler.post { it(null) } }
            }
        }
    }

    private fun startConnection() {
        mainHandler.post {
            synchronized(this@BillingWrapper) {
                if (billingClient == null) {
                    billingClient = clientFactory.buildClient(this)
                }
                billingClient?.let {
                    log(LogIntent.DEBUG_INFO, BillingStrings.BILLING_CLIENT_STARTING.format(it))
                    it.startConnection(this)
                }
            }
        }
    }

    private fun endConnection() {
        mainHandler.post {
            synchronized(this@BillingWrapper) {
                billingClient?.let {
                    log(LogIntent.DEBUG_INFO, BillingStrings.BILLING_CLIENT_ENDING.format(it))
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
                startConnection()
            } else {
                executePendingRequests()
            }
        }
    }

    fun querySkuDetailsAsync(
        @BillingClient.SkuType itemType: String,
        skuList: List<String>,
        onReceiveSkuDetails: (List<SkuDetails>) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        log(LogIntent.DEBUG_INFO, OfferingStrings.FETCHING_PRODUCTS.format(skuList.joinToString()))
        executeRequestOnUIThread { connectionError ->
            if (connectionError == null) {
                val params = SkuDetailsParams.newBuilder()
                    .setType(itemType).setSkusList(skuList).build()
                withConnectedClient {
                    querySkuDetailsAsync(params) { billingResult, skuDetailsList ->
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            log(LogIntent.DEBUG_INFO, OfferingStrings.FINISHED_FETCHING_PRODUCTS
                                    .format(skuList.joinToString()))
                            log(LogIntent.PURCHASE, OfferingStrings.RETRIEVED_SKU
                                    .format(skuDetailsList?.joinToString { it.toString() }))
                            skuDetailsList?.takeUnless { it.isEmpty() }?.forEach {
                                log(LogIntent.PURCHASE, OfferingStrings.PRODUCTS.format(it.sku, it))
                            }

                            onReceiveSkuDetails(skuDetailsList ?: emptyList())
                        } else {
                            log(LogIntent.GOOGLE_ERROR, OfferingStrings.FETCHING_PRODUCTS_ERROR
                                    .format(billingResult.toHumanReadableDescription()))
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

    fun makePurchaseAsync(
        activity: Activity,
        appUserID: String,
        skuDetails: SkuDetails,
        replaceSkuInfo: ReplaceSkuInfo?,
        presentedOfferingIdentifier: String?
    ) {
        if (replaceSkuInfo != null) {
            log(LogIntent.PURCHASE, PurchaseStrings.UPGRADING_SKU
                    .format(replaceSkuInfo.oldPurchase.sku, skuDetails.sku))
        } else {
            log(LogIntent.PURCHASE, PurchaseStrings.PURCHASING_PRODUCT.format(skuDetails.sku))
        }
        synchronized(this@BillingWrapper) {
            productTypes[skuDetails.sku] = PurchaseType.fromSKUType(skuDetails.type)
            presentedOfferingsByProductIdentifier[skuDetails.sku] = presentedOfferingIdentifier
        }
        executeRequestOnUIThread {
            val params = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                // Causing issues with downgrades/upgrades https://issuetracker.google.com/issues/155005449
                // .setObfuscatedAccountId(appUserID.sha256())
                .apply {
                    replaceSkuInfo?.apply {
                        setOldSku(oldPurchase.sku, oldPurchase.purchaseToken)
                        prorationMode?.let { prorationMode ->
                            setReplaceSkusProrationMode(prorationMode)
                        }
                    }
                }.build()

            launchBillingFlow(activity, params)
        }
    }

    @UiThread
    private fun launchBillingFlow(
        activity: Activity,
        params: BillingFlowParams
    ) {
        withConnectedClient {
            launchBillingFlow(activity, params)
                .takeIf { billingResult -> billingResult?.responseCode != BillingClient.BillingResponseCode.OK }
                ?.let { billingResult ->
                    log(LogIntent.GOOGLE_ERROR, BillingStrings.BILLING_INTENT_FAILED
                            .format(billingResult.toHumanReadableDescription()))
                }
        }
    }

    fun queryPurchaseHistoryAsync(
        @BillingClient.SkuType skuType: String,
        onReceivePurchaseHistory: (List<PurchaseHistoryRecord>) -> Unit,
        onReceivePurchaseHistoryError: (PurchasesError) -> Unit
    ) {
        log(LogIntent.DEBUG_INFO, RestoreStrings.QUERYING_PURCHASE_HISTORY.format(skuType))
        executeRequestOnUIThread { connectionError ->
            if (connectionError == null) {
                withConnectedClient {
                    queryPurchaseHistoryAsync(skuType) { billingResult, purchaseHistoryRecordList ->
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            purchaseHistoryRecordList.takeUnless { it.isNullOrEmpty() }?.forEach {
                                log(LogIntent.DEBUG_INFO, RestoreStrings.PURCHASE_HISTORY_RETRIEVED
                                        .format(it.toHumanReadableDescription()))
                            } ?: log(LogIntent.DEBUG_INFO, RestoreStrings.PURCHASE_HISTORY_EMPTY)
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

    fun queryAllPurchases(
        onReceivePurchaseHistory: (List<PurchaseHistoryRecordWrapper>) -> Unit,
        onReceivePurchaseHistoryError: (PurchasesError) -> Unit
    ) {
        queryPurchaseHistoryAsync(
            SkuType.SUBS,
            { subsPurchasesList ->
                queryPurchaseHistoryAsync(
                    SkuType.INAPP,
                    { inAppPurchasesList ->
                        onReceivePurchaseHistory(
                            subsPurchasesList.map {
                                PurchaseHistoryRecordWrapper(
                                    it,
                                    PurchaseType.SUBS
                                )
                            } +
                                inAppPurchasesList.map {
                                    PurchaseHistoryRecordWrapper(
                                        it,
                                        PurchaseType.INAPP
                                    )
                                }
                        )
                    },
                    onReceivePurchaseHistoryError
                )
            },
            onReceivePurchaseHistoryError
        )
    }

    fun consumePurchase(
        token: String,
        onConsumed: (billingResult: BillingResult, purchaseToken: String) -> Unit
    ) {
        log(LogIntent.RC_SUCCESS, PurchaseStrings.CONSUMING_PURCHASE.format(token))
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

    fun acknowledge(
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

    data class QueryPurchasesResult(
        @BillingClient.BillingResponseCode val responseCode: Int,
        val purchasesByHashedToken: Map<String, PurchaseWrapper>
    ) {
        fun isSuccessful(): Boolean = responseCode == BillingClient.BillingResponseCode.OK
    }

    fun queryPurchases(@SkuType skuType: String): QueryPurchasesResult? {
        return billingClient?.let { billingClient ->
            log(LogIntent.DEBUG_INFO, RestoreStrings.QUERYING_PURCHASE.format(skuType))
            val result = billingClient.queryPurchases(skuType)

            val purchasesList = result.purchasesList ?: emptyList<Purchase>()

            QueryPurchasesResult(
                result.responseCode,
                purchasesList.map { purchase ->
                    val hash = purchase.purchaseToken.sha1()
                    log(LogIntent.DEBUG_INFO, RestoreStrings.QUERYING_PURCHASE_WITH_HASH.format(skuType, hash))
                    hash to PurchaseWrapper(purchase, PurchaseType.fromSKUType(skuType), null)
                }.toMap()
            )
        }
    }

    fun findPurchaseInPurchaseHistory(
        @SkuType skuType: String,
        sku: String,
        completion: (BillingResult, PurchaseHistoryRecordWrapper?) -> Unit
    ) {
        withConnectedClient {
            log(LogIntent.DEBUG_INFO, RestoreStrings.QUERYING_PURCHASE_WITH_TYPE.format(sku, skuType))
            queryPurchaseHistoryAsync(skuType) { result, purchasesList ->
                completion(
                    result,
                    purchasesList?.firstOrNull { sku == it.sku }?.let {
                        PurchaseHistoryRecordWrapper(it, PurchaseType.fromSKUType(skuType))
                    }
                )
            }
        }
    }

    internal fun getPurchaseType(purchaseToken: String): PurchaseType {
        billingClient?.let { client ->
            val querySubsResult = client.queryPurchases(SkuType.SUBS)
            val subsResponseOK = querySubsResult.responseCode == BillingClient.BillingResponseCode.OK
            val subFound = querySubsResult.purchasesList?.any { it.purchaseToken == purchaseToken } ?: false
            if (subsResponseOK && subFound) {
                return@getPurchaseType PurchaseType.SUBS
            }
            val queryInAppsResult = client.queryPurchases(SkuType.INAPP)
            val inAppsResponseIsOK = queryInAppsResult.responseCode == BillingClient.BillingResponseCode.OK
            val inAppFound = queryInAppsResult.purchasesList?.any { it.purchaseToken == purchaseToken } ?: false
            if (inAppsResponseIsOK && inAppFound) {
                return@getPurchaseType PurchaseType.INAPP
            }
        }
        return PurchaseType.UNKNOWN
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: List<Purchase>?
    ) {
        val notNullPurchasesList = purchases ?: emptyList()
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && notNullPurchasesList.isNotEmpty()) {
            notNullPurchasesList.map { purchase ->
                log(LogIntent.DEBUG_INFO, BillingStrings.BILLING_WRAPPER_PURCHASES_UPDATED
                        .format(purchase.toHumanReadableDescription()))
                var type: PurchaseType?
                var presentedOffering: String?
                synchronized(this@BillingWrapper) {
                    type = productTypes[purchase.sku]
                    presentedOffering = presentedOfferingsByProductIdentifier[purchase.sku]
                }
                PurchaseWrapper(
                    purchase,
                    type ?: getPurchaseType(purchase.purchaseToken),
                    presentedOffering
                )
            }.let { mappedPurchases ->
                purchasesUpdatedListener?.onPurchasesUpdated(mappedPurchases)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            // When doing a DEFERRED downgrade, the result is OK, but the list of purchases is null
            purchasesUpdatedListener?.onPurchasesUpdated(emptyList())
        } else {
            log(LogIntent.GOOGLE_ERROR, BillingStrings.BILLING_WRAPPER_PURCHASES_ERROR
                    .format(billingResult.toHumanReadableDescription()) +
                    "${notNullPurchasesList.takeUnless { it.isEmpty() }?.let { purchase ->
                    "Purchases:" + purchase.joinToString(
                        ", ",
                        transform = { it.toHumanReadableDescription() }
                    )
                }}"
            )

            purchasesUpdatedListener?.onPurchasesFailedToUpdate(
                purchases,
                if (purchases == null && billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    BillingClient.BillingResponseCode.ERROR
                } else {
                    billingResult.responseCode
                },
                "Error updating purchases. ${billingResult.toHumanReadableDescription()}"
            )
        }
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                log(LogIntent.DEBUG_INFO, BillingStrings.BILLING_SERVICE_SETUP_FINISHED
                        .format(billingClient?.toString()))
                stateListener?.onConnected()
                executePendingRequests()
            }
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED,
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                val message =
                        BillingStrings.BILLING_UNAVAILABLE.format(billingResult.toHumanReadableDescription())
                log(LogIntent.GOOGLE_INFO, message)
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
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
            BillingClient.BillingResponseCode.USER_CANCELED,
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
            BillingClient.BillingResponseCode.ERROR,
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED,
            BillingClient.BillingResponseCode.SERVICE_TIMEOUT,
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> {
                log(LogIntent.GOOGLE_INFO, BillingStrings.BILLING_CLIENT_ERROR
                        .format(billingResult.toHumanReadableDescription()))
            }
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> {
                // Billing service is already trying to connect. Don't do anything.
            }
        }
    }

    override fun onBillingServiceDisconnected() {
        log(LogIntent.DEBUG_INFO, BillingStrings.BILLING_SERVICE_DISCONNECTED.format(billingClient.toString()))
    }

    fun isConnected(): Boolean = billingClient?.isReady ?: false

    private fun withConnectedClient(receivingFunction: BillingClient.() -> Unit) {
        billingClient?.takeIf { it.isReady }?.let {
            it.receivingFunction()
        } ?: log(LogIntent.GOOGLE_INFO, BillingStrings.BILLING_CLIENT_DISCONNECTED.format(getStackTrace()))
    }

    private fun getStackTrace(): String {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        Throwable().printStackTrace(printWriter)
        return stringWriter.toString()
    }
}
