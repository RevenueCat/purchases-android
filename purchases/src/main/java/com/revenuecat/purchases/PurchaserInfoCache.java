package com.revenuecat.purchases;

import android.content.SharedPreferences;

class PurchaserInfoCache {
    private final SharedPreferences preferences;
    private final String cacheKey;

    PurchaserInfoCache(SharedPreferences preferences, String appUserID, String apiKey) {
        this.preferences = preferences;
        this.cacheKey = apiKey + "_" + appUserID;
    }

    public PurchaserInfo getCachedPurchaserInfo() {

        String json = preferences.getString(cacheKey, null);
        if (json == null) {
            return null;
        }
        throw new RuntimeException("");
    }

    public void cachePurchaserInfo(PurchaserInfo info) {

    }
}
