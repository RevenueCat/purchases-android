package com.revenuecat.apitester.java;

import com.revenuecat.purchases.models.PricingPhase;
import com.revenuecat.purchases.models.SubscriptionOption;
import com.revenuecat.purchases.models.SubscriptionOptions;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unused"})
final class SubscriptionOptionsAPI {

    static void checkSubscriptionOptions() {
        SubscriptionOptions subscriptionOptions = new SubscriptionOptions(new ArrayList());

        SubscriptionOption freeTrial = subscriptionOptions.getFreeTrial();
        SubscriptionOption introTrial = subscriptionOptions.getIntroTrial();
        List<SubscriptionOption> tagOptions = subscriptionOptions.withTag("pick-this-one");

        for (SubscriptionOption subscriptionOption : subscriptionOptions) {
            String optionId = subscriptionOption.getId();
        }
    }

}
