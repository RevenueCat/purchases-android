//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

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
import java.util.concurrent.ConcurrentLinkedQueue

internal class BillingWrapper internal constructor(
    private val clientFactory: ClientFactory,
    private val mainHandler: Handler
) : PurchasesUpdatedListener, BillingClientStateListener {

    @get:Synchronized @set:Synchronized
    @Volatile internal var stateListener: StateListener? = null

    @get:Synchronized @set:Synchronized
    @Volatile internal var billingClient: BillingClient? = null

    @get:Synchronized
    @Volatile internal var purchasesUpdatedListener: PurchasesUpdatedListener? = null
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

    private val serviceRequests = ConcurrentLinkedQueue<(connectionError: PurchasesError?) -> Unit>()

    internal class ClientFactory(private val context: Context) {
        @UiThread
        fun buildClient(listener: com.android.billingclient.api.PurchasesUpdatedListener): BillingClient {
            return BillingClient.newBuilder(context).enablePendingPurchases().setListener(listener).build()
        }
    }

    internal interface PurchasesUpdatedListener {
        fun onPurchasesUpdated(purchases: List<PurchaseWrapper>)
        fun onPurchasesFailedToUpdate(
            purchases: List<Purchase>?,
            @BillingClient.BillingResponseCode responseCode: Int,
            message: String
        )
    }

    internal interface StateListener {
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
                    debugLog("Starting connection for $it")
                    it.startConnection(this)
                }
            }
        }
    }

    private fun endConnection() {
        mainHandler.post {
            synchronized(this@BillingWrapper) {
                billingClient?.let {
                    debugLog("Ending connection for $it")
                    it.endConnection()
                }
                billingClient = null
            }
        }
    }

    @Synchronized private fun executeRequestOnUIThread(request: (PurchasesError?) -> Unit) {
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
        onError: (PurchasesError) ->  Unit
    ) {
        debugLog("Requesting products with identifiers: ${skuList.joinToString()}")
        executeRequestOnUIThread { connectionError ->
            if (connectionError == null) {
                val params = SkuDetailsParams.newBuilder()
                    .setType(itemType).setSkusList(skuList).build()
                billingClient?.querySkuDetailsAsync(params) { billingResult, skuDetailsList ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        debugLog("Products request finished for ${skuList.joinToString()}")
                        debugLog("Retrieved skuDetailsList: ${skuDetailsList?.joinToString { it.toString() }}")
                        skuDetailsList?.takeUnless { it.isEmpty() }?.forEach {
                            debugLog("${it.sku} - $it")
                        }

                        onReceiveSkuDetails(skuDetailsList ?: emptyList())
                    } else {
                        log("Error when fetching products. ${billingResult.toHumanReadableDescription()}")
                        onError(
                            billingResult.responseCode.billingResponseToPurchasesError(
                                "Error when fetching products. ${billingResult.toHumanReadableDescription()}"
                            )
                        )
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
        oldSku: String?,
        presentedOfferingIdentifier: String?
    ) {
        if (oldSku != null) {
            debugLog("Upgrading old sku $oldSku with sku: ${skuDetails.sku}")
        } else {
            debugLog("Making purchase for sku: ${skuDetails.sku}")
        }
        synchronized(this@BillingWrapper) {
            productTypes[skuDetails.sku] = PurchaseType.fromSKUType(skuDetails.type)
            presentedOfferingsByProductIdentifier[skuDetails.sku] = presentedOfferingIdentifier
        }
        executeRequestOnUIThread {
            val params = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .setAccountId(appUserID)
                .setOldSku(oldSku).build()

            launchBillingFlow(activity, params)
        }
    }

    @UiThread
    private fun launchBillingFlow(
        activity: Activity,
        params: BillingFlowParams
    ) {
        billingClient?.launchBillingFlow(activity, params)
            .takeIf { billingResult -> billingResult?.responseCode != BillingClient.BillingResponseCode.OK }
            ?.let { billingResult -> log("Failed to launch billing intent. ${billingResult.toHumanReadableDescription()}") }
    }

    fun queryPurchaseHistoryAsync(
        @BillingClient.SkuType skuType: String,
        onReceivePurchaseHistory: (List<PurchaseHistoryRecord>) -> Unit,
        onReceivePurchaseHistoryError: (PurchasesError) -> Unit
    ) {
        debugLog("Querying purchase history for type $skuType")
        executeRequestOnUIThread { connectionError ->
            if (connectionError == null) {
                billingClient?.queryPurchaseHistoryAsync(skuType) { billingResult, purchaseHistoryRecordList ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        purchaseHistoryRecordList.takeUnless { it.isEmpty() }?.forEach {
                            debugLog("Purchase history retrieved ${it.toHumanReadableDescription()}")
                        } ?: debugLog("Purchase history is empty.")
                        onReceivePurchaseHistory(purchaseHistoryRecordList)
                    } else {
                        onReceivePurchaseHistoryError(billingResult.responseCode.billingResponseToPurchasesError(
                            "Error receiving purchase history. ${billingResult.toHumanReadableDescription()}"
                        ))
                    }
                }
            } else {
                onReceivePurchaseHistoryError(connectionError)
            }
        }
    }

    fun queryAllPurchases(
        onReceivePurchaseHistory: (List<PurchaseWrapper>) -> Unit,
        onReceivePurchaseHistoryError: (PurchasesError) -> Unit
    ) {
        queryPurchaseHistoryAsync(
            SkuType.SUBS,
            { subsPurchasesList ->
                queryPurchaseHistoryAsync(
                    SkuType.INAPP,
                    { inAppPurchasesList ->
                        onReceivePurchaseHistory(
                            subsPurchasesList.map { PurchaseWrapper(it, PurchaseType.SUBS) } +
                                inAppPurchasesList.map { PurchaseWrapper(it, PurchaseType.INAPP) }
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
        debugLog("Consuming purchase with token $token")
        executeRequestOnUIThread { connectionError ->
            if (connectionError == null) {
                billingClient?.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(token).build(), onConsumed)
            }
        }
    }

    fun acknowledge(
        token: String,
        onAcknowledged: (billingResult: BillingResult, purchaseToken: String) -> Unit
    ) {
        debugLog("Acknowledging purchase with token $token")
        executeRequestOnUIThread { connectionError ->
            if (connectionError == null) {
                billingClient?.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder().setPurchaseToken(token).build()
                ) { billingResult ->
                    onAcknowledged(billingResult, token)
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
        return billingClient?.let {
            debugLog("[QueryPurchases] Querying $skuType")
            val result = it.queryPurchases(skuType)

            // Purchases.PurchaseResult#purchasesList is not marked as nullable, but it does sometimes return null.
            val purchasesList = result.purchasesList ?: emptyList<Purchase>()

            QueryPurchasesResult(
                result.responseCode,
                purchasesList.map { purchase ->
                    val hash = purchase.purchaseToken.sha1()
                    debugLog("[QueryPurchases] Purchase of type $skuType with hash $hash")
                    hash to PurchaseWrapper(purchase, PurchaseType.fromSKUType(skuType), null)
                }.toMap()
            )
        }
    }

    private fun getPurchaseType(purchaseToken: String): PurchaseType {
        billingClient?.let { client ->
            val querySubsResult = client.queryPurchases(SkuType.SUBS)
            if (querySubsResult.responseCode == BillingClient.BillingResponseCode.OK && querySubsResult.purchasesList.any { it.purchaseToken == purchaseToken }) {
                return PurchaseType.SUBS
            }
            val queryINAPPsResult = client.queryPurchases(SkuType.INAPP)
            if (queryINAPPsResult.responseCode == BillingClient.BillingResponseCode.OK && queryINAPPsResult.purchasesList.any { it.purchaseToken == purchaseToken }) {
                return PurchaseType.INAPP
            }
        }
        return PurchaseType.UNKNOWN
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: List<Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.map { purchase ->
                debugLog("BillingWrapper purchases updated. ${purchase.toHumanReadableDescription()}")
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
        } else {
            debugLog("BillingWrapper purchases failed to update. ${billingResult.toHumanReadableDescription()}" +
                "${purchases?.takeUnless { it.isEmpty() }?.let { purchase ->
                    "Purchases:" + purchase.joinToString(
                        ", ",
                        transform = { it.toHumanReadableDescription() }
                    )
                }}"
            )

            purchasesUpdatedListener?.onPurchasesFailedToUpdate(
                purchases,
                if (purchases == null && billingResult.responseCode == BillingClient.BillingResponseCode.OK)
                    BillingClient.BillingResponseCode.ERROR
                else
                    billingResult.responseCode,
                "Error updating purchases. ${billingResult.toHumanReadableDescription()}"
            )
        }

    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        when(billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                debugLog("Billing Service Setup finished for ${billingClient?.toString()}.")
                stateListener?.onConnected()
                executePendingRequests()
            }
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED,
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                val message =
                    "Billing is not available in this device. ${billingResult.toHumanReadableDescription()}"
                log(message)
                // The calls will fail with an error that will be surfaced. We want to surface these errors
                // Can't call executePendingRequests because it will not do anything since it checks for isReady()
                synchronized(this@BillingWrapper) {
                    while (!serviceRequests.isEmpty()) {
                        serviceRequests.remove().let { serviceRequest ->
                            mainHandler.post {
                                serviceRequest(billingResult.responseCode.billingResponseToPurchasesError(message))
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
                log("Billing Service Setup finished with error code: ${billingResult.toHumanReadableDescription()}")
            }
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> {
                // Billing service is already trying to connect. Don't do anything.
            }
        }
    }

    override fun onBillingServiceDisconnected() {
        debugLog("Billing Service disconnected for ${billingClient?.toString()}")
    }

    fun isConnected(): Boolean = billingClient?.isReady ?: false
}
