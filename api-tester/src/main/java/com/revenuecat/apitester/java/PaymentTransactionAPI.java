package com.revenuecat.apitester.java;

import com.revenuecat.purchases.ProductType;
import com.revenuecat.purchases.models.PurchaseState;
import com.revenuecat.purchases.models.PurchaseType;
import com.revenuecat.purchases.models.StoreTransaction;

import org.json.JSONObject;

import java.util.List;

@SuppressWarnings({"unused"})
final class PaymentTransactionAPI {
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
        final String su1 = transaction.getStoreUserID();
        final PurchaseType purchaseType = transaction.getPurchaseType();
    }

    static void check(final PurchaseType type) {
        switch (type) {
            case GOOGLE_PURCHASE:
            case GOOGLE_RESTORED_PURCHASE:
            case AMAZON_PURCHASE:
        }
    }
}
