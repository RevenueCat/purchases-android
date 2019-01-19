//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.interfaces;


import com.revenuecat.purchases.Entitlement;
import com.revenuecat.purchases.PurchasesError;

import java.util.Map;

import androidx.annotation.NonNull;

/**
 * Used when retrieving entitlements
 */
@FunctionalInterface
public interface ReceiveEntitlementsListener {

    /**
     * Will be called after a successful fetch of entitlements
     * @param entitlementMap Map of entitlements keyed by name. Null if an error has occurred.
     */
    void onReceived(@NonNull Map<String, Entitlement> entitlementMap);

    /**
     * Will be called after an error fetching entitlements
     * @param error Not null if there has been an error when retrieving entitlements
     */
    void onError(@NonNull PurchasesError error);
}
