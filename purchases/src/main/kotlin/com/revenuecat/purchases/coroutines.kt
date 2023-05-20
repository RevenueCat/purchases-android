package com.revenuecat.purchases

import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Get latest available purchaser info.
 * Coroutine friendly version of [Purchases.getCustomerInfo].
 *
 * @return [Result] type containing either the [CustomerInfo] or the [PurchasesError]
 */
suspend fun Purchases.getCustomerInfoSuspend(): Result<CustomerInfo, PurchasesError> {
    return suspendCoroutine { continuation ->
        val receiveCustomerInfoCallback = object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                continuation.resume(Result.Success(customerInfo))
            }

            override fun onError(error: PurchasesError) {
                continuation.resume(Result.Error(error))
            }
        }
        getCustomerInfo(receiveCustomerInfoCallback)
    }
}
