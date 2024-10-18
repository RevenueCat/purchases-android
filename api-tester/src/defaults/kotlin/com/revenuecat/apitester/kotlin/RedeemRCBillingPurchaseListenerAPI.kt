package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.interfaces.RedeemRCBillingPurchaseListener

@Suppress("unused", "UNUSED_VARIABLE")
private class RedeemRCBillingPurchaseListenerAPI {
    fun checkListener(
        redeemRCBillingPurchaseListener: RedeemRCBillingPurchaseListener,
        callback: (RedeemRCBillingPurchaseListener.ResultListener) -> Unit,
    ) {
        redeemRCBillingPurchaseListener.handleRCBillingPurchaseRedemption(callback)
    }

    fun checkResultListener(
        resultListener: RedeemRCBillingPurchaseListener.ResultListener,
        result: RedeemRCBillingPurchaseListener.RedeemResult,
    ) {
        resultListener.handleResult(result)
    }

    fun checkResult(result: RedeemRCBillingPurchaseListener.RedeemResult): Boolean {
        when (result) {
            RedeemRCBillingPurchaseListener.RedeemResult.SUCCESS,
            RedeemRCBillingPurchaseListener.RedeemResult.ERROR -> {
                return true
            }
        }
    }

    fun checkPurchases(
        purchases: Purchases,
        redeemRCBillingPurchaseListener: RedeemRCBillingPurchaseListener,
    ) {
        purchases.redeemRCBillingPurchaseListener = redeemRCBillingPurchaseListener
        val getListener: RedeemRCBillingPurchaseListener? = purchases.redeemRCBillingPurchaseListener
    }
}
