package com.revenuecat.apitester.java;

import com.revenuecat.purchases.UpgradeInfo;
import com.revenuecat.purchases.models.GoogleProrationMode;

@SuppressWarnings({"unused"})
final class UpgradeInfoAPI {
    static void check(final UpgradeInfo upgradeInfo) {
        final String oldProductId = upgradeInfo.getOldProductId();
        GoogleProrationMode prorationMode = upgradeInfo.getGoogleProrationMode();

        UpgradeInfo constructedUpgradeInfo = new UpgradeInfo(
                upgradeInfo.getOldProductId(),
                upgradeInfo.getGoogleProrationMode()
        );

        UpgradeInfo constructedUpgradeInfoNullProrationMode = new UpgradeInfo(upgradeInfo.getOldProductId());

        UpgradeInfo constructedUpgradeInfoProductIdOnly = new UpgradeInfo(upgradeInfo.getOldProductId());
        UpgradeInfo constructedUpgradeInfoProductIdAndOldSkuOnly = new UpgradeInfo(upgradeInfo.getOldProductId());
    }
}