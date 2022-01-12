package com.revenuecat.apitester.java;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;
import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.Offerings;
import com.revenuecat.purchases.Package;
import com.revenuecat.purchases.PurchaserInfo;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.UpgradeInfo;
import com.revenuecat.purchases.interfaces.GetSkusResponseListener;
import com.revenuecat.purchases.interfaces.MakePurchaseListener;
import com.revenuecat.purchases.interfaces.ProductChangeCallback;
import com.revenuecat.purchases.interfaces.ProductChangeListener;
import com.revenuecat.purchases.interfaces.PurchaseCallback;
import com.revenuecat.purchases.interfaces.ReceiveOfferingsListener;
import com.revenuecat.purchases.interfaces.ReceivePurchaserInfoListener;
import com.revenuecat.purchases.interfaces.UpdatedPurchaserInfoListener;
import com.revenuecat.purchases.models.StoreProduct;
import com.revenuecat.purchases.models.StoreTransaction;

import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@SuppressWarnings({"unused"})
final class DeprecatedPurchasesAPI {
    static void check(final Purchases purchases,
                      final Activity activity,
                      final SkuDetails skuDetails,
                      final StoreProduct storeProduct,
                      final Package packageToPurchase,
                      final UpgradeInfo upgradeInfo) {
        final ArrayList<String> skus = new ArrayList<>();

        final ReceiveOfferingsListener receiveOfferingsListener = new ReceiveOfferingsListener() {
            @Override public void onReceived(@NonNull Offerings offerings) {}
            @Override public void onError(@NonNull PurchasesError error) {}
        };
        final GetSkusResponseListener skusResponseListener = new GetSkusResponseListener() {
            @Override public void onReceived(@NonNull List<? extends SkuDetails> skus) {}
            @Override public void onError(@NonNull PurchasesError error) {}
        };
        final ProductChangeListener productChangeListener = new ProductChangeListener() {
            @Override public void onCompleted(@Nullable Purchase purchase, @NonNull PurchaserInfo customerInfo) {}
            @Override public void onError(@NonNull PurchasesError error, boolean userCancelled) {}
        };
        final MakePurchaseListener makePurchaseListener = new MakePurchaseListener() {
            @Override public void onCompleted(@NonNull Purchase purchase, @NonNull PurchaserInfo customerInfo) {}
            @Override public void onError(@NonNull PurchasesError error, boolean userCancelled) {}
        };
        final ReceivePurchaserInfoListener receiveCustomerInfoListener = new ReceivePurchaserInfoListener() {
            @Override public void onReceived(@NonNull PurchaserInfo customerInfo) {}
            @Override public void onError(@NonNull PurchasesError error) {}
        };

        purchases.getOfferings(receiveOfferingsListener);
        purchases.getSubscriptionSkus(skus, skusResponseListener);
        purchases.getNonSubscriptionSkus(skus, skusResponseListener);

        final ProductChangeCallback productChangeCallback = new ProductChangeCallback() {
            @Override public void onCompleted(@Nullable StoreTransaction storeTransaction, @NonNull CustomerInfo customerInfo) { }
            @Override public void onError(@NonNull PurchasesError error, boolean userCancelled) {}
        };

        final PurchaseCallback purchaseCallback = new PurchaseCallback() {
            @Override public void onCompleted(@Nullable StoreTransaction storeTransaction, @NonNull CustomerInfo customerInfo) { }
            @Override public void onError(@NonNull PurchasesError error, boolean userCancelled) {}
        };

        purchases.purchaseProduct(activity, skuDetails, upgradeInfo, productChangeListener);
        purchases.purchaseProduct(activity, storeProduct, upgradeInfo, productChangeListener);
        purchases.purchaseProduct(activity, skuDetails, upgradeInfo, productChangeCallback);
        purchases.purchaseProduct(activity, skuDetails, makePurchaseListener);
        purchases.purchaseProduct(activity, storeProduct, makePurchaseListener);
        purchases.purchaseProduct(activity, skuDetails, purchaseCallback);
        purchases.purchasePackage(activity, packageToPurchase, upgradeInfo, productChangeListener);
        purchases.purchasePackage(activity, packageToPurchase, makePurchaseListener);
        purchases.restorePurchases(receiveCustomerInfoListener);

        purchases.logOut(receiveCustomerInfoListener);

        purchases.getPurchaserInfo(receiveCustomerInfoListener);
        purchases.getPurchaserInfo(new ReceivePurchaserInfoListener() {
            @Override public void onReceived(@NonNull PurchaserInfo purchaserInfo) { }
            @Override public void onError(@NonNull PurchasesError error) { }
        });
        purchases.getCustomerInfo(receiveCustomerInfoListener);
        purchases.invalidatePurchaserInfoCache();
        purchases.removeUpdatedPurchaserInfoListener();

        final UpdatedPurchaserInfoListener updatedPurchaserInfoListener = purchases.getUpdatedPurchaserInfoListener();
        purchases.setUpdatedPurchaserInfoListener((PurchaserInfo purchaserInfo) -> {});

        purchases.setAllowSharingPlayStoreAccount(true);
    }

    static void checkConfiguration(final Context context,
                                   final ExecutorService executorService) throws MalformedURLException {
        Purchases.configure(context, "");
        Purchases.configure(context, "", "");
        Purchases.configure(context, "", "", true);
        Purchases.configure(context, "", "", false, executorService);
    }
}
