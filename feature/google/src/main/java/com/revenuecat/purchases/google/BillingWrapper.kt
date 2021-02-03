//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.google

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
import com.android.billingclient.api.SkuDetailsParams
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.ProductDetailsListCallback
import com.revenuecat.purchases.common.PurchaseHistoryRecordWrapper
import com.revenuecat.purchases.common.PurchaseWrapper
import com.revenuecat.purchases.common.ReplaceSkuInfo
import com.revenuecat.purchases.common.billingResponseToPurchasesError
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.isSuccessful
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.common.toHumanReadableDescription
import com.revenuecat.purchases.common.toRevenueCatPurchaseDetails
import com.revenuecat.purchases.models.ProductDetails
import com.revenuecat.purchases.models.PurchaseDetails
import com.revenuecat.purchases.models.RevenueCatPurchaseState
import com.revenuecat.purchases.models.RevenueCatPurchaseState
import com.revenuecat.purchases.models.skuDetails
import com.revenuecat.purchases.strings.BillingStrings
import com.revenuecat.purchases.strings.OfferingStrings
import com.revenuecat.purchases.strings.PurchaseStrings
import com.revenuecat.purchases.strings.RestoreStrings
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.ConcurrentLinkedQueue

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

    override fun startConnection() {
        mainHandler.post {
            synchronized(this@BillingWrapper) {
                if (billingClient == null) {
                    billingClient = clientFactory.buildClient(this)
                }
                billingClient?.let {
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
                startConnection()
            } else {
                executePendingRequests()
            }
        }
    }

    override fun querySkuDetailsAsync(
        productType: ProductType,
        skus: Set<String>,
        onReceive: ProductDetailsListCallback,
        onError: PurchasesErrorCallback
    ) {
        log(LogIntent.DEBUG, OfferingStrings.FETCHING_PRODUCTS.format(skus.joinToString()))
        executeRequestOnUIThread { connectionError ->
            if (connectionError == null) {
                val params = SkuDetailsParams.newBuilder()
                    .setType(productType.toSKUType() ?: SkuType.INAPP)
                    .setSkusList(skus.toList()).build()

                withConnectedClient {
                    querySkuDetailsAsync(params) { billingResult, skuDetailsList ->
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            log(LogIntent.DEBUG, OfferingStrings.FETCHING_PRODUCTS_FINISHED
                                    .format(skus.joinToString()))
                            log(LogIntent.PURCHASE, OfferingStrings.RETRIEVED_PRODUCTS
                                    .format(skuDetailsList?.joinToString { it.toString() }))
                            skuDetailsList?.takeUnless { it.isEmpty() }?.forEach {
                                log(LogIntent.PURCHASE, OfferingStrings.LIST_PRODUCTS.format(it.sku, it))
                            }

                            onReceive(skuDetailsList?.map { it.toProductDetails() } ?: emptyList())
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

    override fun makePurchaseAsync(
        activity: Activity,
        appUserID: String,
        productDetails: ProductDetails,
        replaceSkuInfo: ReplaceSkuInfo?,
        presentedOfferingIdentifier: String?
    ) {
        if (replaceSkuInfo != null) {
            log(LogIntent.PURCHASE, PurchaseStrings.UPGRADING_SKU
                    .format(replaceSkuInfo.oldPurchase.sku, productDetails.sku))
        } else {
            log(LogIntent.PURCHASE, PurchaseStrings.PURCHASING_PRODUCT.format(productDetails.sku))
        }
        synchronized(this@BillingWrapper) {
            productTypes[productDetails.sku] = productDetails.type
            presentedOfferingsByProductIdentifier[productDetails.sku] = presentedOfferingIdentifier
        }
        executeRequestOnUIThread {
            val params = BillingFlowParams.newBuilder()
                .setSkuDetails(productDetails.skuDetails)
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
                .takeIf { billingResult -> billingResult.responseCode != BillingClient.BillingResponseCode.OK }
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
        log(LogIntent.DEBUG, RestoreStrings.QUERYING_PURCHASE_HISTORY.format(skuType))
        executeRequestOnUIThread { connectionError ->
            if (connectionError == null) {
                withConnectedClient {
                    queryPurchaseHistoryAsync(skuType) { billingResult, purchaseHistoryRecordList ->
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            purchaseHistoryRecordList.takeUnless { it.isNullOrEmpty() }?.forEach {
                                log(LogIntent.RC_PURCHASE_SUCCESS, RestoreStrings.PURCHASE_HISTORY_RETRIEVED
                                        .format(it.toHumanReadableDescription()))
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
        onReceivePurchaseHistory: (List<PurchaseDetails>) -> Unit,
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
                                it.toRevenueCatPurchaseDetails(ProductType.SUBS)
                            } + inAppPurchasesList.map {
                                it.toRevenueCatPurchaseDetails(ProductType.INAPP)
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
        purchase: PurchaseDetails
    ) {
        if (purchase.type == ProductType.UNKNOWN) {
            // Would only get here if the purchase was triggered from outside of the app and there was
            // an issue getting the purchase type
            return
        }
        if (purchase.purchaseState == RevenueCatPurchaseState.PENDING) {
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

    private fun Purchase.PurchasesResult.isSuccessful() = responseCode == BillingClient.BillingResponseCode.OK

    override fun queryPurchases(
        appUserID: String,
        onSuccess: (Map<String, PurchaseWrapper>) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        withConnectedClient {
            log(LogIntent.DEBUG, RestoreStrings.QUERYING_PURCHASE)

            val queryActiveSubscriptionsResult = this.queryPurchases(SkuType.SUBS)
            if (!queryActiveSubscriptionsResult.isSuccessful()) {
                val billingResult = queryActiveSubscriptionsResult.billingResult
                val purchasesError = billingResult.responseCode.billingResponseToPurchasesError(
                    RestoreStrings.QUERYING_SUBS_ERROR.format(billingResult.toHumanReadableDescription())
                )
                onError(purchasesError)
                return@withConnectedClient
            }

            val queryUnconsumedInAppsResult = this.queryPurchases(SkuType.INAPP)
            if (!queryUnconsumedInAppsResult.isSuccessful()) {
                val billingResult = queryUnconsumedInAppsResult.billingResult
                val purchasesError = billingResult.responseCode.billingResponseToPurchasesError(
                    RestoreStrings.QUERYING_INAPP_ERROR.format(billingResult.toHumanReadableDescription())
                )
                onError(purchasesError)
                return@withConnectedClient
            }

            val activeSubscriptionsList = queryActiveSubscriptionsResult.purchasesList ?: emptyList<Purchase>()
            val mapOfActiveSubscriptions = activeSubscriptionsList.toMapOfGooglePurchaseWrapper(SkuType.SUBS)

            val unconsumedInAppsList = queryUnconsumedInAppsResult.purchasesList ?: emptyList<Purchase>()
            val mapOfUnconsumedInApps = unconsumedInAppsList.toMapOfGooglePurchaseWrapper(SkuType.INAPP)

            onSuccess(mapOfActiveSubscriptions + mapOfUnconsumedInApps)
        }
    }

    private fun List<Purchase>.toMapOfGooglePurchaseWrapper(
        @SkuType skuType: String
    ): Map<String, PurchaseDetails> {
        return this.map { purchase ->
            val hash = purchase.purchaseToken.sha1()
            hash to purchase.toRevenueCatPurchaseDetails(skuType.toProductType(), presentedOfferingIdentifier = null)
        }.toMap()
    }

    override fun findPurchaseInPurchaseHistory(
        appUserID: String,
        productType: ProductType,
        sku: String,
        onCompletion: (PurchaseDetails) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        withConnectedClient {
            log(LogIntent.DEBUG, RestoreStrings.QUERYING_PURCHASE_WITH_TYPE.format(sku, productType.name))
            productType.toSKUType()?.let { skuType ->
                queryPurchaseHistoryAsync(skuType) { result, purchasesList ->
                    if (result.isSuccessful()) {
                        val purchaseHistoryRecordWrapper =
                            purchasesList?.firstOrNull { sku == it.sku }?.let { purchaseHistoryRecord ->
                                purchaseHistoryRecord.toRevenueCatPurchaseDetails(productType)
                            }

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

    internal fun getPurchaseType(purchaseToken: String): ProductType {
        billingClient?.let { client ->
            val querySubsResult = client.queryPurchases(SkuType.SUBS)
            val subsResponseOK = querySubsResult.responseCode == BillingClient.BillingResponseCode.OK
            val subFound = querySubsResult.purchasesList?.any { it.purchaseToken == purchaseToken } ?: false
            if (subsResponseOK && subFound) {
                return@getPurchaseType ProductType.SUBS
            }
            val queryInAppsResult = client.queryPurchases(SkuType.INAPP)
            val inAppsResponseIsOK = queryInAppsResult.responseCode == BillingClient.BillingResponseCode.OK
            val inAppFound = queryInAppsResult.purchasesList?.any { it.purchaseToken == purchaseToken } ?: false
            if (inAppsResponseIsOK && inAppFound) {
                return@getPurchaseType ProductType.INAPP
            }
        }
        return ProductType.UNKNOWN
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: List<Purchase>?
    ) {
        val notNullPurchasesList = purchases ?: emptyList()
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && notNullPurchasesList.isNotEmpty()) {
            notNullPurchasesList.map { purchase ->
                log(LogIntent.DEBUG, BillingStrings.BILLING_WRAPPER_PURCHASES_UPDATED
                        .format(purchase.toHumanReadableDescription()))
                var type: ProductType?
                var presentedOffering: String?
                synchronized(this@BillingWrapper) {
                    type = productTypes[purchase.sku]
                    presentedOffering = presentedOfferingsByProductIdentifier[purchase.sku]
                }
                purchase.toRevenueCatPurchaseDetails(
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
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                log(LogIntent.DEBUG, BillingStrings.BILLING_SERVICE_SETUP_FINISHED
                        .format(billingClient?.toString()))
                stateListener?.onConnected()
                executePendingRequests()
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
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
            BillingClient.BillingResponseCode.USER_CANCELED,
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
            BillingClient.BillingResponseCode.ERROR,
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED,
            BillingClient.BillingResponseCode.SERVICE_TIMEOUT,
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> {
                log(LogIntent.GOOGLE_WARNING, BillingStrings.BILLING_CLIENT_ERROR
                        .format(billingResult.toHumanReadableDescription()))
            }
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> {
                // Billing service is already trying to connect. Don't do anything.
            }
        }
    }

    override fun onBillingServiceDisconnected() {
        log(LogIntent.DEBUG, BillingStrings.BILLING_SERVICE_DISCONNECTED.format(billingClient.toString()))
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
}
