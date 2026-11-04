package com.revenuecat.apitester.java;

import com.revenuecat.purchases.ads.AdRewardConfiguration;
import com.revenuecat.purchases.ads.ConfiguredAdRewards;
import com.revenuecat.purchases.interfaces.GetAdRewardsCallback;
import java.util.List;

final class ConfiguredAdRewardsAPI {
    static void check(ConfiguredAdRewards rewards, AdRewardConfiguration.Entitlement entitlement,
                      AdRewardConfiguration.VirtualCurrency currency) {
        AdRewardConfiguration primary = rewards.getReward();
        List<AdRewardConfiguration> additional = rewards.getMoreRewards();
        boolean noReward = rewards.getReward() == null;
        String identifier = entitlement.getIdentifier();
        long seconds = entitlement.getDurationInSeconds();
        String code = currency.getCode();
        int amount = currency.getAmount();
        AdRewardConfiguration unsupported = AdRewardConfiguration.UnsupportedReward.INSTANCE;
    }

    static void checkCallback(ConfiguredAdRewards rewards) {
        GetAdRewardsCallback callback = new GetAdRewardsCallback() {
            @Override public void onReceived(ConfiguredAdRewards rewards) {}
        };
        callback.onReceived(rewards);
        callback.onReceived(null);
        GetAdRewardsCallback samCallback = result -> {};
    }
}
