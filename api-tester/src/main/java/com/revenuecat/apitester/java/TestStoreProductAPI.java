package com.revenuecat.apitester.java;

import com.revenuecat.purchases.models.Period;
import com.revenuecat.purchases.models.Price;
import com.revenuecat.purchases.models.StoreProduct;
import com.revenuecat.purchases.models.TestStoreProduct;

@SuppressWarnings({"unused", "deprecation"})
final class TestStoreProductAPI {
    static void checkConstructors(final Price price, final Period period) {
        new TestStoreProduct(
                "id", "title", "description", price, period, null, null
        );
        new TestStoreProduct(
                "id",  "title", "description", price, period, period, price
        );
        new TestStoreProduct(
             "id", "name", "title", "description", price, period, null, null
        );
        new TestStoreProduct(
            "id", "name", "title", "description", price, period, period, price
        );
    }

    static void checkTestStoreProductIsStoreProduct(final TestStoreProduct testStoreProduct) {
        StoreProduct storeProduct = testStoreProduct;
    }
}
