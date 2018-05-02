package com.revenuecat.purchases;

import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

class PurchaserInfoCache {
    private final SharedPreferences preferences;
    private final String apiKey;
    private final PurchaserInfo.Factory infoFactory;

    PurchaserInfoCache(SharedPreferences preferences, String apiKey) {
        this.preferences = preferences;
        this.apiKey = apiKey;
        this.infoFactory = new PurchaserInfo.Factory();
    }

    private String cacheKey(String appUserID) {
        return this.apiKey + "_" + appUserID;
    }

    public PurchaserInfo getCachedPurchaserInfo(String appUserID) {
        String json = preferences.getString(cacheKey(appUserID), null);
        if (json == null) {
            return null;
        }

        try {
            return this.infoFactory.build(new JSONObject(json));
        } catch (JSONException e) {
            return null;
        }
    }

    public void cachePurchaserInfo(String appUserID, PurchaserInfo info) {
        JSONObject jsonObject = info.getJSONObject();
        String jsonString = jsonObject.toString();
        preferences.edit().putString(cacheKey(appUserID), jsonString).apply();
    }
}
