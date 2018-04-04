package com.revenuecat.purchases;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class Purchases implements PurchasesUpdatedListener, Application.ActivityLifecycleCallbacks {

    private final Application application;
    private final String apiKey;
    private final String appUserID;
    private final PurchasesListener listener;
    private final Backend backend;
    private final BillingWrapper billingWrapper;

    private Date subscriberInfoLastChecked;

    public interface PurchasesListener {
        void onCompletedPurchase(PurchaserInfo purchaserInfo);
        void onFailedPurchase(Exception reason);
        void onReceiveUpdatedPurchaserInfo(PurchaserInfo purchaserInfo);
    }

    public interface GetSkusResponseHandler {
        void onReceiveSkus(List<SkuDetails> skus);
    }

    Purchases(Application application,
              String apiKey, String appUserID, PurchasesListener listener,
              Backend backend, BillingWrapper billingWrapper) {
        this.application = application;
        this.apiKey = apiKey;
        this.appUserID = appUserID;
        this.listener = listener;
        this.backend = backend;
        this.billingWrapper = billingWrapper;

        this.application.registerActivityLifecycleCallbacks(this);

        getSubscriberInfo();
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

    private void getSubscriberInfo() {
        if (subscriberInfoLastChecked != null && (new Date().getTime() - subscriberInfoLastChecked.getTime()) < 60000) {
            return;
        }

        backend.getSubscriberInfo(appUserID, new Backend.BackendResponseHandler() {
            @Override
            public void onReceivePurchaserInfo(PurchaserInfo info) {
                subscriberInfoLastChecked = new Date();
                listener.onReceiveUpdatedPurchaserInfo(info);
            }

            @Override
            public void onError(Exception e) {}
        });
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

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        getSubscriberInfo();
    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}
