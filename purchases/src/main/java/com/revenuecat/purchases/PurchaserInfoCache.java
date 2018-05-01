package com.revenuecat.purchases;

import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

class PurchaserInfoCache {
    private final SharedPreferences preferences;
    private final String cacheKey;
    private final PurchaserInfo.Factory infoFactory;

    PurchaserInfoCache(SharedPreferences preferences, String appUserID, String apiKey) {
        this.preferences = preferences;
        this.cacheKey = apiKey + "_" + appUserID;
        this.infoFactory = new PurchaserInfo.Factory();
    }

    public PurchaserInfo getCachedPurchaserInfo() {

        String json = preferences.getString(cacheKey, null);
        if (json == null) {
            return null;
        }

        try {
            return this.infoFactory.build(new JSONObject(json));
        } catch (JSONException e) {
            return null;
        }
    }

    public void cachePurchaserInfo(PurchaserInfo info) {

    }
}
