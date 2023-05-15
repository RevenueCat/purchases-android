package com.revenuecat.apitester.java;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.revenuecat.purchases.CacheFetchPolicy;
import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.LogHandler;
import com.revenuecat.purchases.LogLevel;
import com.revenuecat.purchases.Offerings;
import com.revenuecat.purchases.Package;
import com.revenuecat.purchases.ProductType;
import com.revenuecat.purchases.PurchaseParams;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesConfiguration;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.Store;
import com.revenuecat.purchases.interfaces.GetStoreProductsCallback;
import com.revenuecat.purchases.interfaces.LogInCallback;
import com.revenuecat.purchases.interfaces.PurchaseCallback;
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback;
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback;
import com.revenuecat.purchases.interfaces.SyncPurchasesCallback;
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener;
import com.revenuecat.purchases.models.BillingFeature;
import com.revenuecat.purchases.models.GoogleProrationMode;
import com.revenuecat.purchases.models.SubscriptionOption;
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
    static void check(final Purchases purchases) {
        final ArrayList<String> productIds = new ArrayList<>();

        final ReceiveOfferingsCallback receiveOfferingsListener = new ReceiveOfferingsCallback() {
            @Override
            public void onReceived(@NonNull Offerings offerings) {
            }

            @Override
            public void onError(@NonNull PurchasesError error) {
            }
        };
        final GetStoreProductsCallback productResponseListener = new GetStoreProductsCallback() {
            @Override
            public void onReceived(@NonNull List<StoreProduct> storeProducts) {
            }

            @Override
            public void onError(@NonNull PurchasesError error) {
            }
        };

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
            public void onSuccess() {
            }

            @Override
            public void onError(@NonNull PurchasesError error) {
            }
        };

        purchases.syncPurchases();
        purchases.syncPurchases(syncPurchasesCallback);
        purchases.getOfferings(receiveOfferingsListener);
        purchases.getProducts(productIds, productResponseListener);
        purchases.getProducts(productIds, ProductType.SUBS, productResponseListener);
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
        purchases.setUpdatedCustomerInfoListener((CustomerInfo customerInfo) -> {
        });

        final boolean anonymous = purchases.isAnonymous();

        final Store store = purchases.getStore();

        purchases.onAppBackgrounded();
        purchases.onAppForegrounded();
    }

    static void checkPurchasing(final Purchases purchases,
                                final Activity activity,
                                final StoreProduct storeProduct,
                                final Package packageToPurchase,
                                final SubscriptionOption subscriptionOption) {
        final PurchaseCallback purchaseCallback = new PurchaseCallback() {
            @Override
            public void onCompleted(@Nullable StoreTransaction storeTransaction, @NonNull CustomerInfo customerInfo) {
            }

            @Override
            public void onError(@NonNull PurchasesError error, boolean userCancelled) {
            }
        };
        String oldProductId = "old";
        GoogleProrationMode prorationMode = GoogleProrationMode.IMMEDIATE_WITH_TIME_PRORATION;
        Boolean isPersonalizedPrice = true;

        PurchaseParams.Builder purchaseProductBuilder = new PurchaseParams.Builder(activity, storeProduct);
        purchaseProductBuilder.oldProductId(oldProductId).googleProrationMode(prorationMode)
                .isPersonalizedPrice(isPersonalizedPrice);
        PurchaseParams purchaseProductParams = purchaseProductBuilder.build();
        purchases.purchase(purchaseProductParams, purchaseCallback);

        PurchaseParams.Builder purchaseOptionBuilder = new PurchaseParams.Builder(activity, subscriptionOption);
        purchaseOptionBuilder.oldProductId(oldProductId).googleProrationMode(prorationMode)
                .isPersonalizedPrice(isPersonalizedPrice);
        PurchaseParams purchaseOptionParams = purchaseOptionBuilder.build();
        purchases.purchase(purchaseOptionParams, purchaseCallback);

        PurchaseParams.Builder purchasePackageBuilder = new PurchaseParams.Builder(activity, packageToPurchase);
        purchasePackageBuilder.oldProductId(oldProductId).googleProrationMode(prorationMode)
                .isPersonalizedPrice(isPersonalizedPrice);
        PurchaseParams purchasePackageParams = purchasePackageBuilder.build();
        purchases.purchase(purchasePackageParams, purchaseCallback);

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
                .diagnosticsEnabled(true)
                // Trusted entitlements: Commented out until ready to be made public
                // .entitlementVerificationMode(EntitlementVerificationMode.INFORMATIONAL)
                .build();

        Purchases.configure(build);

        Purchases.canMakePayments(context, features, (Boolean result) -> {
        });
        Purchases.canMakePayments(context, (Boolean result) -> {
        });

        final boolean debugLogs = Purchases.getDebugLogsEnabled();

        Purchases.setLogLevel(LogLevel.DEBUG);
        final LogLevel logLevel = Purchases.getLogLevel();

        Purchases.setProxyURL(new URL(""));
        final URL proxyURL = Purchases.getProxyURL();

        final Purchases instance = Purchases.getSharedInstance();
    }

    static void checkLogHandler() {
        Purchases.setLogHandler(
                new LogHandler() {
                    @Override public void v(@NonNull String tag, @NonNull String msg) {}
                    @Override public void d(@NonNull String tag, @NonNull String msg) {}
                    @Override public void i(@NonNull String tag, @NonNull String msg) {}
                    @Override public void w(@NonNull String tag, @NonNull String msg) {}
                    @Override public void e(@NonNull String tag, @NonNull String msg, @Nullable Throwable throwable) {}
                }
        );
        final LogHandler handler = Purchases.getLogHandler();
    }

    static void checkLogLevel(final LogLevel level) {
        switch (level) {
            case VERBOSE:
            case ERROR:
            case WARN:
            case INFO:
            case DEBUG:
                break;
        }
    }
}
