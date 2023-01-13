package com.revenuecat.apitester.java;

import com.android.billingclient.api.BillingFlowParams;
import com.revenuecat.purchases.UpgradeInfo;

@SuppressWarnings({"unused"})
final class UpgradeInfoAPI {
    static void check(final UpgradeInfo upgradeInfo) {
        final String oldProductId = upgradeInfo.getOldProductId();
        @BillingFlowParams.ProrationMode final Integer prorationMode = upgradeInfo.getProrationMode();

        UpgradeInfo constructedUpgradeInfo = new UpgradeInfo(
                upgradeInfo.getOldProductId(),
                upgradeInfo.getProrationMode()
        );

        UpgradeInfo constructedUpgradeInfoNullProrationMode = new UpgradeInfo(
                upgradeInfo.getOldProductId(),
                null
        );

        UpgradeInfo constructedUpgradeInfoProductIdOnly = new UpgradeInfo(upgradeInfo.getOldProductId());
        UpgradeInfo constructedUpgradeInfoProductIdAndOldSkuOnly = new UpgradeInfo(
                upgradeInfo.getOldProductId()
        );
    }
}