package com.revenuecat.apitester.java;

import com.revenuecat.purchases.Offering;
import com.revenuecat.purchases.Offerings;

import java.util.Map;

@SuppressWarnings({"unused"})
final class OfferingsAPI {
    static void check(final Offerings offerings) {
        final Offering current = offerings.getCurrent();
        final Map<String, Offering> all = offerings.getAll();
        final Offering o1 = offerings.getOffering("");
        final Offering o2 = offerings.get("");
        final Offering o3 = offerings.getCurrentOfferingForPlacement("")
    }
}
