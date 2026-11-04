package com.revenuecat.apitester.java;

import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.interfaces.GetAdRewardsCallback;

final class ConfiguredAdRewardsPurchasesAPI {
    static void check(Purchases purchases, GetAdRewardsCallback callback) {
        purchases.getAdRewards("unit", callback);
        purchases.getAdRewards("unit", rewards -> {});
    }
}
