package com.revenuecat.rcttester.purchasing

import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Helper class for acknowledging and consuming purchases when purchasesAreCompletedBy
 * is set to MY_APP.
 *
 * When `finishTransactions == false`, the RevenueCat SDK does NOT acknowledge or consume
 * purchases. Google will auto-refund unacknowledged purchases after 3 days, so the app
 * must handle this itself.
 */
class BillingClientAcknowledgeHelper(private val billingClient: BillingClient) {

    /**
     * Acknowledges a subscription purchase. Must be called for subscriptions
     * to prevent Google from auto-refunding after 3 days.
     */
    suspend fun acknowledgePurchase(purchaseToken: String): Boolean {
        if (!ensureConnected()) {
            Log.e(TAG, "Failed to connect BillingClient for acknowledge")
            return false
        }
        return suspendCoroutine { continuation ->
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Acknowledged purchase: $purchaseToken")
                    continuation.resume(true)
                } else {
                    Log.e(
                        TAG,
                        "Failed to acknowledge purchase: ${billingResult.responseCode} " +
                            "- ${billingResult.debugMessage}",
                    )
                    continuation.resume(false)
                }
            }
        }
    }

    /**
     * Consumes a consumable in-app purchase. Consuming both acknowledges the purchase
     * and allows it to be purchased again.
     */
    suspend fun consumePurchase(purchaseToken: String): Boolean {
        if (!ensureConnected()) {
            Log.e(TAG, "Failed to connect BillingClient for consume")
            return false
        }
        return suspendCoroutine { continuation ->
            val params = ConsumeParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()
            billingClient.consumeAsync(params) { billingResult, _ ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Consumed purchase: $purchaseToken")
                    continuation.resume(true)
                } else {
                    Log.e(
                        TAG,
                        "Failed to consume purchase: ${billingResult.responseCode} " +
                            "- ${billingResult.debugMessage}",
                    )
                    continuation.resume(false)
                }
            }
        }
    }

    private suspend fun ensureConnected(): Boolean {
        if (billingClient.isReady) return true
        return suspendCoroutine { continuation ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    continuation.resume(
                        billingResult.responseCode == BillingClient.BillingResponseCode.OK,
                    )
                }

                override fun onBillingServiceDisconnected() {
                    // Will retry connection on next call
                }
            })
        }
    }

    companion object {
        private const val TAG = "BillingAcknowledgeHelper"
    }
}
