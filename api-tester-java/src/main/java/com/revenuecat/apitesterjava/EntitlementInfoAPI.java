package com.revenuecat.apitesterjava;

import com.revenuecat.purchases.EntitlementInfo;
import com.revenuecat.purchases.OwnershipType;
import com.revenuecat.purchases.PeriodType;
import com.revenuecat.purchases.Store;

import java.util.Date;

@SuppressWarnings({"ConstantConditions", "unused", "SpellCheckingInspection"})
final class EntitlementInfoAPI {
    static void check() {
        final EntitlementInfo entitlementInfo = null;

        final String identifier = entitlementInfo.getIdentifier();
        final boolean active = entitlementInfo.isActive();
        final boolean willRenew = entitlementInfo.getWillRenew();
        final PeriodType periodType = entitlementInfo.getPeriodType();
        final Date latestPurchaseDate = entitlementInfo.getLatestPurchaseDate();
        final Date originalPurchaseDate = entitlementInfo.getOriginalPurchaseDate();
        final Date expirationDate = entitlementInfo.getExpirationDate();
        final Store store = entitlementInfo.getStore();
        final String productIdentifier = entitlementInfo.getProductIdentifier();
        final boolean sandbox = entitlementInfo.isSandbox();
        final Date unsubscribeDetectedAt = entitlementInfo.getUnsubscribeDetectedAt();
        final Date billingIssueDetectedAt = entitlementInfo.getBillingIssueDetectedAt();
        final OwnershipType ownershipType = entitlementInfo.getOwnershipType();
    }
}
