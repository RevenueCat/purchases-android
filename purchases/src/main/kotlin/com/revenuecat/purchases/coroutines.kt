package com.revenuecat.purchases

import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Get latest available purchaser info.
 * Coroutine friendly version of [Purchases.getCustomerInfo].
 *
 * @return The [CustomerInfo] or a [PurchasesException] with the [PurchasesError]
 */
suspend fun Purchases.awaitCustomerInfo(): CustomerInfo {
    return suspendCoroutine { continuation ->
        val receiveCustomerInfoCallback = object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                continuation.resume(customerInfo)
            }

            override fun onError(error: PurchasesError) {
                continuation.resumeWithException(PurchasesException(error))
            }
        }
        getCustomerInfo(receiveCustomerInfoCallback)
    }
}
