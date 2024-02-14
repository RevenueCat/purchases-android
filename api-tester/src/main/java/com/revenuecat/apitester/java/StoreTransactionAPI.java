package com.revenuecat.apitester.java;

import com.revenuecat.purchases.PresentedOfferingContext;
import com.revenuecat.purchases.ProductType;
import com.revenuecat.purchases.ProrationMode;
import com.revenuecat.purchases.ReplacementMode;
import com.revenuecat.purchases.models.PurchaseState;
import com.revenuecat.purchases.models.PurchaseType;
import com.revenuecat.purchases.models.StoreTransaction;

import org.json.JSONObject;

import java.util.List;

@SuppressWarnings({"unused"})
final class StoreTransactionAPI {
    static void check(final StoreTransaction transaction) {
        final String orderId = transaction.getOrderId();
        final List<String> skus = transaction.getSkus();
        final List<String> productIds = transaction.getProductIds();
        final ProductType type = transaction.getType();
        final long purchaseTime = transaction.getPurchaseTime();
        final String purchaseToken = transaction.getPurchaseToken();
        final PurchaseState purchaseState = transaction.getPurchaseState();
        final Boolean autoRenewing = transaction.isAutoRenewing();
        final String signature = transaction.getSignature();
        final JSONObject originalJson = transaction.getOriginalJson();
        final String presentedOfferingIdentifier = transaction.getPresentedOfferingIdentifier();
        final PresentedOfferingContext presentedOfferingContext = transaction.getPresentedOfferingContext();
        final String su1 = transaction.getStoreUserID();
        final PurchaseType purchaseType = transaction.getPurchaseType();
        final String marketplace = transaction.getMarketplace();
        final String subscriptionOptionId = transaction.getSubscriptionOptionId();
        final ReplacementMode replacementMode = transaction.getReplacementMode();
        final ProrationMode prorationMode = transaction.getProrationMode();

        StoreTransaction constructedStoreTransaction = new StoreTransaction(
                transaction.getOrderId(),
                transaction.getProductIds(),
                transaction.getType(),
                transaction.getPurchaseTime(),
                transaction.getPurchaseToken(),
                transaction.getPurchaseState(),
                transaction.isAutoRenewing(),
                transaction.getSignature(),
                transaction.getOriginalJson(),
                transaction.getPresentedOfferingContext(),
                transaction.getStoreUserID(),
                transaction.getPurchaseType(),
                transaction.getMarketplace(),
                transaction.getSubscriptionOptionId(),
                transaction.getReplacementMode()
        );

        StoreTransaction constructedStoreTransactionWithOfferingId = new StoreTransaction(
                transaction.getOrderId(),
                transaction.getProductIds(),
                transaction.getType(),
                transaction.getPurchaseTime(),
                transaction.getPurchaseToken(),
                transaction.getPurchaseState(),
                transaction.isAutoRenewing(),
                transaction.getSignature(),
                transaction.getOriginalJson(),
                transaction.getPresentedOfferingIdentifier(),
                transaction.getStoreUserID(),
                transaction.getPurchaseType(),
                transaction.getMarketplace(),
                transaction.getSubscriptionOptionId(),
                transaction.getReplacementMode()
        );
    }

    static void check(final PurchaseType type) {
        switch (type) {
            case GOOGLE_PURCHASE:
            case GOOGLE_RESTORED_PURCHASE:
            case AMAZON_PURCHASE:
        }
    }
}
