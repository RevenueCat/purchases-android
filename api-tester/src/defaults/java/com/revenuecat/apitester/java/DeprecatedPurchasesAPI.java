package com.revenuecat.apitester.java;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.SkuDetails;
import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.Package;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.interfaces.ProductChangeCallback;
import com.revenuecat.purchases.interfaces.PurchaseCallback;
import com.revenuecat.purchases.models.StoreProduct;
import com.revenuecat.purchases.models.StoreTransaction;
import com.revenuecat.purchases.models.SubscriptionOption;

@SuppressWarnings({"unused"})
final class DeprecatedPurchasesAPI {
    static void check(final Purchases purchases,
                      final Activity activity,
                      final SkuDetails skuDetails,
                      final StoreProduct storeProduct,
                      final Package packageToPurchase,
                      final SubscriptionOption subscriptionOption) {
        final ProductChangeCallback purchaseChangeListener = new ProductChangeCallback() {
            @Override
            public void onCompleted(@Nullable StoreTransaction storeTransaction, @NonNull CustomerInfo customerInfo) {
            }

            @Override
            public void onError(@NonNull PurchasesError error, boolean userCancelled) {
            }
        };
        final PurchaseCallback makePurchaseListener = new PurchaseCallback() {
            @Override
            public void onCompleted(@NonNull StoreTransaction storeTransaction, @NonNull CustomerInfo customerInfo) {
            }

            @Override
            public void onError(@NonNull PurchasesError error, boolean userCancelled) {
            }
        };

        purchases.setAllowSharingPlayStoreAccount(true);
        Purchases.setDebugLogsEnabled(false);
        purchases.purchaseProduct(activity, storeProduct, makePurchaseListener);
        purchases.purchasePackage(activity, packageToPurchase, makePurchaseListener);

        boolean finishTransactions = purchases.getFinishTransactions();
        purchases.setFinishTransactions(true);

        purchases.syncObserverModeAmazonPurchase(
                storeProduct.getId(),
                "receipt-id",
                "amazon-user-id",
                "EUR",
                1.99
        );
    }

}
