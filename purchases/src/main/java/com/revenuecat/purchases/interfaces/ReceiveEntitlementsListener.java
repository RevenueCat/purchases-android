//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.interfaces;


import androidx.annotation.NonNull;
import com.revenuecat.purchases.Entitlement;
import com.revenuecat.purchases.PurchasesError;

import java.util.Map;

/**
 * Interface to be implemented when making calls to fetch [Entitlement]
 */
public interface ReceiveEntitlementsListener {

    /**
     * Will be called after a successful fetch of entitlements
     *
     * @param entitlementMap Map of [Entitlement] keyed by name
     */
    void onReceived(@NonNull Map<String, Entitlement> entitlementMap);

    /**
     * Will be called after an error fetching entitlements
     *
     * @param error A [PurchasesError] containing the reason for the failure when fetching entitlements
     */
    void onError(@NonNull PurchasesError error);
}
