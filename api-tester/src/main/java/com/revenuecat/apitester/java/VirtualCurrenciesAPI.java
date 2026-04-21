package com.revenuecat.apitester.java;

import androidx.annotation.OptIn;

import com.revenuecat.purchases.InternalRevenueCatAPI;
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencies;
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrency;

import java.util.HashMap;
import java.util.Map;

final class VirtualCurrenciesAPI {
    static void check(final VirtualCurrencies virtualCurrencies){
        final Map<String, VirtualCurrency> all = virtualCurrencies.getAll();
        final VirtualCurrency testGetVC = virtualCurrencies.get("COIN");
    }

    @OptIn(markerClass = InternalRevenueCatAPI.class)
    static void checkInternalRevenueCatAPIs() {
        final VirtualCurrencies vcs = new VirtualCurrencies(new HashMap<String, VirtualCurrency>());
    }
}
