package com.revenuecat.purchases.interfaces

interface RedeemRCBillingPurchaseListener {
    enum class RedeemResult {
        SUCCESS,
        ERROR,
    }

    interface ResultListener {
        fun handleResult(result: RedeemResult)
    }

    fun handleRCBillingPurchaseRedemption(startRedemption: (ResultListener) -> Unit)
}
