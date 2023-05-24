package com.revenuecat.apitester.java;

import com.revenuecat.purchases.Offering;
import com.revenuecat.purchases.Package;

import java.util.List;
import java.util.Map;

@SuppressWarnings({"unused"})
final class OfferingAPI {
    static void check(final Offering offering) {
        final String identifier = offering.getIdentifier();
        final String serverDescription = offering.getServerDescription();
        final List<Package> availablePackages = offering.getAvailablePackages();

        final Package lifetime = offering.getLifetime();
        final Package annual = offering.getAnnual();
        final Package sixMonth = offering.getSixMonth();
        final Package threeMonth = offering.getThreeMonth();
        final Package twoMonth = offering.getTwoMonth();
        final Package monthly = offering.getMonthly();
        final Package weekly = offering.getWeekly();
        final Package p = offering.get("");
        final Package p2 = offering.getPackage("");

        final Map<String, Object> metadata = offering.getMetadata();
    }
}
