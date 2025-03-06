package com.revenuecat.apitester.java;

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI;
import com.revenuecat.purchases.VirtualCurrencyInfo;

final class VirtualCurrencyInfoAPI {
    @ExperimentalPreviewRevenueCatPurchasesAPI
    static void check(final VirtualCurrencyInfo virtualCurrencyInfo) {
        final long balance = virtualCurrencyInfo.getBalance();
        final Long balance2 = virtualCurrencyInfo.getBalance();
    }
}
