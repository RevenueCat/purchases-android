package com.revenuecat.apitester.java;

import com.revenuecat.purchases.ProductType;
import com.revenuecat.purchases.models.Price;
import com.revenuecat.purchases.models.StoreProduct;

import org.json.JSONObject;

@SuppressWarnings({"unused"})
final class StoreProductAPI {
    static void check(final StoreProduct product) {
        final String sku = product.getSku(); //TODOBC5 rename
        final ProductType type = product.getType();
        final Price price = product.getOneTimeProductPrice();
        final String title = product.getTitle();
        final String description = product.getDescription();
        final String subscriptionPeriod = product.getSubscriptionPeriod();
    }

    static void check(final ProductType type) {
        switch (type) {
            case SUBS:
            case INAPP:
            case UNKNOWN:
        }
    }
}
