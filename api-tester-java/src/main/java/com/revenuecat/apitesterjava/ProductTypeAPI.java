package com.revenuecat.apitesterjava;

import com.revenuecat.purchases.ProductType;

@SuppressWarnings({"unused"})
final class ProductTypeAPI {
    static void check(final ProductType type) {
        switch (type) {
            case SUBS:
            case INAPP:
            case UNKNOWN:
        }
    }
}
