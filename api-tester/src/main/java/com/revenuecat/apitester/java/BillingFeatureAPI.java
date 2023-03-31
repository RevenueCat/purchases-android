package com.revenuecat.apitester.java;

import com.revenuecat.purchases.models.BillingFeature;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
final class BillingFeatureAPI {
    static void check(final BillingFeature feature) {
        switch (feature) {
            case SUBSCRIPTIONS:
            case SUBSCRIPTIONS_UPDATE:
            case PRICE_CHANGE_CONFIRMATION:
        }
    }
}
