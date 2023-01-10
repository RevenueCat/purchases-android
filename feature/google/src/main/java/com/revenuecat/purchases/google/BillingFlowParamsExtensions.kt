package com.revenuecat.purchases.google

import com.android.billingclient.api.BillingFlowParams
import com.revenuecat.purchases.common.ReplaceProductInfo

fun BillingFlowParams.Builder.setUpgradeInfo(replaceProductInfo: ReplaceProductInfo) {
    val subscriptionUpdateParams = BillingFlowParams.SubscriptionUpdateParams.newBuilder().apply {
        setOldPurchaseToken(replaceProductInfo.oldPurchase.purchaseToken)
        replaceProductInfo.prorationMode?.let { prorationMode ->
            // TODO BC5 handle new proration mode logic
            setReplaceProrationMode(prorationMode)
        }
    }
    setSubscriptionUpdateParams(subscriptionUpdateParams.build())
}
