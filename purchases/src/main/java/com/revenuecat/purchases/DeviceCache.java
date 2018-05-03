package com.revenuecat.purchases;

import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

class DeviceCache {
    private final SharedPreferences preferences;
    private final String apiKey;
    private final PurchaserInfo.Factory infoFactory;

    DeviceCache(SharedPreferences preferences, String apiKey) {
        this.preferences = preferences;
        this.apiKey = apiKey;
        this.infoFactory = new PurchaserInfo.Factory();
    }

    private String purchaserInfoCacheKey(String appUserID) {
        return appUserIDCacheKey() + "." + appUserID;
    }

    private String appUserIDCacheKey() {
        return "com.revenuecat.purchases." + apiKey;
    }

    public PurchaserInfo getCachedPurchaserInfo(String appUserID) {
        String json = preferences.getString(purchaserInfoCacheKey(appUserID), null);
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
        preferences.edit().putString(purchaserInfoCacheKey(appUserID), jsonString).apply();
    }

    public String getCachedAppUserID() {
        return preferences.getString(appUserIDCacheKey(), null);
    }

    public void cacheAppUserID(String appUserID) {
        preferences.edit().putString(appUserIDCacheKey(), appUserID).apply();
    }
}
