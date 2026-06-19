package com.revenuecat.purchases.google

import com.android.billingclient.api.BillingFlowParams
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.ReplaceProductInfo
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.models.toPlayBillingClientMode
import com.revenuecat.purchases.models.toStoreReplacementModeOrNull

@OptIn(InternalRevenueCatAPI::class)
internal fun BillingFlowParams.Builder.setUpgradeInfo(replaceProductInfo: ReplaceProductInfo) {
    val subscriptionUpdateParams = BillingFlowParams.SubscriptionUpdateParams.newBuilder().apply {
        setOldPurchaseToken(replaceProductInfo.oldPurchase.purchaseToken)
        val replacementMode = replaceProductInfo.replacementMode.toStoreReplacementModeOrNull()
        if (replacementMode != null) {
            setSubscriptionReplacementMode(replacementMode.toPlayBillingClientMode())
        } else if (replaceProductInfo.replacementMode != null) {
            errorLog { "Got unidentified replacement mode" }
        }
    }
    setSubscriptionUpdateParams(subscriptionUpdateParams.build())
}
