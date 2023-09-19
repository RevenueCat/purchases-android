package com.revenuecat.apitester.java;

import com.revenuecat.purchases.models.OfferPaymentMode;
import com.revenuecat.purchases.models.Period;
import com.revenuecat.purchases.models.Price;
import com.revenuecat.purchases.models.PricingPhase;
import com.revenuecat.purchases.models.RecurrenceMode;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
final class PricingPhaseAPI {
    static void checkPricingPhase(PricingPhase pricingPhase) {
        Period billingPeriod = pricingPhase.getBillingPeriod();
        RecurrenceMode recurrenceMode = pricingPhase.getRecurrenceMode();
        Integer billingCycleCount = pricingPhase.getBillingCycleCount();
        Price price = pricingPhase.getPrice();

        OfferPaymentMode offerPaymentMode = pricingPhase.getOfferPaymentMode();
    }

    static void checkPrice(Price price) {
        String formatted = price.getFormatted();
        Long amount = price.getAmountMicros();
        String currencyCode = price.getCurrencyCode();
    }
}
