package com.revenuecat.apitester.java;

import com.revenuecat.purchases.SubscriptionInfo;
import com.revenuecat.purchases.Store;
import com.revenuecat.purchases.OwnershipType;
import com.revenuecat.purchases.PeriodType;
import com.revenuecat.purchases.models.Price;

import java.util.Date;

@SuppressWarnings({"unused"})
final class SubscriptionInfoAPI {
    static void check(final SubscriptionInfo subscriptionInfo) {
        final String productIdentifier = subscriptionInfo.getProductIdentifier();
        final Date purchaseDate = subscriptionInfo.getPurchaseDate();
        final Date originalPurchaseDate = subscriptionInfo.getOriginalPurchaseDate();
        final Date expiresDate = subscriptionInfo.getExpiresDate();
        final Store store = subscriptionInfo.getStore();
        final Date unsubscribeDetectedAt = subscriptionInfo.getUnsubscribeDetectedAt();
        final boolean isSandbox = subscriptionInfo.isSandbox();
        final Date billingIssuesDetectedAt = subscriptionInfo.getBillingIssuesDetectedAt();
        final Date gracePeriodExpiresDate = subscriptionInfo.getGracePeriodExpiresDate();
        final OwnershipType ownershipType = subscriptionInfo.getOwnershipType();
        final PeriodType periodType = subscriptionInfo.getPeriodType();
        final Date refundedAt = subscriptionInfo.getRefundedAt();
        final String storeTransactionId = subscriptionInfo.getStoreTransactionId();
        final Date autoResumeDate = subscriptionInfo.getAutoResumeDate();
        final String displayName = subscriptionInfo.getDisplayName();
        final Price price = subscriptionInfo.getPrice();
        final String productPlanIdentifier = subscriptionInfo.getProductPlanIdentifier();
        final android.net.Uri managementURL = subscriptionInfo.getManagementURL();
        final boolean isActive = subscriptionInfo.isActive();
        final boolean willRenew = subscriptionInfo.getWillRenew();

        // Test deprecated constructor
        @SuppressWarnings("deprecation") final SubscriptionInfo deprecatedSubscriptionInfo = new SubscriptionInfo(
                "product",
                new Date(),
                new Date(),
                new Date(),
                Store.PLAY_STORE,
                new Date(),
                false,
                new Date(),
                new Date(),
                OwnershipType.PURCHASED,
                PeriodType.NORMAL,
                new Date(),
                "store_id",
                new Date()
        );

        // Test new constructor
        final SubscriptionInfo newSubscriptionInfo = new SubscriptionInfo(
                "product",
                new Date(),
                new Date(),
                new Date(),
                Store.PLAY_STORE,
                new Date(),
                false,
                new Date(),
                new Date(),
                OwnershipType.PURCHASED,
                PeriodType.NORMAL,
                new Date(),
                "store_id",
                new Date(),
                "Display Name",
                new Price("", 0, "USD"),
                "plan_id",
                null,
                new Date()
        );
    }
}