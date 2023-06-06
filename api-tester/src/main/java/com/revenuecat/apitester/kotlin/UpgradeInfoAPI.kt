package com.revenuecat.apitester.kotlin

import com.android.billingclient.api.BillingFlowParams.ProrationMode
import com.revenuecat.purchases.UpgradeInfo

@Suppress("unused", "UNUSED_VARIABLE", "RemoveExplicitTypeArguments")
private class UpgradeInfoAPI {
    fun check(upgradeInfo: UpgradeInfo) {
        with(upgradeInfo) {
            val oldProductId: String = oldSku

            @ProrationMode val prorationMode: Int? = prorationMode

            val constructedUpgradeInfo =
                UpgradeInfo(
                    oldSku,
                    prorationMode,
                )

            val constructedUpgradeInfoSkuOnly = UpgradeInfo(oldSku)
        }
    }
}
