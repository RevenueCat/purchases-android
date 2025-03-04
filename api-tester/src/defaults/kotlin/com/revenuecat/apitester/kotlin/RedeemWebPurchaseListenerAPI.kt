package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.RedeemWebPurchaseListener

@Suppress("unused", "UNUSED_VARIABLE", "ReturnCount")
private class RedeemWebPurchaseListenerAPI {
    fun checkListener(
        redeemWebPurchaseListener: RedeemWebPurchaseListener,
        result: RedeemWebPurchaseListener.Result,
    ) {
        redeemWebPurchaseListener.handleResult(result)
    }

    fun checkResult(result: RedeemWebPurchaseListener.Result): Boolean {
        val isSuccess: Boolean = result.isSuccess

        when (result) {
            is RedeemWebPurchaseListener.Result.Success -> {
                val customerInfo: CustomerInfo = result.customerInfo
                return true
            }
            is RedeemWebPurchaseListener.Result.Error -> {
                val error: PurchasesError = result.error
                return false
            }
            is RedeemWebPurchaseListener.Result.InvalidToken -> {
                return false
            }
            is RedeemWebPurchaseListener.Result.PurchaseBelongsToOtherUser -> {
                return false
            }
            is RedeemWebPurchaseListener.Result.Expired -> {
                val obfuscatedEmail: String = result.obfuscatedEmail
                return false
            }
        }
    }
}
