package com.revenuecat.apitester.java;

import com.revenuecat.purchases.models.Period;
import com.revenuecat.purchases.models.Price;
import com.revenuecat.purchases.models.TestStoreProduct;

@SuppressWarnings({"unused"})
final class TestStoreProductAPI {
    static void check(final Price price, final Period period) {
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
}
