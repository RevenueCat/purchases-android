package com.revenuecat.apitester.java;

import com.revenuecat.purchases.models.OfferPaymentMode;
import com.revenuecat.purchases.models.Period;
import com.revenuecat.purchases.models.Price;
import com.revenuecat.purchases.models.PricingPhase;
import com.revenuecat.purchases.models.RecurrenceMode;

import java.util.Locale;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
final class PricingPhaseAPI {
    static void checkPricingPhase(PricingPhase pricingPhase) {
        final Locale locale = Locale.getDefault();
        Period billingPeriod = pricingPhase.getBillingPeriod();
        RecurrenceMode recurrenceMode = pricingPhase.getRecurrenceMode();
        Integer billingCycleCount = pricingPhase.getBillingCycleCount();
        Price price = pricingPhase.getPrice();
        String pricePerMonthString = pricingPhase.formattedPriceInMonths(locale);
        String pricePerMonthStringNoLocale = pricingPhase.formattedPriceInMonths();
        Price pricePerWeek = pricingPhase.pricePerWeek(locale);
        Price pricePerMonth = pricingPhase.pricePerMonth(locale);
        Price pricePerYear = pricingPhase.pricePerYear(locale);
        Price pricePerWeekNoLocale = pricingPhase.pricePerWeek();
        Price pricePerMonthNoLocale = pricingPhase.pricePerMonth();
        Price pricePerYearNoLocale = pricingPhase.pricePerYear();

        OfferPaymentMode offerPaymentMode = pricingPhase.getOfferPaymentMode();
    }

    static void checkPrice(Price price) {
        String formatted = price.getFormatted();
        Long amount = price.getAmountMicros();
        String currencyCode = price.getCurrencyCode();
    }
}
