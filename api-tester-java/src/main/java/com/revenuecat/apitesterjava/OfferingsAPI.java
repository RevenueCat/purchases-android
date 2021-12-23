package com.revenuecat.apitesterjava;

import com.revenuecat.purchases.Offering;
import com.revenuecat.purchases.Offerings;

import java.util.Map;

@SuppressWarnings({"ConstantConditions", "unused"})
final class OfferingsAPI {
    static void check() {
        final Offerings offerings = null;

        final Offering current = offerings.getCurrent();
        final Map<String, Offering> all = offerings.getAll();
        final Offering o1 = offerings.getOffering("");
        final Offering o2 = offerings.get("");
    }
}
