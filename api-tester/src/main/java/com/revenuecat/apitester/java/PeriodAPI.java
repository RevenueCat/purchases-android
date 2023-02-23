package com.revenuecat.apitester.java;

import com.revenuecat.purchases.models.Period;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
final class PeriodAPI {
    static void check(final Period period) {
        int val = period.getValue();
        Period.Unit unit = period.getUnit();
        String iso8601 = period.getIso8601();

        switch (unit) {
            case DAY:
            case WEEK:
            case MONTH:
            case YEAR:
            case UNKNOWN:
        }
    }
}
