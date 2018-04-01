package com.revenuecat.purchases;

import org.json.JSONObject;

class PurchaserInfo {
    public static class Factory {
        PurchaserInfo build(JSONObject object) {
            return new PurchaserInfo();
        }
    }
}
