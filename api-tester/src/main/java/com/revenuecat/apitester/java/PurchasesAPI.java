package com.revenuecat.apitester.java;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.revenuecat.purchases.BillingFeature;
import com.revenuecat.purchases.CacheFetchPolicy;
import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.LogHandler;
import com.revenuecat.purchases.Offerings;
import com.revenuecat.purchases.Package;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesConfiguration;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.UpgradeInfo;
import com.revenuecat.purchases.interfaces.GetStoreProductsCallback;
import com.revenuecat.purchases.interfaces.LogInCallback;
import com.revenuecat.purchases.interfaces.ProductChangeCallback;
import com.revenuecat.purchases.interfaces.PurchaseCallback;
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback;
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback;
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener;
import com.revenuecat.purchases.models.StoreProduct;
import com.revenuecat.purchases.models.StoreTransaction;

import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

@SuppressWarnings({"unused"})
final class PurchasesAPI {
    static void check(final Purchases purchases,
                      final Activity activity,
                      final StoreProduct storeProduct,
                      final Package packageToPurchase,
                      final UpgradeInfo upgradeInfo) {
        final ArrayList<String> skus = new ArrayList<>();

        final ReceiveOfferingsCallback receiveOfferingsListener = new ReceiveOfferingsCallback() {
            @Override public void onReceived(@NonNull Offerings offerings) {}
            @Override public void onError(@NonNull PurchasesError error) {}
        };
        final GetStoreProductsCallback skusResponseListener = new GetStoreProductsCallback() {
            @Override public void onReceived(@NonNull List<? extends StoreProduct> storeProducts) { }
            @Override public void onError(@NonNull PurchasesError error) {}
        };
        final ProductChangeCallback purchaseChangeListener = new ProductChangeCallback() {
            @Override public void onCompleted(@Nullable StoreTransaction storeTransaction, @NonNull CustomerInfo customerInfo) { }
            @Override public void onError(@NonNull PurchasesError error, boolean userCancelled) {}
        };
        final PurchaseCallback makePurchaseListener = new PurchaseCallback() {
            @Override public void onCompleted(@NonNull StoreTransaction storeTransaction, @NonNull CustomerInfo customerInfo) { }
            @Override public void onError(@NonNull PurchasesError error, boolean userCancelled) {}
        };
        final ReceiveCustomerInfoCallback receiveCustomerInfoListener = new ReceiveCustomerInfoCallback() {
            @Override public void onReceived(@NonNull CustomerInfo customerInfo) {}
            @Override public void onError(@NonNull PurchasesError error) {}
        };
        final LogInCallback logInCallback = new LogInCallback() {
            @Override public void onReceived(@NotNull CustomerInfo customerInfo, boolean created) {}
            @Override public void onError(@NotNull PurchasesError error) {}
        };

        purchases.syncPurchases();
        purchases.getOfferings(receiveOfferingsListener);
        purchases.getSubscriptionSkus(skus, skusResponseListener);
        purchases.getNonSubscriptionSkus(skus, skusResponseListener);
        purchases.purchaseProduct(activity, storeProduct, upgradeInfo, purchaseChangeListener);
        purchases.purchaseProduct(activity, storeProduct, makePurchaseListener);
        purchases.purchasePackage(activity, packageToPurchase, upgradeInfo, purchaseChangeListener);
        purchases.purchasePackage(activity, packageToPurchase, makePurchaseListener);
        purchases.restorePurchases(receiveCustomerInfoListener);

        purchases.logIn("", logInCallback);
        purchases.logOut();
        purchases.logOut(receiveCustomerInfoListener);
        final String appUserID = purchases.getAppUserID();
        purchases.getCustomerInfo(receiveCustomerInfoListener);
        purchases.getCustomerInfo(CacheFetchPolicy.CACHED_OR_FETCHED, receiveCustomerInfoListener);
        purchases.removeUpdatedCustomerInfoListener();
        purchases.invalidateCustomerInfoCache();
        purchases.close();

        final boolean finishTransactions = purchases.getFinishTransactions();
        purchases.setFinishTransactions(true);
        final UpdatedCustomerInfoListener updatedCustomerInfoListener = purchases.getUpdatedCustomerInfoListener();
        purchases.setUpdatedCustomerInfoListener((CustomerInfo customerInfo) -> {});

        final boolean anonymous = purchases.isAnonymous();

        purchases.onAppBackgrounded();
        purchases.onAppForegrounded();
    }

    static void check(final Purchases purchases, final Map<String, String> attributes) {
        purchases.setAttributes(attributes);
        purchases.setEmail("");
        purchases.setPhoneNumber("");
        purchases.setDisplayName("");
        purchases.setPushToken("");
        purchases.collectDeviceIdentifiers();
        purchases.setAdjustID("");
        purchases.setAppsflyerID("");
        purchases.setFBAnonymousID("");
        purchases.setMparticleID("");
        purchases.setOnesignalID("");
        purchases.setAirshipChannelID("");
        purchases.setMediaSource("");
        purchases.setCampaign("");
        purchases.setCleverTapID("");
        purchases.setAdGroup("");
        purchases.setAd("");
        purchases.setKeyword("");
        purchases.setCreative("");
    }

    static void checkConfiguration(final Context context,
                                   final ExecutorService executorService) throws MalformedURLException {
        final List<? extends BillingFeature> features = new ArrayList<>();

        final boolean configured = Purchases.isConfigured();

        PurchasesConfiguration build = new PurchasesConfiguration.Builder(context, "")
                .appUserID("")
                .observerMode(true)
                .observerMode(false)
                .service(executorService)
                .build();

        Purchases.configure(build);

        Purchases.canMakePayments(context, features, (Boolean result) -> {});
        Purchases.canMakePayments(context, (Boolean result) -> {});

        Purchases.setDebugLogsEnabled(false);
        final boolean debugLogs = Purchases.getDebugLogsEnabled();

        Purchases.setProxyURL(new URL(""));
        final URL proxyURL = Purchases.getProxyURL();

        final Purchases instance = Purchases.getSharedInstance();
    }

    static void checkLogHandler() {
        Purchases.setLogHandler(
                new LogHandler() {
                    @Override public void d(@NonNull String tag, @NonNull String msg) {}
                    @Override public void i(@NonNull String tag, @NonNull String msg) {}
                    @Override public void w(@NonNull String tag, @NonNull String msg) {}
                    @Override public void e(@NonNull String tag, @NonNull String msg, @Nullable Throwable throwable) {}
                }
        );
        final LogHandler handler = Purchases.getLogHandler();
    }

}
