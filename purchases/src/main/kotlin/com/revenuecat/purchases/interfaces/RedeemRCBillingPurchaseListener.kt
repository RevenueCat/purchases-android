package com.revenuecat.purchases.interfaces

enum class RedeemRCBillingPurchaseResult {
    SUCCESS,
    ERROR,
}

interface RedeemRCBillingResultListener {
    fun handleResult(result: RedeemRCBillingPurchaseResult)
}

interface RedeemRCBillingPurchaseListener {
    fun handleRCBillingPurchaseRedemption(startRedemption: (RedeemRCBillingResultListener) -> Unit)
}
