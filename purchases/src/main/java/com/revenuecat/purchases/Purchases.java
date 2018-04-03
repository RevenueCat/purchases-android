package com.revenuecat.purchases;

import android.app.Activity;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.SkuDetails;

import java.util.List;

public final class Purchases {

    private final String apiKey;
    private final String appUserID;
    private final PurchasesListener listener;
    private final Backend backend;
    private final BillingWrapper billingWrapper;

    public interface PurchasesListener {
        void onCompletedPurchase(PurchaserInfo purchaserInfo);
        void onFailedPurchase(Exception reason);
        void onReceiveUpdatedPurchaserInfo(PurchaserInfo purchaserInfo);
    }

    public interface GetSkusResponseHandler {
        void onReceiveSkus(List<SkuDetails> skus);
    }

    public Purchases(String apiKey, PurchasesListener listener) {
        this(apiKey, "", listener);
    }

    public Purchases(String apiKey, String appUserID, PurchasesListener listener) {
        this(apiKey, appUserID, listener, null, null);
    }

    Purchases(String apiKey, String appUserID, PurchasesListener listener,
              Backend backend, BillingWrapper billingWrapper) {
        this.apiKey = apiKey;
        this.appUserID = appUserID;
        this.listener = listener;
        this.backend = backend;
        this.billingWrapper = billingWrapper;
    }

    public void getSubscriptionSkus(List<String> skus, final GetSkusResponseHandler handler) {
        billingWrapper.querySkuDetailsAsync(BillingClient.SkuType.SUBS, skus, new BillingWrapper.SkuDetailsResponseListener() {
            @Override
            public void onReceiveSkuDetails(List<SkuDetails> skuDetails) {
                handler.onReceiveSkus(skuDetails);
            }
        });
    }

    public void getNonSubscriptionSkus(List<String> skus, final GetSkusResponseHandler handler) {

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
