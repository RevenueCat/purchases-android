package com.revenuecat.apitester.java;

import com.revenuecat.purchases.models.OfferPaymentMode;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
final class OfferPaymentModeAPI {
    static void check(final OfferPaymentMode offerPaymentMode) {
        switch (offerPaymentMode) {
            case PAY_AS_YOU_GO:
            case PAY_UP_FRONT:
            case FREE_TRIAL:
        }
    }
}
