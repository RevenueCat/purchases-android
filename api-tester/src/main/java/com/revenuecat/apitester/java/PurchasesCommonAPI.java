package com.revenuecat.apitester.java;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.EntitlementVerificationMode;
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
import com.revenuecat.purchases.interfaces.PurchaseCallback;
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback;
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener;
import com.revenuecat.purchases.models.BillingFeature;
import com.revenuecat.purchases.models.InAppMessageType;
import com.revenuecat.purchases.models.GoogleReplacementMode;
import com.revenuecat.purchases.models.StoreProduct;
import com.revenuecat.purchases.models.StoreTransaction;
import com.revenuecat.purchases.models.SubscriptionOption;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@SuppressWarnings({"unused"})
final class PurchasesCommonAPI {
    static void check(final Purchases purchases, final Activity activity) {
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

        purchases.getOfferings(receiveOfferingsListener);
        purchases.getProducts(productIds, productResponseListener);
        purchases.getProducts(productIds, ProductType.SUBS, productResponseListener);

        final String appUserID = purchases.getAppUserID();
        purchases.removeUpdatedCustomerInfoListener();
        purchases.close();

        final UpdatedCustomerInfoListener updatedCustomerInfoListener = purchases.getUpdatedCustomerInfoListener();
        purchases.setUpdatedCustomerInfoListener((CustomerInfo customerInfo) -> {
        });

        final List<InAppMessageType> inAppMessageTypeList = new ArrayList<>();
        purchases.showInAppMessagesIfNeeded(activity);
        purchases.showInAppMessagesIfNeeded(activity, inAppMessageTypeList);
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
        GoogleReplacementMode replacementMode = GoogleReplacementMode.WITH_TIME_PRORATION;
        Boolean isPersonalizedPrice = true;

        PurchaseParams.Builder purchaseProductBuilder = new PurchaseParams.Builder(activity, storeProduct);
        purchaseProductBuilder
                .oldProductId(oldProductId)
                .googleReplacementMode(replacementMode)
                .isPersonalizedPrice(isPersonalizedPrice);
        PurchaseParams purchaseProductParams = purchaseProductBuilder.build();
        purchases.purchase(purchaseProductParams, purchaseCallback);

        PurchaseParams.Builder purchaseOptionBuilder = new PurchaseParams.Builder(activity, subscriptionOption);
        purchaseOptionBuilder
                .oldProductId(oldProductId)
                .googleReplacementMode(replacementMode)
                .isPersonalizedPrice(isPersonalizedPrice);
        PurchaseParams purchaseOptionParams = purchaseOptionBuilder.build();
        purchases.purchase(purchaseOptionParams, purchaseCallback);

        PurchaseParams.Builder purchasePackageBuilder = new PurchaseParams.Builder(activity, packageToPurchase);
        purchasePackageBuilder
                .oldProductId(oldProductId)
                .googleReplacementMode(replacementMode)
                .isPersonalizedPrice(isPersonalizedPrice);
        PurchaseParams purchasePackageParams = purchasePackageBuilder.build();
        purchases.purchase(purchasePackageParams, purchaseCallback);

    }

    static void checkConfiguration(final Context context,
                                   final ExecutorService executorService) throws MalformedURLException {
        final List<? extends BillingFeature> features = new ArrayList<>();

        final boolean configured = Purchases.isConfigured();

        Purchases.canMakePayments(context, features, (Boolean result) -> {
        });
        Purchases.canMakePayments(context, (Boolean result) -> {
        });

        Purchases.setLogLevel(LogLevel.DEBUG);
        final LogLevel logLevel = Purchases.getLogLevel();

        Purchases.setProxyURL(new URL(""));
        final URL proxyURL = Purchases.getProxyURL();

        PurchasesConfiguration build = new PurchasesConfiguration.Builder(context, "")
                .appUserID("")
                .observerMode(true)
                .observerMode(false)
                .service(executorService)
                .diagnosticsEnabled(true)
                .entitlementVerificationMode(EntitlementVerificationMode.INFORMATIONAL)
                .showInAppMessagesAutomatically(true)
                .store(Store.APP_STORE)
                .build();

        final Boolean showInAppMessagesAutomatically = build.getShowInAppMessagesAutomatically();

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
