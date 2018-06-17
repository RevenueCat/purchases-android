package com.revenuecat.purchases;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Entitlement {

    private final Map<String, Offering> offerings;

    static class Factory {
        private Offering parseOffering(JSONObject offering) throws JSONException {
            String activeProductIdentifier = offering.getString("active_product_identifier");
            return new Offering(activeProductIdentifier);
        }

        private Entitlement parseEntitlement(JSONObject entitlement) throws JSONException {
            Map<String, Offering> offeringMap = new HashMap<>();
            JSONObject offerings = entitlement.getJSONObject("offerings");
            for (Iterator<String> it = offerings.keys(); it.hasNext(); ) {
                String offeringID = it.next();
                try {
                    JSONObject offering = offerings.getJSONObject(offeringID);
                    Offering o = parseOffering(offering);
                    offeringMap.put(offeringID, o);
                } catch (JSONException ignored) {}
            }
            return new Entitlement(offeringMap);
        }

        Map<String, Entitlement> build(JSONObject entitlementsResponse) {
            Map<String, Entitlement> returnMap = new HashMap<>();
            for (Iterator<String> it = entitlementsResponse.keys(); it.hasNext(); ) {
                String entID = it.next();
                try {
                    JSONObject entitlement = entitlementsResponse.getJSONObject(entID);
                    Entitlement e = parseEntitlement(entitlement);
                    returnMap.put(entID, e);
                } catch (JSONException ignored) {}
            }
            return returnMap;
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
