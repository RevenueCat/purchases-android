package com.revenuecat.apitester.java;

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI;
import com.revenuecat.purchases.VirtualCurrencyInfo;

final class VirtualCurrencyInfoAPI {
    @ExperimentalPreviewRevenueCatPurchasesAPI
    static void check(final VirtualCurrencyInfo virtualCurrencyInfo) {
        final int balance = virtualCurrencyInfo.getBalance();
        final Integer balance2 = virtualCurrencyInfo.getBalance();
    }
}
