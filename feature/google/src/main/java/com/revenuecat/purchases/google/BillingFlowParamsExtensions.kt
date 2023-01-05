package com.revenuecat.purchases.google

import com.android.billingclient.api.BillingFlowParams
import com.revenuecat.purchases.common.ReplaceSkuInfo

fun BillingFlowParams.Builder.setUpgradeInfo(replaceProductInfo: ReplaceSkuInfo) {
    val subscriptionUpdateParams = BillingFlowParams.SubscriptionUpdateParams.newBuilder().apply {
        setOldPurchaseToken(replaceProductInfo.oldPurchase.purchaseToken)
        replaceProductInfo.prorationMode?.let { prorationMode ->
            // TODO BC5 handle new proration mode logic
            setReplaceProrationMode(prorationMode)
        }
    }
    setSubscriptionUpdateParams(subscriptionUpdateParams.build())
}
