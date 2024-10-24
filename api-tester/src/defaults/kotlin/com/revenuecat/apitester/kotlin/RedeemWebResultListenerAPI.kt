package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.RedeemWebResultListener

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Suppress("unused", "UNUSED_VARIABLE")
private class RedeemWebResultListenerAPI {
    fun checkListener(
        redeemWebPurchaseListener: RedeemWebResultListener,
        result: RedeemWebResultListener.Result,
    ) {
        redeemWebPurchaseListener.handleResult(result)
    }

    fun checkResult(result: RedeemWebResultListener.Result): Boolean {
        val isSuccess: Boolean = result.isSuccess

        when (result) {
            is RedeemWebResultListener.Result.Success -> {
                val customerInfo: CustomerInfo = result.customerInfo
                return true
            }
            is RedeemWebResultListener.Result.Error -> {
                val error: PurchasesError = result.error
                return false
            }
        }
    }
}
