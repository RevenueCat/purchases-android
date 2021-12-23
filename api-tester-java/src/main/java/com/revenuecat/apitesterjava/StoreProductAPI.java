package com.revenuecat.apitesterjava;

import com.revenuecat.purchases.ProductType;
import com.revenuecat.purchases.models.StoreProduct;

import org.json.JSONObject;

@SuppressWarnings({"unused"})
final class StoreProductAPI {
    static void check(final StoreProduct product) {
        final String sku = product.getSku();
        final ProductType type = product.getType();
        final String price = product.getPrice();
        final long priceAmountMicros = product.getPriceAmountMicros();
        final String priceCurrencyCode = product.getPriceCurrencyCode();
        final String originalPrice = product.getOriginalPrice();
        final long originalPriceAmountMicros = product.getOriginalPriceAmountMicros();
        final String title = product.getTitle();
        final String description = product.getDescription();
        final String subscriptionPeriod = product.getSubscriptionPeriod();
        final String freeTrialPeriod = product.getFreeTrialPeriod();
        final String introductoryPrice = product.getIntroductoryPrice();
        final long introductoryPriceAmountMicros = product.getIntroductoryPriceAmountMicros();
        final String introductoryPricePeriod = product.getIntroductoryPricePeriod();
        final int introductoryPriceCycles = product.getIntroductoryPriceCycles();
        final String iconUrl = product.getIconUrl();
        final JSONObject originalJson = product.getOriginalJson();
    }

    static void check(final ProductType type) {
        switch (type) {
            case SUBS:
            case INAPP:
            case UNKNOWN:
        }
    }
}
