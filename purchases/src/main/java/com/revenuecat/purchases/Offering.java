package com.revenuecat.purchases;

import com.android.billingclient.api.SkuDetails;

public class Offering {
    private final String activeProductIdentifier;
    private SkuDetails skuDetails;

    Offering(final String activeProductIdentifier) {
        this.activeProductIdentifier = activeProductIdentifier;
    }

    String getActiveProductIdentifier() {
        return activeProductIdentifier;
    }

    void setSkuDetails(SkuDetails details) {
        this.skuDetails = details;
    }

    public SkuDetails getSkuDetails() {
        return skuDetails;
    }
}
