package com.revenuecat.apitester.java;

import com.revenuecat.purchases.models.PricingPhase;
import com.revenuecat.purchases.models.SubscriptionOption;

import java.util.List;

@SuppressWarnings({"unused"})
final class PurchaseOptionAPI {

    static void checkPurchaseOption(SubscriptionOption subscriptionOption) {
        List<PricingPhase> pricingPhases = subscriptionOption.getPricingPhases();
        List<String> tags = subscriptionOption.getTags();
        Boolean isBasePlan = subscriptionOption.isBasePlan();
    }

}
