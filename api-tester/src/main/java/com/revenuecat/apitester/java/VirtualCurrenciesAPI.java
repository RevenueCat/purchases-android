package com.revenuecat.apitester.java;

import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencies;
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrency;

import java.util.Map;

final class VirtualCurrenciesAPI {
    static void check(final VirtualCurrencies virtualCurrencies){
        final Map<String, VirtualCurrency> all = virtualCurrencies.getAll();
        final VirtualCurrency testGetVC = virtualCurrencies.get("COIN");
    }
}
