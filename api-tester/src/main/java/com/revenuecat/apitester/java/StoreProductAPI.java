package com.revenuecat.apitester.java;

import com.android.billingclient.api.ProductDetails;
import com.revenuecat.purchases.ProductType;
import com.revenuecat.purchases.models.GoogleSubscriptionOption;
import com.revenuecat.purchases.models.GoogleStoreProduct;
import com.revenuecat.purchases.models.GoogleStoreProductKt;
import com.revenuecat.purchases.models.Price;
import com.revenuecat.purchases.models.SubscriptionOption;
import com.revenuecat.purchases.models.StoreProduct;
import com.revenuecat.purchases.models.Period;
import com.revenuecat.purchases.models.SubscriptionOptions;

@SuppressWarnings({"unused"})
final class StoreProductAPI {
    static void check(final StoreProduct product) {
        final String productId = product.getId();
        final String sku = product.getSku();
        final ProductType type = product.getType();
        final Price price = product.getPrice();
        final String title = product.getTitle();
        final String description = product.getDescription();
        final Period period = product.getPeriod();
        SubscriptionOptions subscriptionOptions = product.getSubscriptionOptions();
        SubscriptionOption defaultOption = product.getDefaultOption();

        // TODOBC5 can we find an easier way to do this in java?
        GoogleStoreProduct underlyingProduct = GoogleStoreProductKt.getGoogleProduct(product);
    }

    static void check(final ProductType type) {
        switch (type) {
            case SUBS:
            case INAPP:
            case UNKNOWN:
        }
    }

    static void checkGoogleStoreProduct(GoogleStoreProduct googleStoreProduct) {
        check(googleStoreProduct);
        ProductDetails productDetails = googleStoreProduct.getProductDetails();
        SubscriptionOptions subscriptionOptions = googleStoreProduct.getSubscriptionOptions();
        GoogleSubscriptionOption defaultOption = googleStoreProduct.getDefaultOption();
        GoogleStoreProduct constructedGoogleStoreProduct = new GoogleStoreProduct(
                googleStoreProduct.getId(),
                null,
                googleStoreProduct.getType(),
                googleStoreProduct.getPrice(),
                googleStoreProduct.getTitle(),
                googleStoreProduct.getDescription(),
                googleStoreProduct.getPeriod(),
                googleStoreProduct.getSubscriptionOptions(),
                googleStoreProduct.getDefaultOption(),
                googleStoreProduct.getProductDetails()
        );

        String productId = constructedGoogleStoreProduct.getProductId();
        String basePlanId = constructedGoogleStoreProduct.getBasePlanId();
    }
}
