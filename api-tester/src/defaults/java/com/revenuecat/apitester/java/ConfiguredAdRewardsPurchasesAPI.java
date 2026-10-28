package com.revenuecat.apitester.java;

import androidx.annotation.OptIn;
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.interfaces.GetAdRewardsCallback;

@OptIn(markerClass = ExperimentalPreviewRevenueCatPurchasesAPI.class)
final class ConfiguredAdRewardsPurchasesAPI {
    static void check(Purchases purchases, GetAdRewardsCallback callback) {
        purchases.getAdRewards("unit", callback);
        purchases.getAdRewards("unit", rewards -> {});
    }
}
