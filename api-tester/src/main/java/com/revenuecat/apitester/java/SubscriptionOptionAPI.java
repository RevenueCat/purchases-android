package com.revenuecat.apitester.java;

import com.android.billingclient.api.ProductDetails;
import com.revenuecat.purchases.models.GoogleSubscriptionOption;
import com.revenuecat.purchases.models.PricingPhase;
import com.revenuecat.purchases.models.PurchasingData;
import com.revenuecat.purchases.models.SubscriptionOption;

import java.util.List;

@SuppressWarnings({"unused"})
final class SubscriptionOptionAPI {

    static void checkSubscriptionOption(SubscriptionOption subscriptionOption) {
        List<PricingPhase> pricingPhases = subscriptionOption.getPricingPhases();
        List<String> tags = subscriptionOption.getTags();
        Boolean isBasePlan = subscriptionOption.isBasePlan();
        String presentedOfferingId = subscriptionOption.getPresentedOfferingIdentifier();
        PurchasingData purchasingData = subscriptionOption.getPurchasingData();
        String id = subscriptionOption.getId();
    }

    static void checkGoogleSubscriptionOption(GoogleSubscriptionOption googleSubscriptionOption) {
        checkSubscriptionOption(googleSubscriptionOption);
        String productId = googleSubscriptionOption.getProductId();
        String basePlanId = googleSubscriptionOption.getBasePlanId();
        String offerId = googleSubscriptionOption.getOfferId();
        String offerToken = googleSubscriptionOption.getOfferToken();
        ProductDetails productDetails = googleSubscriptionOption.getProductDetails();

        GoogleSubscriptionOption constructedGoogleSubOption = new GoogleSubscriptionOption(
                productId,
                basePlanId,
                offerId,
                googleSubscriptionOption.getPricingPhases(),
                googleSubscriptionOption.getTags(),
                productDetails,
                offerToken,
                googleSubscriptionOption.getPresentedOfferingIdentifier()
        );
    }

}
