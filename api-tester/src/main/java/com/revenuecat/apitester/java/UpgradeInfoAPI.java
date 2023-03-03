package com.revenuecat.apitester.java;

import com.revenuecat.purchases.UpgradeInfo;

@SuppressWarnings({"unused"})
final class UpgradeInfoAPI {
    static void check(final UpgradeInfo upgradeInfo) {
        final String oldProductId = upgradeInfo.getOldSku();
        final Integer prorationMode = upgradeInfo.getProrationMode();

        UpgradeInfo constructedUpgradeInfo = new UpgradeInfo(
                upgradeInfo.getOldSku(),
                upgradeInfo.getProrationMode()
        );

        UpgradeInfo constructedUpgradeInfoNullProrationMode =
                new UpgradeInfo(upgradeInfo.getOldSku(), null);
    }
}