package com.revenuecat.apitesterjava;

import com.revenuecat.purchases.Offering;
import com.revenuecat.purchases.Package;

import java.util.List;

@SuppressWarnings({"ConstantConditions", "unused"})
final class OfferingAPI {
    static void check() {
        final Offering offering = null;

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
    }
}
