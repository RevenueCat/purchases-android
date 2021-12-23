package com.revenuecat.apitesterjava;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;
import com.revenuecat.purchases.BillingFeature;
import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.Offerings;
import com.revenuecat.purchases.Package;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.UpgradeInfo;
import com.revenuecat.purchases.interfaces.GetSkusResponseListener;
import com.revenuecat.purchases.interfaces.LogInCallback;
import com.revenuecat.purchases.interfaces.MakePurchaseListener;
import com.revenuecat.purchases.interfaces.ProductChangeListener;
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoListener;
import com.revenuecat.purchases.interfaces.ReceiveOfferingsListener;
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

@SuppressWarnings({"unused"})
final class PurchasesAPI {
    static void check(final Purchases purchases,
                      final Activity activity,
                      final SkuDetails skuDetails,
                      final Package packageToPurchase,
                      final UpgradeInfo upgradeInfo) {
        final ArrayList<String> skus = new ArrayList<>();

        final ReceiveOfferingsListener receiveOfferingsListener = new ReceiveOfferingsListener() {
            @Override public void onReceived(@NonNull Offerings offerings) {}
            @Override public void onError(@NonNull PurchasesError error) {}
        };
        final GetSkusResponseListener skusResponseListener = new GetSkusResponseListener() {
            @Override public void onReceived(@NonNull List<SkuDetails> skus) {}
            @Override public void onError(@NonNull PurchasesError error) {}
        };
        final ProductChangeListener purchaseChangeListener = new ProductChangeListener() {
            @Override public void onCompleted(@Nullable Purchase purchase, @NonNull CustomerInfo customerInfo) {}
            @Override public void onError(@NonNull PurchasesError error, boolean userCancelled) {}
        };
        final MakePurchaseListener makePurchaseListener = new MakePurchaseListener() {
            @Override public void onCompleted(@NonNull Purchase purchase, @NonNull CustomerInfo customerInfo) {}
            @Override public void onError(@NonNull PurchasesError error, boolean userCancelled) {}
        };
        final ReceiveCustomerInfoListener receiveCustomerInfoListener = new ReceiveCustomerInfoListener() {
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
        purchases.purchaseProduct(activity, skuDetails, upgradeInfo, purchaseChangeListener);
        purchases.purchaseProduct(activity, skuDetails, makePurchaseListener);
        purchases.purchasePackage(activity, packageToPurchase, upgradeInfo, purchaseChangeListener);
        purchases.purchasePackage(activity, packageToPurchase, makePurchaseListener);
        purchases.restorePurchases(receiveCustomerInfoListener);

        purchases.logIn("", logInCallback);
        purchases.logOut();
        purchases.logOut(receiveCustomerInfoListener);
        final String appUserID = purchases.getAppUserID();
        purchases.getCustomerInfo(receiveCustomerInfoListener);
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
        purchases.setAdGroup("");
        purchases.setAd("");
        purchases.setKeyword("");
        purchases.setCreative("");
    }

    static void checkConfiguration(final Context context, final ExecutorService executorService) {
        final List<? extends BillingFeature> features = new ArrayList<>();

        final boolean configured = Purchases.isConfigured();

        Purchases.configure(context, "");
        Purchases.configure(context, "", "");
        Purchases.configure(context, "", "", true);
        Purchases.configure(context, "", "", false, executorService);

        Purchases.canMakePayments(context, features, (Boolean result) -> {});
        Purchases.canMakePayments(context, (Boolean result) -> {});
    }


    static void check(final Purchases.AttributionNetwork network) {
        switch (network) {
            case ADJUST:
            case APPSFLYER:
            case BRANCH:
            case TENJIN:
            case FACEBOOK:
            case MPARTICLE:
        }
    }
}
