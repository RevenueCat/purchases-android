package com.revenuecat.apitester.java;

import com.revenuecat.purchases.models.PurchaseState;

@SuppressWarnings({"unused"})
final class PurchaseStateAPI {
    static void check(final PurchaseState state) {
        switch (state) {
            case UNSPECIFIED_STATE:
            case PURCHASED:
            case PENDING:
        }
    }
}
