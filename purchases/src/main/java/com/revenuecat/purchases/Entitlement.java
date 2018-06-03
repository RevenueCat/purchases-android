package com.revenuecat.purchases;

import java.util.Map;

public class Entitlement {

    private final Map<String, Offering> offerings;

    Entitlement(Map<String, Offering> offeringMap)
    {
        offerings = offeringMap;
    }

    public Map<String, Offering> getOfferings()
    {
        return offerings;
    }

}
