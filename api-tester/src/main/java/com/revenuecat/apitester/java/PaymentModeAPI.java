package com.revenuecat.apitester.java;

import com.revenuecat.purchases.models.PaymentMode;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
final class PaymentModeAPI {
    static void check(final PaymentMode paymentMode) {
        switch (paymentMode) {
            case PAY_AS_YOU_GO:
            case PAY_UP_FRONT:
            case FREE_TRIAL:
        }
    }
}
