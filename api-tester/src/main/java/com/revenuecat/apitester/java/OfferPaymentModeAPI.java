package com.revenuecat.apitester.java;

import com.revenuecat.purchases.models.OfferPaymentMode;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
final class OfferPaymentModeAPI {
    static void check(final OfferPaymentMode offerPaymentMode) {
        switch (offerPaymentMode) {
            case DISCOUNTED_RECURRING_PAYMENT:
            case SINGLE_PAYMENT:
            case FREE_TRIAL:
        }
    }
}
