package com.revenuecat.apitester.kotlin

import com.android.billingclient.api.BillingFlowParams.SubscriptionUpdateParams.ReplacementMode
import com.revenuecat.purchases.UpgradeInfo

@Suppress("unused", "UNUSED_VARIABLE", "RemoveExplicitTypeArguments")
private class UpgradeInfoAPI {
    fun check(upgradeInfo: UpgradeInfo) {
        with(upgradeInfo) {
            val oldProductId: String = oldSku

            @ReplacementMode val replacementMode: Int? = replacementMode

            val constructedUpgradeInfo =
                UpgradeInfo(
                    oldSku,
                    replacementMode,
                )

            val constructedUpgradeInfoSkuOnly = UpgradeInfo(oldSku)
        }
    }
}
