package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.RestoreByOrderIdListener

@Suppress("unused", "UNUSED_VARIABLE", "ReturnCount")
private class RestoreByOrderIdListenerAPI {
    fun checkListener(
        restoreByOrderIdListener: RestoreByOrderIdListener,
        result: RestoreByOrderIdListener.Result,
    ) {
        restoreByOrderIdListener.handleResult(result)
    }

    fun checkResult(result: RestoreByOrderIdListener.Result): Boolean {
        val isSuccess: Boolean = result.isSuccess

        when (result) {
            is RestoreByOrderIdListener.Result.Success -> {
                val customerInfo: CustomerInfo = result.customerInfo
                return true
            }
            is RestoreByOrderIdListener.Result.Error -> {
                val error: PurchasesError = result.error
                return false
            }
            is RestoreByOrderIdListener.Result.RateLimitExceeded -> {
                return false
            }
            is RestoreByOrderIdListener.Result.OrderIdNotFound -> {
                return false
            }
            is RestoreByOrderIdListener.Result.OrderNotEligible -> {
                return false
            }
            is RestoreByOrderIdListener.Result.FeatureNotEnabled -> {
                return false
            }
            is RestoreByOrderIdListener.Result.PurchaseBelongsToAuthenticatedUser -> {
                return false
            }
            else -> return false
        }
    }
}
