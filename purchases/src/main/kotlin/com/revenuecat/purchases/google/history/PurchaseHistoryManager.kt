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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Manager for querying purchase history using the AIDL-generated stub.
 */
internal class PurchaseHistoryManager(private val context: Context) {
    private var billingService: IInAppBillingService? = null
    private var serviceConnection: ServiceConnection? = null

    private val operationsMutex = Mutex()
    private var connectDeferred: CompletableDeferred<Boolean>? = null
    private val queryDeferreds = mutableMapOf<String, CompletableDeferred<List<StoreTransaction>>>()

    /**
     * Helper function to execute an operation with concurrency control.
     * If an operation is already in progress, subsequent calls will reuse it.
     *
     * @param clearOnCompletion If true, clears the deferred after operation completes.
     *                          If false, keeps it cached (useful for one-time operations like connect).
     */
    private suspend inline fun <T> getOrExecute(
        crossinline getDeferred: () -> CompletableDeferred<T>?,
        crossinline setDeferred: (CompletableDeferred<T>?) -> Unit,
        debugMessage: String,
        clearOnCompletion: Boolean = true,
        crossinline operation: suspend () -> T,
    ): T {
        // Atomically check if operation is in progress or completed, or start a new one
        val (deferred, shouldStart) = operationsMutex.withLock {
            getDeferred()?.let { existing ->
                // Check if it's already completed
                if (existing.isCompleted) {
                    debugLog { "$debugMessage (already completed)" }
                } else {
                    debugLog { debugMessage }
                }
                return@withLock existing to false
            }
            val newDeferred = CompletableDeferred<T>()
            setDeferred(newDeferred)
            newDeferred to true
        }

        if (!shouldStart) {
            return deferred.await()
        }

        return try {
            val result = operation()
            deferred.complete(result)
            result
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            deferred.completeExceptionally(e)
            throw e
        } finally {
            if (clearOnCompletion) {
                operationsMutex.withLock {
                    setDeferred(null)
                }
            }
        }
    }

    /**
     * Connect to the Google Play billing service.
     * This is a suspending function that waits for the connection to be established.
     * If a connection is already in progress or has completed, this will reuse it.
     *
     * @return true if connection was successful, false otherwise
     */
    suspend fun connect(): Boolean = getOrExecute(
        getDeferred = { connectDeferred },
        setDeferred = { connectDeferred = it },
        debugMessage = "Connection already in progress or completed, hooking into existing operation",
        clearOnCompletion = false, // Keep connectDeferred as a cache
    ) {
        suspendCoroutine { continuation ->
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    debugLog { "AIDL Billing service connected" }
                    billingService = IInAppBillingService.Stub.asInterface(service)
                    serviceConnection = this
                    continuation.resume(true)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    debugLog { "AIDL Billing service disconnected" }
                    cleanup()
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
            } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                errorLog(e) { "Error binding to AIDL billing service" }
                continuation.resumeWithException(e)
            }
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
        if (billingService == null) {
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
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
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
     * If a query is already in progress for the same type, this will hook into that
     * operation instead of starting a new one.
     *
     * @param type Product type ("inapp" or "subs")
     * @return List of all purchase history records
     */
    suspend fun queryAllPurchaseHistory(
        type: String = BillingConstants.ITEM_TYPE_INAPP,
    ): List<StoreTransaction> = getOrExecute(
        getDeferred = { queryDeferreds[type] },
        setDeferred = { deferred ->
            if (deferred != null) {
                queryDeferreds[type] = deferred
            } else {
                queryDeferreds.remove(type)
            }
        },
        debugMessage = "Query for type $type already in progress, hooking into existing operation",
    ) {
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

        allRecords.map { it.toStoreTransaction(productType) }
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
     * Clears the cached connection state so subsequent connect() calls will reconnect.
     */
    suspend fun disconnect() {
        operationsMutex.withLock {
            cleanup()
        }
    }

    private fun cleanup() {
        connectDeferred?.cancel()
        queryDeferreds.forEach {
            it.value.cancel()
        }
        serviceConnection?.let { connection ->
            try {
                context.unbindService(connection)
                debugLog { "AIDL Billing service disconnected" }
            } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                errorLog(e) { "Error disconnecting from AIDL Billing service" }
            }
        }
        billingService = null
        serviceConnection = null
        connectDeferred = null
        queryDeferreds.clear()
    }
}
