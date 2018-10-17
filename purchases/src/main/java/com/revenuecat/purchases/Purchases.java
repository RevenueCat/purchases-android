package com.revenuecat.purchases;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.util.Log;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static java.lang.annotation.RetentionPolicy.SOURCE;

public final class Purchases implements BillingWrapper.PurchasesUpdatedListener, Application.ActivityLifecycleCallbacks {

    private final String appUserID;
    private final DeviceCache deviceCache;
    private final Application application;
    private Boolean usingAnonymousID = false;
    private final PurchasesListener listener;
    private final Backend backend;
    private final BillingWrapper billingWrapper;

    private final HashSet<String> postedTokens = new HashSet<>();

    private Date cachesLastChecked;
    private Map<String, Entitlement> cachedEntitlements;

    @IntDef({ErrorDomains.REVENUECAT_BACKEND, ErrorDomains.PLAY_BILLING})
    @Retention(SOURCE)
    public @interface ErrorDomains {
        int REVENUECAT_BACKEND = 0;
        int PLAY_BILLING = 1;
    }

    @IntDef({AttributionNetwork.ADJUST, AttributionNetwork.APPSFLYER, AttributionNetwork.BRANCH})
    @Retention(SOURCE)
    public @interface AttributionNetwork {
        int ADJUST = 1;
        int APPSFLYER = 2;
        int BRANCH = 3;
    }

    /**
     * Used to handle async updates from Purchases
     */
    public interface PurchasesListener {
        void onCompletedPurchase(String sku, PurchaserInfo purchaserInfo);
        void onFailedPurchase(@ErrorDomains int domain, int code, String reason);

        void onReceiveUpdatedPurchaserInfo(PurchaserInfo purchaserInfo);

        void onRestoreTransactions(PurchaserInfo purchaserInfo);
        void onRestoreTransactionsFailed(@ErrorDomains int domain, int code, String reason);
    }

    public interface GetSkusResponseHandler {
        void onReceiveSkus(List<SkuDetails> skus);
    }

    public interface GetEntitlementsHandler {
        void onReceiveEntitlements(Map<String, Entitlement> entitlementMap);
        void onReceiveEntitlementsError(@ErrorDomains int domain, int code, String message);
    }

    public static String getFrameworkVersion() {
        return "1.4.0-SNAPSHOT";
    }

    Purchases(Application application,
              String appUserID, PurchasesListener listener,
              Backend backend,
              BillingWrapper.Factory billingWrapperFactory,
              DeviceCache deviceCache) {

        if (appUserID == null) {
            usingAnonymousID = true;

            appUserID = deviceCache.getCachedAppUserID();

            if (appUserID == null) {
                appUserID = UUID.randomUUID().toString();
                deviceCache.cacheAppUserID(appUserID);
            }
        }
        this.appUserID = appUserID;

        this.listener = listener;
        this.backend = backend;
        this.billingWrapper = billingWrapperFactory.buildWrapper(this);
        this.deviceCache = deviceCache;

        this.application = application;
        this.application.registerActivityLifecycleCallbacks(this);

        emitCachedPurchaserInfo();
        getCaches();
        restorePurchasesForPlayStoreAccount();
    }

    /**
     * returns the passed in or generated app user ID
     * @return appUserID
     */
    public String getAppUserID() {
        return appUserID;
    }

    /**
     * If true, treats all purchases as restores, aliasing together appUserIDs that share a Play acccount.
     * @param isUsingAnonymousID
     */

    public void setIsUsingAnonymousID(boolean isUsingAnonymousID) {
        this.usingAnonymousID = isUsingAnonymousID;
    }

    /**
     * Add attribution data from a supported network
     */
    public void addAttributionData(JSONObject data, @AttributionNetwork int network) {
        backend.postAttributionData(appUserID, network, data);
    }

    public void addAttributionData(Map<String, String> data, @AttributionNetwork int network) {
        JSONObject jsonObject = new JSONObject();
        for (String key: data.keySet()) {
            try {
                jsonObject.put(key, data.get(key));
            } catch (JSONException e) {
                Log.e("Purchases", "Failed to add key " + key + " to attribution map");
            }
        }
        backend.postAttributionData(appUserID, network, jsonObject);
    }

    private void emitCachedPurchaserInfo() {
        PurchaserInfo info = deviceCache.getCachedPurchaserInfo(appUserID);
        if (info != null) {
            listener.onReceiveUpdatedPurchaserInfo(info);
        }
    }

    /**
     * Fetch the configured entitlements for this user. Entitlements allows you to configure your in-app products via RevenueCat
     * and greatly simplifies management. See the guide (https://docs.revenuecat.com/v1.0/docs/entitlements) for more info.
     *
     * Entitlements will be fetched and cached on instantiation so that, by the time they are needed, your prices are
     * loaded for your purchase flow. Time is money.
     *
     * @param handler Called when entitlements are available. Called immediately if entitlements are cached.
     */
    public void getEntitlements(final GetEntitlementsHandler handler) {
        if (this.cachedEntitlements != null) {
            handler.onReceiveEntitlements(this.cachedEntitlements);
        } else {
            backend.getEntitlements(getAppUserID(), new Backend.EntitlementsResponseHandler() {
                @Override
                public void onReceiveEntitlements(final Map<String, Entitlement> entitlements) {
                    final List<String> skus = new ArrayList<>();
                    final Map<String, SkuDetails> detailsByID = new HashMap<>();
                    for (Entitlement e : entitlements.values()) {
                        for (Offering o : e.getOfferings().values()) {
                            skus.add(o.getActiveProductIdentifier());
                        }
                    }

                    billingWrapper.querySkuDetailsAsync(BillingClient.SkuType.SUBS, skus, new BillingWrapper.SkuDetailsResponseListener() {
                        @Override
                        public void onReceiveSkuDetails(List<SkuDetails> skuDetails) {
                            List<String> skusCopy = new ArrayList<>(skus);
                            for (SkuDetails d : skuDetails) {
                                skusCopy.remove(d.getSku());
                                detailsByID.put(d.getSku(), d);
                            }
                            if (skusCopy.size() > 0) {
                                billingWrapper.querySkuDetailsAsync(BillingClient.SkuType.INAPP, skusCopy, new BillingWrapper.SkuDetailsResponseListener() {
                                    @Override
                                    public void onReceiveSkuDetails(List<SkuDetails> skuDetails) {
                                        for (SkuDetails d : skuDetails) {
                                            detailsByID.put(d.getSku(), d);
                                        }
                                        populateSkuDetailsAndCallHandler(detailsByID, entitlements, handler);
                                    }
                                });
                            } else {
                                populateSkuDetailsAndCallHandler(detailsByID, entitlements, handler);
                            }
                        }
                    });
                }

                @Override
                public void onError(int code, String message) {
                    handler.onReceiveEntitlementsError(ErrorDomains.REVENUECAT_BACKEND, code,"Error fetching entitlements: " + message);
                }
            });
        }
    }

    private void populateSkuDetailsAndCallHandler(Map<String, SkuDetails> details, Map<String, Entitlement> entitlements, GetEntitlementsHandler handler)
    {
        for (Entitlement e : entitlements.values()) {
            for (Offering o : e.getOfferings().values()) {
                if (details.containsKey(o.getActiveProductIdentifier())) {
                    o.setSkuDetails(details.get(o.getActiveProductIdentifier()));
                } else {
                    Log.e("Purchases", "Failed to find SKU for " + o.getActiveProductIdentifier());
                }
            }
        }
        cachedEntitlements = entitlements;
        handler.onReceiveEntitlements(entitlements);
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
            public void onReceivePurchaseHistory(final List<Purchase> subsPurchasesList) {
                billingWrapper.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP, new BillingWrapper.PurchaseHistoryResponseListener() {
                    @Override
                    public void onReceivePurchaseHistory(List<Purchase> inAppPurchasesList) {
                        List<Purchase> allPurchases = new ArrayList<>(subsPurchasesList);
                        allPurchases.addAll(inAppPurchasesList);
                        postPurchases(allPurchases, true, false);
                    }

                    @Override
                    public void onReceivePurchaseHistoryError(int responseCode, String message) {
                        listener.onRestoreTransactionsFailed(ErrorDomains.PLAY_BILLING, responseCode, message);
                    }
                });
            }
            @Override
            public void onReceivePurchaseHistoryError(int responseCode, String message) {
                listener.onRestoreTransactionsFailed(ErrorDomains.PLAY_BILLING, responseCode, message);
            }
        });
    }

    /**
     * Call close when you are done with this instance of Purchases
     */
    public void close() {
        this.billingWrapper.close();
        this.backend.close();
        this.application.unregisterActivityLifecycleCallbacks(this);
    }

    /// Private Methods
    private void getCaches() {
        if (cachesLastChecked != null && (new Date().getTime() - cachesLastChecked.getTime()) < 60000) {
            emitCachedPurchaserInfo();
            return;
        }

        cachesLastChecked = new Date();

        backend.getSubscriberInfo(appUserID, new Backend.BackendResponseHandler() {
            @Override
            public void onReceivePurchaserInfo(PurchaserInfo info) {
                deviceCache.cachePurchaserInfo(appUserID, info);
                listener.onReceiveUpdatedPurchaserInfo(info);
            }

            @Override
            public void onError(int code, String message) {
                Log.e("Purchases", "Error fetching subscriber data: " + message);
                cachesLastChecked = null;
            }
        });

        getEntitlements(new GetEntitlementsHandler() {
            @Override
            public void onReceiveEntitlements(Map<String, Entitlement> entitlementMap) {}

            @Override
            public void onReceiveEntitlementsError(int domain, int code, String message) {}
        });
    }

    private void postPurchases(List<Purchase> purchases, final Boolean isRestore, final Boolean isPurchase) {
        for (Purchase p : purchases) {
            final String token = p.getPurchaseToken();
            final String sku = p.getSku();

            if (postedTokens.contains(token)) continue;
            postedTokens.add(token);
            backend.postReceiptData(token, appUserID, sku, isRestore, new Backend.BackendResponseHandler() {
                @Override
                public void onReceivePurchaserInfo(PurchaserInfo info) {
                    billingWrapper.consumePurchase(token);

                    deviceCache.cachePurchaserInfo(appUserID, info);
                    if (isPurchase) {
                        listener.onCompletedPurchase(sku, info);
                    } else if (isRestore) {
                        listener.onRestoreTransactions(info);
                    } else {
                        listener.onReceiveUpdatedPurchaserInfo(info);
                    }
                }

                @Override
                public void onError(int code, String message) {
                    if (code < 500) {
                        billingWrapper.consumePurchase(token);
                        postedTokens.remove(token);
                    }

                    if (isPurchase) {
                        listener.onFailedPurchase(ErrorDomains.REVENUECAT_BACKEND, code, message);
                    } else if (isRestore) {
                        listener.onRestoreTransactionsFailed(ErrorDomains.REVENUECAT_BACKEND, code, message);
                    }
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
        listener.onFailedPurchase(ErrorDomains.PLAY_BILLING, responseCode, message);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        getCaches();
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
        private final String apiKey;
        private final Application application;
        private final PurchasesListener listener;
        private String appUserID;
        private ExecutorService service;

        private boolean hasPermission(Context context, String permission) {
            return context.checkCallingOrSelfPermission(permission) == PERMISSION_GRANTED;
        }

        public Builder(Context context, String apiKey, PurchasesListener listener) {
            if (context == null) {
                throw new IllegalArgumentException("Context must be set.");
            }

            if (!hasPermission(context, Manifest.permission.INTERNET)) {
                throw new IllegalArgumentException("Purchases requires INTERNET permission.");
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

            Backend backend = new Backend(this.apiKey, new Dispatcher(service), new HTTPClient(),
                                          new PurchaserInfo.Factory(), Entitlement.Factory);

            BillingWrapper.Factory billingWrapperFactory =
                    new BillingWrapper.Factory(
                            new BillingWrapper.ClientFactory(application.getApplicationContext()),
                            new Handler(application.getMainLooper()));

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.application);
            DeviceCache cache = new DeviceCache(prefs, apiKey);

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
