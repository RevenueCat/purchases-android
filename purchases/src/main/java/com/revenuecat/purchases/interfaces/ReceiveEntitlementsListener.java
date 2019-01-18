//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.interfaces;


import com.revenuecat.purchases.Entitlement;
import com.revenuecat.purchases.PurchasesError;

import java.util.Map;

import androidx.annotation.Nullable;

/**
 * Used when retrieving entitlements
 */
public interface ReceiveEntitlementsListener {

    /**
     * Will be called after a successful fetch of entitlements
     * @param entitlementMap Map of entitlements keyed by name. Null if an error has occurred.
     * @param error Not null if there has been an error when retrieving entitlements
     */
    void onReceived(@Nullable Map<String, Entitlement> entitlementMap, @Nullable PurchasesError error);

}
