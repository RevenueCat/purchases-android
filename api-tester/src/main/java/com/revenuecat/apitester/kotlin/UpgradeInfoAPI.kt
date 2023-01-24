package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.UpgradeInfo
import com.revenuecat.purchases.models.GoogleProrationMode

@Suppress("unused", "UNUSED_VARIABLE", "RemoveExplicitTypeArguments")
private class UpgradeInfoAPI {
    fun check(upgradeInfo: UpgradeInfo) {
        with(upgradeInfo) {
            val oldProductId: String = oldProductId
            val prorationMode: GoogleProrationMode = googleProrationMode

            val constructedUpgradeInfo =
                UpgradeInfo(
                    oldProductId,
                    googleProrationMode
                )

            val constructedUpgradeInfoProductIdOnly = UpgradeInfo(oldProductId)
        }
    }
}
