package com.revenuecat.apitester.java;

import com.revenuecat.purchases.UpgradeInfo;

@SuppressWarnings({"unused"})
final class UpgradeInfoAPI {
    static void check(final UpgradeInfo upgradeInfo) {
        final String oldProductId = upgradeInfo.getOldSku();
        final Integer replacementMode = upgradeInfo.getReplacementMode();

        UpgradeInfo constructedUpgradeInfo = new UpgradeInfo(
                upgradeInfo.getOldSku(),
                upgradeInfo.getReplacementMode()
        );

        UpgradeInfo constructedUpgradeInfoNullProrationMode =
                new UpgradeInfo(upgradeInfo.getOldSku(), null);
    }
}
