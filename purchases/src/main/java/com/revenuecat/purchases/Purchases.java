package com.revenuecat.purchases;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public final class Purchases implements BillingWrapper.PurchasesUpdatedListener, Application.ActivityLifecycleCallbacks {

    private final String appUserID;
    private final PurchaserInfoCache purchaserInfoCache;
    private Boolean usingAnonymousID = false;
    private final PurchasesListener listener;
    private final Backend backend;
    private final BillingWrapper billingWrapper;

    private final HashSet<String> postedTokens = new HashSet<>();

    private Date subscriberInfoLastChecked;


    public interface PurchasesListener {
        void onCompletedPurchase(PurchaserInfo purchaserInfo);
        void onFailedPurchase(Exception reason);
        void onReceiveUpdatedPurchaserInfo(PurchaserInfo purchaserInfo);
    }

    public interface GetSkusResponseHandler {
        void onReceiveSkus(List<SkuDetails> skus);
    }

    public static String getFrameworkVersion() {
        return "0.1.0-SNAPSHOT";
    }

    Purchases(Application application,
              String appUserID, PurchasesListener listener,
              Backend backend,
              BillingWrapper.Factory billingWrapperFactory,
              PurchaserInfoCache purchaserInfoCache) {

        if (appUserID == null) {
            appUserID = UUID.randomUUID().toString();
            usingAnonymousID = true;
        }
        this.appUserID = appUserID;

        this.listener = listener;
        this.backend = backend;
        this.billingWrapper = billingWrapperFactory.buildWrapper(this);
        this.purchaserInfoCache = purchaserInfoCache;

        application.registerActivityLifecycleCallbacks(this);

        PurchaserInfo info = purchaserInfoCache.getCachedPurchaserInfo();
        if (info != null) {
            listener.onReceiveUpdatedPurchaserInfo(info);
        }

        getSubscriberInfo();
    }

    /**
     * returns the passed in or generated app user ID
     * @return appUserID
     */
    public String getAppUserID() {
        return appUserID;
    }

    /**
     * Gets the SKUDetails for the given list of subscription skus.
     * @param skus List of skus
     * @param handler Response handler
     */
    public void getSubscriptionSkus(List<String> skus, final GetSkusResponseHandler handler) {
        getSkus(skus, BillingClient.SkuType.SUBS, handler);
    }

    /**
     * Gets the SKUDetails for the given list of non-subscription skus.
     * @param skus
     * @param handler
     */
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

    /**
     * Make a purchase.
     * @param activity Current activity
     * @param sku The sku you wish to purchase
     * @param skuType The type of sku, INAPP or SUBS
     */
    public void makePurchase(final Activity activity, final String sku,
                             @BillingClient.SkuType final String skuType) {
        makePurchase(activity, sku, skuType, new ArrayList<String>());
    }

    /**
     * Make a purchase passing in the skus you wish to upgrade from.
     * @param activity Current activity
     * @param sku The sku you wish to purchase
     * @param skuType The type of sku, INAPP or SUBS
     * @param oldSkus List of old skus to upgrade from
     */
    public void makePurchase(final Activity activity, final String sku,
                             @BillingClient.SkuType final String skuType,
                             final ArrayList<String> oldSkus) {
        billingWrapper.makePurchaseAsync(activity, appUserID, sku, oldSkus, skuType);
    }

    /**
     * Restores purchases made with the current Play Store account for the current user.
     * If you initialized Purchases with an `appUserID` any receipt tokens currently being used by
     * other users of your app will not be restored. If you used an anonymous id, i.e. you
     * initialized Purchases without an appUserID, any other anonymous users using the same
     * purchases will be merged.
     */
    public void restorePurchasesForPlayStoreAccount() {
        billingWrapper.queryPurchaseHistoryAsync(BillingClient.SkuType.SUBS, new BillingWrapper.PurchaseHistoryResponseListener() {
            @Override
            public void onReceivePurchaseHistory(List<Purchase> purchasesList) {
                postPurchases(purchasesList, true, false);
            }
        });

        billingWrapper.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP, new BillingWrapper.PurchaseHistoryResponseListener() {
            @Override
            public void onReceivePurchaseHistory(List<Purchase> purchasesList) {
                postPurchases(purchasesList, true, false);
            }
        });
    }


    private void getSubscriberInfo() {
        if (subscriberInfoLastChecked != null && (new Date().getTime() - subscriberInfoLastChecked.getTime()) < 60000) {
            return;
        }

        backend.getSubscriberInfo(appUserID, new Backend.BackendResponseHandler() {
            @Override
            public void onReceivePurchaserInfo(PurchaserInfo info) {
                subscriberInfoLastChecked = new Date();
                purchaserInfoCache.cachePurchaserInfo(info);
                listener.onReceiveUpdatedPurchaserInfo(info);
            }

            @Override
            public void onError(Exception e) {
                Log.e("Purchases", "Error fetching subscriber data: " + e.getMessage());
            }
        });
    }

    private void postPurchases(List<Purchase> purchases, Boolean isRestore, final Boolean isPurchase) {
        for (Purchase p : purchases) {
            final String token = p.getPurchaseToken();
            if (postedTokens.contains(token)) continue;
            postedTokens.add(token);
            backend.postReceiptData(token, appUserID, p.getSku(), isRestore, new Backend.BackendResponseHandler() {
                @Override
                public void onReceivePurchaserInfo(PurchaserInfo info) {
                    if (isPurchase) {
                        listener.onCompletedPurchase(info);
                    } else {
                        listener.onReceiveUpdatedPurchaserInfo(info);
                    }
                }

                @Override
                public void onError(Exception e) {
                    postedTokens.remove(token);
                    listener.onFailedPurchase(e);
                }
            });
        }
    }

    @Override
    public void onPurchasesUpdated(List<Purchase> purchases) {
        postPurchases(purchases, usingAnonymousID, true);
    }

    @Override
    public void onPurchasesFailedToUpdate(int responseCode, String message) {
        listener.onFailedPurchase(new Exception(message));
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
        restorePurchasesForPlayStoreAccount();
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

    /**
     * Used to construct a Purchases object
     */
    public static class Builder {
        private final Context context;
        private final String apiKey;
        private final Application application;
        private final PurchasesListener listener;
        private String appUserID;
        private ExecutorService service;

        private boolean hasPermission(Context context, String permission) {
            return context.checkCallingOrSelfPermission(permission) == PERMISSION_GRANTED;
        }

        public Builder(Context context, String apiKey, PurchasesListener listener) {

            if (!hasPermission(context, Manifest.permission.INTERNET)) {
                throw new IllegalArgumentException("Purchases requires INTERNET permission.");
            }

            if (context == null) {
                throw new IllegalArgumentException("Context must be set.");
            }

            if (apiKey == null || apiKey.length() == 0) {
                throw new IllegalArgumentException("API key must be set. Get this from the RevenueCat web app");
            }

            Application application = (Application) context.getApplicationContext();
            if (application == null) {
                throw new IllegalArgumentException("Needs an application context.");
            }

            if (listener == null) {
                throw new IllegalArgumentException("Purchases listener must be set");
            }

            this.context = context;
            this.apiKey = apiKey;
            this.application = application;
            this.listener = listener;
        }

        private ExecutorService createDefaultExecutor() {
            return new ThreadPoolExecutor(
                    1,
                    2,
                    0,
                    TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>()
            );
        }

        public Purchases build() {

            ExecutorService service = this.service;
            if (service == null) {
                service = createDefaultExecutor();
            }

            Backend backend = new Backend(this.apiKey, new Dispatcher(service), new HTTPClient(), new PurchaserInfo.Factory());

            BillingWrapper.Factory billingWrapperFactory = new BillingWrapper.Factory(new BillingWrapper.ClientFactory(context), new Handler(application.getMainLooper()));

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.application);
            PurchaserInfoCache cache = new PurchaserInfoCache(prefs, appUserID, apiKey);

            return new Purchases(this.application, this.appUserID, this.listener, backend, billingWrapperFactory, cache);
        }

        public Builder appUserID(String appUserID) {
            this.appUserID = appUserID;
            return this;
        }

        public Builder networkExecutorService(ExecutorService service) {
            this.service = service;
            return this;
        }
    }
}
