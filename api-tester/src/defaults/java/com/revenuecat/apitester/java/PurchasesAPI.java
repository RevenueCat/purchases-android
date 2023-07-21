package com.revenuecat.apitester.java;

import android.content.Context;

import androidx.annotation.NonNull;

import com.revenuecat.purchases.CacheFetchPolicy;
import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.EntitlementVerificationMode;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesConfiguration;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.Store;
import com.revenuecat.purchases.interfaces.LogInCallback;
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback;
import com.revenuecat.purchases.interfaces.SyncPurchasesCallback;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ExecutorService;

@SuppressWarnings({"unused"})
final class PurchasesAPI {
    static void check(final Purchases purchases) {
        final ReceiveCustomerInfoCallback receiveCustomerInfoListener = new ReceiveCustomerInfoCallback() {
            @Override
            public void onReceived(@NonNull CustomerInfo customerInfo) {
            }

            @Override
            public void onError(@NonNull PurchasesError error) {
            }
        };
        final LogInCallback logInCallback = new LogInCallback() {
            @Override
            public void onReceived(@NotNull CustomerInfo customerInfo, boolean created) {
            }

            @Override
            public void onError(@NotNull PurchasesError error) {
            }
        };
        final SyncPurchasesCallback syncPurchasesCallback = new SyncPurchasesCallback() {
            @Override
            public void onSuccess(@NonNull CustomerInfo customerInfo) {
            }

            @Override
            public void onError(@NonNull PurchasesError error) {
            }
        };

        purchases.syncPurchases();
        purchases.syncPurchases(syncPurchasesCallback);
        purchases.logIn("", logInCallback);
        purchases.logOut();
        purchases.logOut(receiveCustomerInfoListener);
        purchases.getCustomerInfo(receiveCustomerInfoListener);
        purchases.getCustomerInfo(CacheFetchPolicy.CACHED_OR_FETCHED, receiveCustomerInfoListener);

        purchases.restorePurchases(receiveCustomerInfoListener);
        purchases.invalidateCustomerInfoCache();

        final boolean anonymous = purchases.isAnonymous();

        final boolean finishTransactions = purchases.getFinishTransactions();
        purchases.setFinishTransactions(true);

        purchases.onAppBackgrounded();
        purchases.onAppForegrounded();

        final Store store = purchases.getStore();
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
                                   final ExecutorService executorService) {
        final boolean configured = Purchases.isConfigured();

        PurchasesConfiguration build = new PurchasesConfiguration.Builder(context, "")
                .appUserID("")
                .observerMode(true)
                .observerMode(false)
                .service(executorService)
                .diagnosticsEnabled(true)
                .entitlementVerificationMode(EntitlementVerificationMode.INFORMATIONAL)
                .build();

        Purchases.configure(build);

        final boolean debugLogs = Purchases.getDebugLogsEnabled();
    }
}
