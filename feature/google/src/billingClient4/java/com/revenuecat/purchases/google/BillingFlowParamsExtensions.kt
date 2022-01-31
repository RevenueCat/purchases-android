package com.revenuecat.purchases.google

import com.android.billingclient.api.BillingFlowParams
import com.revenuecat.purchases.common.ReplaceSkuInfo

fun BillingFlowParams.Builder.setUpgradeInfo(replaceSkuInfo: ReplaceSkuInfo) {
    val subscriptionUpdateParams = BillingFlowParams.SubscriptionUpdateParams.newBuilder().apply {
        setOldSkuPurchaseToken(replaceSkuInfo.oldPurchase.purchaseToken)
        replaceSkuInfo.prorationMode?.let { prorationMode ->
            setReplaceSkusProrationMode(prorationMode)
        }
    }
    setSubscriptionUpdateParams(subscriptionUpdateParams.build())
}
