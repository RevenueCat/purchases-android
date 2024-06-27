package com.revenuecat.apitester.java;

import com.revenuecat.purchases.models.GoogleReplacementMode;

@SuppressWarnings({"unused"})
final class GoogleReplacementModeAPI {
    static void check(final GoogleReplacementMode mode) {
        switch (mode) {
            case WITHOUT_PRORATION:
            case WITH_TIME_PRORATION:
            case CHARGE_FULL_PRICE:
            case CHARGE_PRORATED_PRICE:
            case DEFERRED:
        }
    }
}
