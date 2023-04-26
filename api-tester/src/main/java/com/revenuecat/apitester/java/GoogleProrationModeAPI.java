package com.revenuecat.apitester.java;

import com.revenuecat.purchases.models.GoogleProrationMode;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
final class GoogleProrationModeAPI {
    static void check(final GoogleProrationMode mode) {
        switch (mode) {
            case IMMEDIATE_WITHOUT_PRORATION:
            case IMMEDIATE_WITH_TIME_PRORATION:
            case DEFERRED:
            case IMMEDIATE_AND_CHARGE_FULL_PRICE:
            case IMMEDIATE_AND_CHARGE_PRORATED_PRICE:
        }
    }
}
