package com.revenuecat.apitester.java;

import com.revenuecat.purchases.EntitlementInfo;
import com.revenuecat.purchases.OwnershipType;
import com.revenuecat.purchases.PeriodType;
import com.revenuecat.purchases.Store;
import com.revenuecat.purchases.VerificationResult;

import org.json.JSONObject;

import java.util.Date;

@SuppressWarnings({"unused", "deprecation"})
final class EntitlementInfoAPI {
    static void check(final EntitlementInfo entitlementInfo) {
        final String identifier = entitlementInfo.getIdentifier();
        final boolean active = entitlementInfo.isActive();
        final boolean willRenew = entitlementInfo.getWillRenew();
        final PeriodType periodType = entitlementInfo.getPeriodType();
        final Date latestPurchaseDate = entitlementInfo.getLatestPurchaseDate();
        final Date originalPurchaseDate = entitlementInfo.getOriginalPurchaseDate();
        final Date expirationDate = entitlementInfo.getExpirationDate();
        final Store store = entitlementInfo.getStore();
        final String productIdentifier = entitlementInfo.getProductIdentifier();
        final String productPlanIdentifier = entitlementInfo.getProductPlanIdentifier();
        final boolean sandbox = entitlementInfo.isSandbox();
        final Date unsubscribeDetectedAt = entitlementInfo.getUnsubscribeDetectedAt();
        final Date billingIssueDetectedAt = entitlementInfo.getBillingIssueDetectedAt();
        final OwnershipType ownershipType = entitlementInfo.getOwnershipType();
        final VerificationResult verification = entitlementInfo.getVerification();
    }

    static void checkConstructor(
            String identifier,
            boolean active,
            boolean willRenew,
            PeriodType periodType,
            Date latestPurchaseDate,
            Date originalPurchaseDate,
            Date expirationDate,
            Store store,
            String productIdentifier,
            String productPlanIdentifier,
            boolean sandbox,
            Date unsubscribeDetectedAt,
            Date billingIssueDetectedAt,
            OwnershipType ownershipType,
            JSONObject jsonObject,
            VerificationResult verification
    ) {
        final EntitlementInfo entitlementInfo = new EntitlementInfo(identifier, active, willRenew, periodType,
                latestPurchaseDate, originalPurchaseDate, expirationDate, store, productIdentifier,
                productPlanIdentifier, sandbox, unsubscribeDetectedAt, billingIssueDetectedAt, ownershipType,
                jsonObject,
                verification
        );
        final EntitlementInfo entitlementInfo2 = new EntitlementInfo(identifier, active, willRenew, periodType,
                latestPurchaseDate, originalPurchaseDate, expirationDate, store, productIdentifier,
                productPlanIdentifier, sandbox, unsubscribeDetectedAt, billingIssueDetectedAt, ownershipType,
                jsonObject);
    }

    static void store(final Store store) {
        switch (store) {
            case APP_STORE:
            case MAC_APP_STORE:
            case PLAY_STORE:
            case STRIPE:
            case PROMOTIONAL:
            case UNKNOWN_STORE:
            case AMAZON:
        }
    }

    static void periodType(final PeriodType type) {
        switch (type) {
            case NORMAL:
            case INTRO:
            case TRIAL:
        }
    }

    static void ownershipType(final OwnershipType type) {
        switch (type) {
            case PURCHASED:
            case FAMILY_SHARED:
            case UNKNOWN:
        }
    }
}
