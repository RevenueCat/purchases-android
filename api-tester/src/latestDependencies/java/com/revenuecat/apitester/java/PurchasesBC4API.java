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
final class PurchasesBC4API {
    static void check(final BillingFeature feature) {
        switch (feature) {
            case SUBSCRIPTIONS:
            case SUBSCRIPTIONS_UPDATE:
            case PRICE_CHANGE_CONFIRMATION:
            case IN_APP_ITEMS_ON_VR:
            case SUBSCRIPTIONS_ON_VR:
        }
    }
}
