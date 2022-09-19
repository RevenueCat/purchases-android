package com.revenuecat.apitester.java;

import com.revenuecat.purchases.BillingFeature;

@SuppressWarnings({"unused"})
final class PurchasesBC4API {
    static void check(final BillingFeature feature) {
        switch (feature) {
            case SUBSCRIPTIONS:
            case SUBSCRIPTIONS_UPDATE:
            case PRICE_CHANGE_CONFIRMATION:
            case IN_APP_ITEMS_ON_VR:
            case SUBSCRIPTIONS_ON_VR:
        }
    }
}
