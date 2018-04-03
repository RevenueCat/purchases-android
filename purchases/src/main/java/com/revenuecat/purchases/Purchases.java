package com.revenuecat.purchases;

import android.app.Activity;
import android.support.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;

import java.util.ArrayList;
import java.util.List;

public final class Purchases implements PurchasesUpdatedListener {

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
        getSkus(skus, BillingClient.SkuType.SUBS, handler);
    }

    public void getNonSubscriptionSkus(List<String> skus, final GetSkusResponseHandler handler) {
        getSkus(skus, BillingClient.SkuType.INAPP, handler);
    }

    private void getSkus(List<String> skus, @BillingClient.SkuType String skuType, final GetSkusResponseHandler handler) {
        billingWrapper.querySkuDetailsAsync(skuType, skus, new BillingWrapper.SkuDetailsResponseListener() {
            @Override
            public void onReceiveSkuDetails(List<SkuDetails> skuDetails) {
                handler.onReceiveSkus(skuDetails);
            }
        });
    }

    public void makePurchase(final Activity activity, final String sku,
                             @BillingClient.SkuType final String skuType) {
        makePurchase(activity, sku, skuType, new ArrayList<String>());
    }

    public void makePurchase(final Activity activity, final String sku,
                             @BillingClient.SkuType final String skuType,
                             final ArrayList<String> oldSkus) {
        billingWrapper.makePurchaseAsync(activity, appUserID, sku, oldSkus, skuType);
    }

    @Override
    public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {
        if (responseCode == BillingClient.BillingResponse.OK && purchases != null) {
            for (Purchase p : purchases) {
                backend.postReceiptData(p.getPurchaseToken(), appUserID, p.getSku(), new Backend.BackendResponseHandler() {
                    @Override
                    public void onReceivePurchaserInfo(PurchaserInfo info) {
                        listener.onCompletedPurchase(info);
                    }

                    @Override
                    public void onError(Exception e) {
                        listener.onFailedPurchase(e);
                    }
                });
            }
        } else {
            listener.onFailedPurchase(new Exception("Failed to update purchase with reason " + responseCode));
        }
    }
}
