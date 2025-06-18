package com.revenuecat.apitester.java;

import com.revenuecat.purchases.virtualcurrencies.VirtualCurrency;

final class VirtualCurrencyAPI {
    static void check(final VirtualCurrency virtualCurrency) {
        final int balance = virtualCurrency.getBalance();
        final Integer balance2 = virtualCurrency.getBalance();
        final String name = virtualCurrency.getName();
        final String code = virtualCurrency.getCode();
        final String serverDescription = virtualCurrency.getServerDescription();
    }
}
