package com.revenuecat.purchases.google

import com.android.billingclient.api.BillingFlowParams
import com.revenuecat.purchases.common.ReplaceProductInfo
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.models.GoogleReplacementMode

fun BillingFlowParams.Builder.setUpgradeInfo(replaceProductInfo: ReplaceProductInfo) {
    val subscriptionUpdateParams = BillingFlowParams.SubscriptionUpdateParams.newBuilder().apply {
        setOldPurchaseToken(replaceProductInfo.oldPurchase.purchaseToken)
        replaceProductInfo.replacementMode?.let {
            val googleReplacementMode = it as? GoogleReplacementMode
            if (googleReplacementMode == null) {
                errorLog("Got non-Google replacement mode")
            } else {
                setSubscriptionReplacementMode(googleReplacementMode.playBillingClientMode)
            }
        }
    }
    setSubscriptionUpdateParams(subscriptionUpdateParams.build())
}
