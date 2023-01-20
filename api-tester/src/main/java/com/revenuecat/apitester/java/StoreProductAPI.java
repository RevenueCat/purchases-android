package com.revenuecat.apitester.java;

import com.android.billingclient.api.ProductDetails;
import com.revenuecat.purchases.ProductType;
import com.revenuecat.purchases.models.GoogleSubscriptionOption;
import com.revenuecat.purchases.models.GoogleStoreProduct;
import com.revenuecat.purchases.models.GoogleStoreProductKt;
import com.revenuecat.purchases.models.Price;
import com.revenuecat.purchases.models.SubscriptionOption;
import com.revenuecat.purchases.models.StoreProduct;

import java.util.List;

@SuppressWarnings({"unused"})
final class StoreProductAPI {
    static void check(final StoreProduct product) {
        final String productId = product.getProductId();
        final String sku = product.getSku();
        final ProductType type = product.getType();
        final Price price = product.getOneTimeProductPrice();
        final String title = product.getTitle();
        final String description = product.getDescription();
        final String subscriptionPeriod = product.getSubscriptionPeriod();
        List<SubscriptionOption> subscriptionOptions = product.getSubscriptionOptions();
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
        List<GoogleSubscriptionOption> purchaseOptions = googleStoreProduct.getSubscriptionOptions();
        GoogleStoreProduct constructedGoogleStoreProduct = new GoogleStoreProduct(
                googleStoreProduct.getProductId(),
                googleStoreProduct.getType(),
                googleStoreProduct.getOneTimeProductPrice(),
                googleStoreProduct.getTitle(),
                googleStoreProduct.getDescription(),
                googleStoreProduct.getSubscriptionPeriod(),
                googleStoreProduct.getSubscriptionOptions(),
                googleStoreProduct.getDefaultOption(),
                googleStoreProduct.getProductDetails()
        );
    }
}
