package com.revenuecat.apitester.java;

import com.revenuecat.purchases.models.GoogleSubscriptionOption;
import com.revenuecat.purchases.models.PricingPhase;
import com.revenuecat.purchases.models.SubscriptionOption;

import java.util.List;

@SuppressWarnings({"unused"})
final class SubscriptionOptionAPI {

    static void checkSubscriptionOption(SubscriptionOption subscriptionOption) {
        List<PricingPhase> pricingPhases = subscriptionOption.getPricingPhases();
        List<String> tags = subscriptionOption.getTags();
        Boolean isBasePlan = subscriptionOption.isBasePlan();
    }

    static void checkGooglSubscriptionOption(GoogleSubscriptionOption googleSubscriptionOption) {
        checkSubscriptionOption(googleSubscriptionOption);
        String token = googleSubscriptionOption.getToken();
    }

}
