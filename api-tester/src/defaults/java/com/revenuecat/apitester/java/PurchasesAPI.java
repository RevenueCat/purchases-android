package com.revenuecat.apitester.java;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;

import com.revenuecat.purchases.AmazonLWAConsentStatus;
import com.revenuecat.purchases.CacheFetchPolicy;
import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.EntitlementVerificationMode;
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI;
import com.revenuecat.purchases.Offerings;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesAreCompletedBy;
import com.revenuecat.purchases.PurchasesConfiguration;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.Store;
import com.revenuecat.purchases.amazon.AmazonConfiguration;
import com.revenuecat.purchases.interfaces.GetAmazonLWAConsentStatusCallback;
import com.revenuecat.purchases.interfaces.LogInCallback;
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback;
import com.revenuecat.purchases.interfaces.RedeemWebPurchaseListener;
import com.revenuecat.purchases.interfaces.SyncAttributesAndOfferingsCallback;
import com.revenuecat.purchases.interfaces.SyncPurchasesCallback;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ExecutorService;

@OptIn(markerClass = ExperimentalPreviewRevenueCatPurchasesAPI.class)
@SuppressWarnings({"unused"})
final class PurchasesAPI {
    static void check(
            final Purchases purchases,
            final Purchases.DeepLink.WebPurchaseRedemption deepLink,
            final RedeemWebPurchaseListener redeemWebPurchaseListener,
            final Intent intent
            ) {
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
        final SyncAttributesAndOfferingsCallback syncAttributesAndOfferingsCallback = new SyncAttributesAndOfferingsCallback() {
            @Override
            public void onError(@NonNull PurchasesError error) {
            }

            @Override
            public void onSuccess(@NonNull Offerings offerings) {
            }
        };

        final GetAmazonLWAConsentStatusCallback getAmazonLWAContentStatusCallback = new GetAmazonLWAConsentStatusCallback() {
            @Override
            public void onError(@NonNull PurchasesError error) {
            }

            @Override
            public void onSuccess(@NonNull AmazonLWAConsentStatus contentStatus) {
            }
        };

        purchases.syncAttributesAndOfferingsIfNeeded(syncAttributesAndOfferingsCallback);
        purchases.syncPurchases();
        purchases.syncPurchases(syncPurchasesCallback);
        purchases.logIn("", logInCallback);
        purchases.logOut();
        purchases.logOut(receiveCustomerInfoListener);
        purchases.getCustomerInfo(receiveCustomerInfoListener);
        purchases.getCustomerInfo(CacheFetchPolicy.CACHED_OR_FETCHED, receiveCustomerInfoListener);
        purchases.getAmazonLWAConsentStatus(getAmazonLWAContentStatusCallback);
        purchases.redeemWebPurchase(deepLink, redeemWebPurchaseListener);

        purchases.restorePurchases(receiveCustomerInfoListener);
        purchases.invalidateCustomerInfoCache();

        final boolean anonymous = purchases.isAnonymous();

        final boolean finishTransactions = purchases.getFinishTransactions();
        purchases.setFinishTransactions(true);

        purchases.onAppBackgrounded();
        purchases.onAppForegrounded();

        final Store store = purchases.getStore();

        final String storefrontCountryCode = purchases.getStorefrontCountryCode();

        final PurchasesConfiguration configuration = purchases.getCurrentConfiguration();

        final Purchases.DeepLink parsedDeepLink = Purchases.parseAsDeepLink(intent);
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
        purchases.setOnesignalUserID("");
        purchases.setAirshipChannelID("");
        purchases.setMediaSource("");
        purchases.setCampaign("");
        purchases.setCleverTapID("");
        purchases.setKochavaDeviceID("");
        purchases.setAdGroup("");
        purchases.setAd("");
        purchases.setKeyword("");
        purchases.setCreative("");
    }

    static void checkConfiguration(final Context context,
                                   final ExecutorService executorService,
                                   final PurchasesConfiguration purchasesConfiguration) {
        final boolean configured = Purchases.isConfigured();

        Purchases.configure(purchasesConfiguration);

        final boolean debugLogs = Purchases.getDebugLogsEnabled();
    }

    static void checkAmazonConfiguration(final Context context,
                                         final ExecutorService executorService,
                                         final PurchasesAreCompletedBy purchaseCompleter) {
        PurchasesConfiguration amazonConfiguration = new AmazonConfiguration.Builder(context, "")
                .appUserID("")
                .observerMode(true)
                .observerMode(false)
                .purchasesAreCompletedBy(purchaseCompleter)
                .service(executorService)
                .diagnosticsEnabled(true)
                .entitlementVerificationMode(EntitlementVerificationMode.INFORMATIONAL)
                .showInAppMessagesAutomatically(true)
                .build();
    }
}
