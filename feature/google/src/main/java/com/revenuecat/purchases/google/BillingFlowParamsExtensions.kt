package com.revenuecat.purchases.google

import com.android.billingclient.api.BillingFlowParams
import com.revenuecat.purchases.common.ReplaceProductInfo
import com.revenuecat.purchases.models.GoogleProrationMode

fun BillingFlowParams.Builder.setUpgradeInfo(replaceProductInfo: ReplaceProductInfo) {
    val subscriptionUpdateParams = BillingFlowParams.SubscriptionUpdateParams.newBuilder().apply {
        setOldPurchaseToken(replaceProductInfo.oldPurchase.purchaseToken)
        replaceProductInfo.prorationMode?.let {
            setReplaceProrationMode((it as GoogleProrationMode).playBillingClientMode)
        }
    }
    setSubscriptionUpdateParams(subscriptionUpdateParams.build())
}
