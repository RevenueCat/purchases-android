package com.revenuecat.apitester.java;

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI;
import com.revenuecat.purchases.VirtualCurrency;

final class VirtualCurrencyAPI {
    @ExperimentalPreviewRevenueCatPurchasesAPI
    static void check(final VirtualCurrency virtualCurrency) {
        final int balance = virtualCurrency.getBalance();
        final Integer balance2 = virtualCurrency.getBalance();
    }
}
