package com.revenuecat.purchases.google.history

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import com.android.vending.billing.IInAppBillingService
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.models.StoreTransaction
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Manager for querying purchase history using the AIDL-generated stub.
 * This is the clean implementation that uses the IInAppBillingService.aidl interface.
 *
 * NOTE: This currently encounters a BadParcelableException due to Google Play writing
 * extra bytes (12) after the Bundle response. See PurchaseHistoryManagerDirect for
 * a working workaround that bypasses AIDL validation.
 *
 * This provides access to the deprecated getPurchaseHistory() method that was
 * removed from Play Billing Library 8.0.0 but is still supported by Google Play.
 */
internal class PurchaseHistoryManager(private val context: Context) {
    private var billingService: IInAppBillingService? = null

    // WIP: Handle concurrency.
    private var serviceConnected = false
    private var serviceConnection: ServiceConnection? = null

    /**
     * Connect to the Google Play billing service.
     * This is a suspending function that waits for the connection to be established.
     *
     * @return true if connection was successful, false otherwise
     */
    suspend fun connect(): Boolean = suspendCancellableCoroutine { continuation ->
        if (serviceConnected) {
            continuation.resume(true)
            return@suspendCancellableCoroutine
        }

        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                debugLog { "AIDL Billing service connected" }
                billingService = IInAppBillingService.Stub.asInterface(service)
                serviceConnected = true
                serviceConnection = this
                continuation.resume(true)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                debugLog { "AIDL Billing service disconnected" }
                billingService = null
                serviceConnected = false
            }
        }

        try {
            val serviceIntent = Intent(BillingConstants.BILLING_SERVICE_ACTION).apply {
                setPackage(BillingConstants.VENDING_PACKAGE)
            }

            val bound = context.bindService(
                serviceIntent,
                connection,
                Context.BIND_AUTO_CREATE,
            )

            if (!bound) {
                errorLog { "Failed to bind to AIDL billing service" }
                continuation.resumeWithException(
                    Exception("Failed to bind to Google Play billing service"),
                )
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            errorLog(e) { "Error binding to AIDL billing service" }
            continuation.resumeWithException(e)
        }

        continuation.invokeOnCancellation {
            disconnect()
        }
    }

    /**
     * Query purchase history for the specified product type.
     *
     * @param type Product type ("inapp" or "subs")
     * @param continuationToken Token for pagination (null for first request)
     * @return PurchaseHistoryResult containing the response
     */
    private fun queryPurchaseHistory(
        type: String = BillingConstants.ITEM_TYPE_INAPP,
        continuationToken: String? = null,
    ): PurchaseHistoryResult {
        if (!serviceConnected || billingService == null) {
            return PurchaseHistoryResult(
                responseCode = BillingConstants.BILLING_RESPONSE_RESULT_SERVICE_UNAVAILABLE,
                records = emptyList(),
                continuationToken = null,
            )
        }

        return try {
            // Create empty bundle for extra params
            val extraParams = Bundle()

            debugLog {
                "Calling getPurchaseHistory via AIDL with " +
                    "API version ${BillingConstants.BILLING_API_VERSION}, type=$type"
            }

            // Call the AIDL-generated method
            val response = billingService!!.getPurchaseHistory(
                BillingConstants.BILLING_API_VERSION,
                context.packageName,
                type,
                continuationToken,
                extraParams,
            )

            parseResponse(response)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            errorLog(e) { "Error querying purchase history via AIDL" }
            PurchaseHistoryResult(
                responseCode = BillingConstants.BILLING_RESPONSE_RESULT_ERROR,
                records = emptyList(),
                continuationToken = null,
            )
        }
    }

    /**
     * Query all purchase history, handling pagination automatically.
     *
     * @param type Product type ("inapp" or "subs")
     * @return List of all purchase history records
     */
    suspend fun queryAllPurchaseHistory(
        type: String = BillingConstants.ITEM_TYPE_INAPP,
    ): List<StoreTransaction> {
        val allRecords = mutableListOf<PurchaseHistoryRecord>()
        var continuationToken: String? = null

        do {
            val result = queryPurchaseHistory(type, continuationToken)

            if (!result.isSuccess()) {
                errorLog { "Error querying purchase history through AIDL: ${result.getResponseCodeString()}" }
                break
            }

            allRecords.addAll(result.records)
            continuationToken = result.continuationToken

            debugLog { "Retrieved ${result.records.size} records from AIDL queryPurchaseHistory" }
        } while (continuationToken != null)

        val productType = if (type == BillingConstants.ITEM_TYPE_SUBS) {
            ProductType.SUBS
        } else {
            ProductType.INAPP
        }

        return allRecords.map { it.toStoreTransaction(productType) }
    }

    /**
     * Parse the bundle response from the billing service.
     */
    private fun parseResponse(response: Bundle): PurchaseHistoryResult {
        val responseCode = response.getInt(BillingConstants.RESPONSE_CODE, -1)

        if (responseCode != BillingConstants.BILLING_RESPONSE_RESULT_OK) {
            warnLog {
                "Purchase history query returned non-OK response: ${BillingConstants.getResponseCodeString(
                    responseCode,
                )}"
            }
            return PurchaseHistoryResult(
                responseCode = responseCode,
                records = emptyList(),
                continuationToken = null,
            )
        }

        val purchaseDataList = response.getStringArrayList(BillingConstants.INAPP_PURCHASE_DATA_LIST) ?: ArrayList()
        val signatureList = response.getStringArrayList(BillingConstants.INAPP_DATA_SIGNATURE_LIST) ?: ArrayList()
        val continuationToken = response.getString(BillingConstants.INAPP_CONTINUATION_TOKEN)

        val records = purchaseDataList.zip(signatureList).mapNotNull { (purchaseJson, signature) ->
            val purchaseData = PurchaseData.fromJson(purchaseJson)
            if (purchaseData != null) {
                PurchaseHistoryRecord(
                    purchaseData = purchaseData,
                    signature = signature,
                    rawJson = purchaseJson,
                )
            } else {
                warnLog { "Failed to parse purchase data: $purchaseJson" }
                null
            }
        }

        debugLog { "Parsed ${records.size} purchase history records from AIDL." }
        return PurchaseHistoryResult(
            responseCode = responseCode,
            records = records,
            continuationToken = continuationToken,
        )
    }

    /**
     * Disconnect from the billing service.
     */
    fun disconnect() {
        if (serviceConnected) {
            serviceConnection?.let { connection ->
                try {
                    context.unbindService(connection)
                    debugLog { "AIDL Billing service disconnected" }
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    errorLog(e) { "Error disconnecting from AIDL Billing service" }
                }
            }
            serviceConnected = false
            billingService = null
            serviceConnection = null
        }
    }
}
