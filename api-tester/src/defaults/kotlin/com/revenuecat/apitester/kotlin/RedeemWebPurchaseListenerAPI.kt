package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.RedeemWebPurchaseListener

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Suppress("unused", "UNUSED_VARIABLE")
private class RedeemWebPurchaseListenerAPI {
    fun checkListener(
        redeemWebPurchaseListener: RedeemWebPurchaseListener,
        webPurchaseRedeemer: RedeemWebPurchaseListener.WebPurchaseRedeemer,
    ) {
        redeemWebPurchaseListener.handleWebPurchaseRedemption(webPurchaseRedeemer)
    }

    fun checkRedemptionStarter(
        webPurchaseRedeemer: RedeemWebPurchaseListener.WebPurchaseRedeemer,
        resultListener: RedeemWebPurchaseListener.ResultListener,
    ) {
        webPurchaseRedeemer.redeemWebPurchase(resultListener)
    }

    fun checkResultListener(
        resultListener: RedeemWebPurchaseListener.ResultListener,
        result: RedeemWebPurchaseListener.Result,
    ) {
        resultListener.handleResult(result)
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
        }
    }

    fun checkPurchases(
        purchases: Purchases,
        redeemWebPurchaseListener: RedeemWebPurchaseListener,
    ) {
        purchases.redeemWebPurchaseListener = redeemWebPurchaseListener
        val getListener: RedeemWebPurchaseListener? = purchases.redeemWebPurchaseListener
    }
}
