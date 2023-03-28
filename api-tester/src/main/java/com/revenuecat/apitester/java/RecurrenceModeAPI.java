package com.revenuecat.apitester.java;

import com.revenuecat.purchases.models.RecurrenceMode;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
final class RecurrenceModeAPI {
    static void check(final RecurrenceMode recurrenceMode) {
        switch (recurrenceMode) {
            case NON_RECURRING:
            case FINITE_RECURRING:
            case INFINITE_RECURRING:
            case UNKNOWN:
        }
    }
}
