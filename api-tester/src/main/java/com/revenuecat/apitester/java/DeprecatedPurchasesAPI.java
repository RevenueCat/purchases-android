package com.revenuecat.apitester.java;

import android.app.Activity;

import com.android.billingclient.api.SkuDetails;
import com.revenuecat.purchases.Package;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.ProductChangeInfo;
import com.revenuecat.purchases.models.StoreProduct;

@SuppressWarnings({"unused"})
final class DeprecatedPurchasesAPI {
    static void check(final Purchases purchases,
                      final Activity activity,
                      final SkuDetails skuDetails,
                      final StoreProduct storeProduct,
                      final Package packageToPurchase,
                      final ProductChangeInfo productChangeInfo) {
        purchases.setAllowSharingPlayStoreAccount(true);
    }

}
