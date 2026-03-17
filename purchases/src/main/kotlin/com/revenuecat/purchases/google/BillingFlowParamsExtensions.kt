package com.revenuecat.purchases.google

import com.android.billingclient.api.BillingFlowParams
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.ReplaceProductInfo
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.StoreReplacementMode
import com.revenuecat.purchases.models.toPlayBillingClientMode

@OptIn(InternalRevenueCatAPI::class)
internal fun BillingFlowParams.Builder.setUpgradeInfo(replaceProductInfo: ReplaceProductInfo) {
    val subscriptionUpdateParams = BillingFlowParams.SubscriptionUpdateParams.newBuilder().apply {
        setOldPurchaseToken(replaceProductInfo.oldPurchase.purchaseToken)
        replaceProductInfo.replacementMode?.let {
            when (it) {
                is StoreReplacementMode -> {
                    setSubscriptionReplacementMode(it.toPlayBillingClientMode())
                }
                // TO DO: Remove this when we remove GoogleReplacementMode
                is GoogleReplacementMode -> {
                    setSubscriptionReplacementMode(it.playBillingClientMode)
                }
                else -> {
                    errorLog { "Got unidentified replacement mode" }
                }
            }
        }
    }
    setSubscriptionUpdateParams(subscriptionUpdateParams.build())
}
