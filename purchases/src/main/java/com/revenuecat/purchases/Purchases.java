package com.revenuecat.purchases;

import android.app.Activity;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;

import java.util.List;

public final class Purchases {

    public interface PurchasesListener {
        void onCompletedPurchase(PurchaserInfo purchaserInfo);
        void onFailedPurchase(Exception reason);
        void onReceiveUpdatedPurchaserInfo(PurchaserInfo purchaserInfo);
    }

    public interface GetSkusResponseHandler {
        void onReceiveSkus(List<SkuDetails> skus);
    }

    public Purchases(String appUserID) {

    }

    public Purchases(String APIKey, String appUserID) {

    }

    public void getSubscriptionSkus(final GetSkusResponseHandler handler) {

    }

    public void getNonSubscriptionSkus(final GetSkusResponseHandler handler) {

    }

    public void makePurchase(final Activity activity, final String sku, @BillingClient.SkuType final String skuType) {
        makePurchase(activity, sku, skuType, null);
    }

    public void makePurchase(final Activity activity, final String sku,
                             @BillingClient.SkuType final String skuType, final List<String> oldSkus) {

    }

    public void restorePurchasesForPlayAccount() {
        
    }
}
