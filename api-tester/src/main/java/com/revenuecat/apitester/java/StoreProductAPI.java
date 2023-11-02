package com.revenuecat.apitester.java;

import com.android.billingclient.api.ProductDetails;
import com.revenuecat.purchases.ProductType;
import com.revenuecat.purchases.amazon.AmazonStoreProduct;
import com.revenuecat.purchases.amazon.AmazonStoreProductKt;
import com.revenuecat.purchases.models.GoogleStoreProduct;
import com.revenuecat.purchases.models.GoogleStoreProductKt;
import com.revenuecat.purchases.models.Period;
import com.revenuecat.purchases.models.Price;
import com.revenuecat.purchases.models.StoreProduct;
import com.revenuecat.purchases.models.SubscriptionOption;
import com.revenuecat.purchases.models.SubscriptionOptions;

import org.json.JSONObject;

import java.util.Locale;

@SuppressWarnings({"unused"})
final class StoreProductAPI {
    static void check(final StoreProduct product) {
        final Locale locale = Locale.getDefault();
        final String productId = product.getId();
        final String sku = product.getSku();
        final ProductType type = product.getType();
        final Price price = product.getPrice();
        final String title = product.getTitle();
        final String description = product.getDescription();
        final Period period = product.getPeriod();
        final String formattedPricePerMonth = product.formattedPricePerMonth(locale);
        final Price pricePerWeek = product.pricePerWeek(locale);
        final Price pricePerMonth = product.pricePerMonth(locale);
        final Price pricePerYear = product.pricePerYear(locale);

        SubscriptionOptions subscriptionOptions = product.getSubscriptionOptions();
        SubscriptionOption defaultOption = product.getDefaultOption();
        String presentedOfferingIdentifier = product.getPresentedOfferingIdentifier();

        GoogleStoreProduct underlyingGoogleProduct = GoogleStoreProductKt.getGoogleProduct(product);
        AmazonStoreProduct underlyingAmazonProduct = AmazonStoreProductKt.getAmazonProduct(product);
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
        GoogleStoreProduct constructedGoogleStoreProduct = new GoogleStoreProduct(
                googleStoreProduct.getId(),
                googleStoreProduct.getBasePlanId(),
                googleStoreProduct.getType(),
                googleStoreProduct.getPrice(),
                googleStoreProduct.getTitle(),
                googleStoreProduct.getDescription(),
                googleStoreProduct.getPeriod(),
                googleStoreProduct.getSubscriptionOptions(),
                googleStoreProduct.getDefaultOption(),
                googleStoreProduct.getProductDetails(),
                googleStoreProduct.getPresentedOfferingIdentifier()
        );

        GoogleStoreProduct constructedGoogleStoreProductWithNullableFieldsNull = new GoogleStoreProduct(
                googleStoreProduct.getId(),
                null,
                googleStoreProduct.getType(),
                googleStoreProduct.getPrice(),
                googleStoreProduct.getTitle(),
                googleStoreProduct.getDescription(),
                null,
                null,
                null,
                googleStoreProduct.getProductDetails(),
                null
        );

        String productId = constructedGoogleStoreProduct.getProductId();
        String basePlanId = constructedGoogleStoreProduct.getBasePlanId();
        ProductDetails productDetails = googleStoreProduct.getProductDetails();
    }

    static void checkAmazonStoreProduct(AmazonStoreProduct amazonStoreProduct) {
        check(amazonStoreProduct);
        AmazonStoreProduct constructedAmazonStoreProduct = new AmazonStoreProduct(
                amazonStoreProduct.getId(),
                amazonStoreProduct.getType(),
                amazonStoreProduct.getTitle(),
                amazonStoreProduct.getDescription(),
                amazonStoreProduct.getPeriod(),
                amazonStoreProduct.getPrice(),
                amazonStoreProduct.getSubscriptionOptions(),
                amazonStoreProduct.getDefaultOption(),
                amazonStoreProduct.getIconUrl(),
                amazonStoreProduct.getFreeTrialPeriod(),
                amazonStoreProduct.getOriginalProductJSON(),
                amazonStoreProduct.getPresentedOfferingIdentifier()
        );

        AmazonStoreProduct constructedAmazonStoreProductWithNullOfferingId = new AmazonStoreProduct(
                amazonStoreProduct.getId(),
                amazonStoreProduct.getType(),
                amazonStoreProduct.getTitle(),
                amazonStoreProduct.getDescription(),
                null,
                amazonStoreProduct.getPrice(),
                null,
                null,
                amazonStoreProduct.getIconUrl(),
                null,
                amazonStoreProduct.getOriginalProductJSON(),
                null
        );
        String iconUrl = amazonStoreProduct.getIconUrl();
        Period freeTrialPeriod = amazonStoreProduct.getFreeTrialPeriod();
        JSONObject originalProductJson = amazonStoreProduct.getOriginalProductJSON();
    }
}
