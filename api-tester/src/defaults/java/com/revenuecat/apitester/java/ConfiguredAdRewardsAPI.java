package com.revenuecat.apitester.java;

import androidx.annotation.OptIn;
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.ads.AdRewardConfiguration;
import com.revenuecat.purchases.ads.ConfiguredAdRewards;
import com.revenuecat.purchases.interfaces.GetAdRewardsCallback;
import java.util.List;

@OptIn(markerClass = ExperimentalPreviewRevenueCatPurchasesAPI.class)
final class ConfiguredAdRewardsAPI {
    static void check(Purchases purchases, AdRewardConfiguration.Entitlement entitlement,
                      AdRewardConfiguration.VirtualCurrency currency) {
        purchases.getAdRewards("unit", new GetAdRewardsCallback() {
            @Override public void onReceived(ConfiguredAdRewards rewards) {
                if (rewards == null) return;
                AdRewardConfiguration primary = rewards.getReward();
                List<AdRewardConfiguration> additional = rewards.getMoreRewards();
                List<AdRewardConfiguration> all = rewards.getAllRewards();
                boolean noReward = rewards.getReward() == null;
            }
        });
        String identifier = entitlement.getIdentifier();
        long seconds = entitlement.getDurationInSeconds();
        String code = currency.getCode();
        int amount = currency.getAmount();
        AdRewardConfiguration unsupported = AdRewardConfiguration.UnsupportedReward.INSTANCE;
    }
}
