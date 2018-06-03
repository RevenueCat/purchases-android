package com.revenuecat.purchases;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Entitlement {

    private final Map<String, Offering> offerings;

    static class Factory {
        Map<String, Entitlement> build(JSONObject entitlementsResponse) {
            return new HashMap<>();
        }
    }


    Entitlement(Map<String, Offering> offeringMap)
    {
        offerings = offeringMap;
    }

    public Map<String, Offering> getOfferings()
    {
        return offerings;
    }

}
