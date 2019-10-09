//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.app.Activity
import android.content.Context
import android.os.Handler
import androidx.annotation.UiThread
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.SkuType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import java.util.concurrent.ConcurrentLinkedQueue

internal class BillingWrapper internal constructor(
    private val clientFactory: ClientFactory,
    private val mainHandler: Handler
) : PurchasesUpdatedListener, BillingClientStateListener {

    @Volatile internal var stateListener: StateListener? = null
        @Synchronized get() = field
        @Synchronized set(value) {
            field = value
        }

    @Volatile internal var billingClient: BillingClient? = null
        @Synchronized get() = field
        @Synchronized set(value) {
            field = value
        }
    @Volatile internal var purchasesUpdatedListener: PurchasesUpdatedListener? = null
        @Synchronized get() = field
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

    private val productTypes = mutableMapOf<String, String>()

    private val serviceRequests = ConcurrentLinkedQueue<(connectionError: PurchasesError?) -> Unit>()

    internal class ClientFactory(private val context: Context) {
        @UiThread
        fun buildClient(listener: com.android.billingclient.api.PurchasesUpdatedListener): BillingClient {
            return BillingClient.newBuilder(context).setListener(listener).build()
        }
    }

    internal interface PurchasesUpdatedListener {
        fun onPurchasesUpdated(purchases: List<PurchaseWrapper>)
        fun onPurchasesFailedToUpdate(
            purchases: List<Purchase>?,
            @BillingClient.BillingResponse responseCode: Int,
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
                billingClient?.querySkuDetailsAsync(params) { responseCode, skuDetailsList ->
                    if (responseCode == BillingClient.BillingResponse.OK) {
                        debugLog("Products request finished for ${skuList.joinToString()}")
                        debugLog("Retrieved skuDetailsList: ${skuDetailsList?.joinToString { it.toString() }}")
                        skuDetailsList?.takeUnless { it.isEmpty() }?.forEach {
                            debugLog("${it.sku} - $it")
                        }

                        onReceiveSkuDetails(skuDetailsList ?: emptyList())
                    } else {
                        log("Error ${responseCode.getBillingResponseCodeName()} when fetching products.")
                        onError(responseCode.billingResponseToPurchasesError("Error ${responseCode.getBillingResponseCodeName()} when fetching products."))
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
            upgradeInfo: UpgradeInfo?
    ) {
        if (upgradeInfo != null) {
            debugLog("Upgrading old sku ${upgradeInfo.oldSku} with sku: ${skuDetails.sku}")
        } else {
            debugLog("Making purchase for sku: ${skuDetails.sku}")
        }
        synchronized(this@BillingWrapper) {
            productTypes[skuDetails.sku] = skuDetails.type
        }
        executeRequestOnUIThread {
            val params = BillingFlowParams.newBuilder()
                    .setSkuDetails(skuDetails)
                    .setAccountId(appUserID)
                    .setOldSku(upgradeInfo?.oldSku)
                    .apply {
                        upgradeInfo?.prorationMode?.let { prorationMode ->
                            setReplaceSkusProrationMode(prorationMode)
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
        billingClient?.launchBillingFlow(activity, params)
            .takeIf { response -> response != BillingClient.BillingResponse.OK}
            ?.let { response -> log("Failed to launch billing intent $response") }
    }

    fun queryPurchaseHistoryAsync(
        @BillingClient.SkuType skuType: String,
        onReceivePurchaseHistory: (List<Purchase>) -> Unit,
        onReceivePurchaseHistoryError: (PurchasesError) -> Unit
    ) {
        debugLog("Querying purchase history for type $skuType")
        executeRequestOnUIThread { connectionError ->
            if (connectionError == null) {
                billingClient?.queryPurchaseHistoryAsync(skuType) { responseCode, purchasesList ->
                    if (responseCode == BillingClient.BillingResponse.OK) {
                        purchasesList.takeUnless { it.isEmpty() }?.forEach {
                            debugLog("Purchase history retrieved ${it.toHumanReadableDescription()}")
                        } ?: debugLog("Purchase history is empty.")
                        onReceivePurchaseHistory(purchasesList)
                    } else {
                        onReceivePurchaseHistoryError(responseCode.billingResponseToPurchasesError(
                            "Error receiving purchase history ${responseCode.getBillingResponseCodeName()}"
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
                            subsPurchasesList.map { PurchaseWrapper(it, SkuType.SUBS) } +
                                inAppPurchasesList.map { PurchaseWrapper(it, SkuType.INAPP) }
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
        onConsumed: (responseCode: Int, purchaseToken: String) -> Unit
    ) {
        debugLog("Consuming purchase with token $token")
        executeRequestOnUIThread { connectionError ->
            if (connectionError == null) {
                billingClient?.consumeAsync(token) { responseCode, purchaseToken -> onConsumed(responseCode, purchaseToken) }
            }
        }
    }

    data class QueryPurchasesResult(
        @BillingClient.BillingResponse val responseCode: Int,
        val purchasesByHashedToken: Map<String, PurchaseWrapper>
    ) {
        fun isSuccessful(): Boolean = responseCode == BillingClient.BillingResponse.OK
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
                    hash to PurchaseWrapper(purchase, skuType)
                }.toMap()
            )
        }
    }

    override fun onPurchasesUpdated(@BillingClient.BillingResponse responseCode: Int, purchases: List<Purchase>?) {
        if (responseCode == BillingClient.BillingResponse.OK && purchases != null) {
            purchases.map { purchase ->
                debugLog("BillingWrapper purchases updated: ${purchase.toHumanReadableDescription()}")
                var type: String?
                synchronized(this@BillingWrapper) {
                    type = productTypes[purchase.sku]
                }
                PurchaseWrapper(purchase, type)
            }.let { mappedPurchases ->
                purchasesUpdatedListener?.onPurchasesUpdated(mappedPurchases)
            }
        } else {
            debugLog("BillingWrapper purchases failed to update: responseCode ${responseCode.getBillingResponseCodeName()}" +
                    "${purchases?.takeUnless { it.isEmpty() }?.let { purchase ->
                        "Purchases:" + purchase.joinToString(
                            ", ",
                            transform = { it.toHumanReadableDescription() }
                        )
                    }}"
            )

            purchasesUpdatedListener?.onPurchasesFailedToUpdate(
                purchases,
                if (purchases == null && responseCode == BillingClient.BillingResponse.OK)
                    BillingClient.BillingResponse.ERROR
                else
                    responseCode,
                "Error updating purchases ${responseCode.getBillingResponseCodeName()}"
            )
        }
    }

    override fun onBillingSetupFinished(@BillingClient.BillingResponse responseCode: Int) {
        when(responseCode) {
            BillingClient.BillingResponse.OK -> {
                debugLog("Billing Service Setup finished for ${billingClient?.toString()}.")
                stateListener?.onConnected()
                executePendingRequests()
            }
            BillingClient.BillingResponse.FEATURE_NOT_SUPPORTED,
            BillingClient.BillingResponse.BILLING_UNAVAILABLE -> {
                log("Billing is not available in this device. ${responseCode.getBillingResponseCodeName()}")
                // The calls will fail with an error that will be surfaced. We want to surface these errors
                // Can't call executePendingRequests because it will not do anything since it checks for isReady()
                synchronized(this@BillingWrapper) {
                    while (!serviceRequests.isEmpty()) {
                        serviceRequests.remove()
                            .let { mainHandler.post { it(responseCode.billingResponseToPurchasesError("Billing is not available in this device. ${responseCode.getBillingResponseCodeName()}")) } }
                    }
                }
            }
            BillingClient.BillingResponse.SERVICE_DISCONNECTED,
            BillingClient.BillingResponse.USER_CANCELED,
            BillingClient.BillingResponse.SERVICE_UNAVAILABLE,
            BillingClient.BillingResponse.ITEM_UNAVAILABLE,
            BillingClient.BillingResponse.ERROR,
            BillingClient.BillingResponse.ITEM_ALREADY_OWNED,
            BillingClient.BillingResponse.SERVICE_TIMEOUT,
            BillingClient.BillingResponse.ITEM_NOT_OWNED -> {
                log("Billing Service Setup finished with error code: ${responseCode.getBillingResponseCodeName()}")
            }
            BillingClient.BillingResponse.DEVELOPER_ERROR -> {
                // Billing service is already trying to connect. Don't do anything.
            }
        }
    }

    override fun onBillingServiceDisconnected() {
        debugLog("Billing Service disconnected for ${billingClient?.toString()}")
    }

    fun isConnected(): Boolean = billingClient?.isReady ?: false
}
