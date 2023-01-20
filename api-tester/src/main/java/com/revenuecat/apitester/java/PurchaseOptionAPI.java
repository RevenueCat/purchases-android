package com.revenuecat.apitester.java;

import com.revenuecat.purchases.google.GooglePurchaseOption;
import com.revenuecat.purchases.models.PricingPhase;
import com.revenuecat.purchases.models.PurchaseOption;

import java.util.List;

@SuppressWarnings({"unused"})
final class PurchaseOptionAPI {

    static void checkPurchaseOption(PurchaseOption purchaseOption) {
        List<PricingPhase> pricingPhases = purchaseOption.getPricingPhases();
        List<String> tags = purchaseOption.getTags();
        Boolean isBasePlan = purchaseOption.isBasePlan();
    }

}
