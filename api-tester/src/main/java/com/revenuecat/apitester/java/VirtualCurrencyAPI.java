package com.revenuecat.apitester.java;

import androidx.annotation.OptIn;

import com.revenuecat.purchases.InternalRevenueCatAPI;
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrency;

final class VirtualCurrencyAPI {
    static void check(final VirtualCurrency virtualCurrency) {
        final int balance = virtualCurrency.getBalance();
        final Integer balance2 = virtualCurrency.getBalance();
        final String name = virtualCurrency.getName();
        final String code = virtualCurrency.getCode();
        final String serverDescription = virtualCurrency.getServerDescription();
    }

    @OptIn(markerClass = InternalRevenueCatAPI.class)
    static void checkInternalRevenueCatAPIs() {
        VirtualCurrency virtualCurrencyWithServerDescription = new VirtualCurrency(
                100,
                "Gold",
                "GLD",
                "This is a test currency."
        );

        VirtualCurrency virtualCurrencyWithoutServerDescription = new VirtualCurrency(
                100,
                "Gold",
                "GLD",
                null
        );
    }
}
