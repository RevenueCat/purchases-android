package com.revenuecat.apitester.java;

import com.revenuecat.purchases.VirtualCurrencies;
import com.revenuecat.purchases.VirtualCurrency;

import java.util.Map;

final class VirtualCurrenciesAPI {
    static void check(final VirtualCurrencies virtualCurrencies){
        final Map<String, VirtualCurrency> all = virtualCurrencies.getAll();
        final Map<String, VirtualCurrency> withZeroBalance = virtualCurrencies.getWithZeroBalance();
        final Map<String, VirtualCurrency> withNonZeroBalance = virtualCurrencies.getWithNonZeroBalance();
        final VirtualCurrency testGetVC = virtualCurrencies.get("COIN");
    }
}
