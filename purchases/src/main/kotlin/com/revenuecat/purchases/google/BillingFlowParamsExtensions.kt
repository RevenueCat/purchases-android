package com.revenuecat.purchases.google

import com.android.billingclient.api.BillingFlowParams
import com.revenuecat.purchases.common.ReplaceProductInfo
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.models.GoogleProrationMode

internal fun BillingFlowParams.Builder.setUpgradeInfo(replaceProductInfo: ReplaceProductInfo) {
    val subscriptionUpdateParams = BillingFlowParams.SubscriptionUpdateParams.newBuilder().apply {
        setOldPurchaseToken(replaceProductInfo.oldPurchase.purchaseToken)
        replaceProductInfo.prorationMode?.let {
            val googleProrationMode = it as? GoogleProrationMode
            if (googleProrationMode == null) {
                errorLog("Got non-Google proration mode")
            } else {
                setReplaceProrationMode(googleProrationMode.playBillingClientMode)
            }
        }
    }
    setSubscriptionUpdateParams(subscriptionUpdateParams.build())
}
