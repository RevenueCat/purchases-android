//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.interfaces;

import com.revenuecat.purchases.PurchaserInfo;

/**
 * Used to handle async updates from Purchases
 */
@FunctionalInterface
public interface UpdatedPurchaserInfoListener {
    /**
     * Called when a new purchaser info has been received
     * @param purchaserInfo Updated purchaser info
     */
    void onReceived(PurchaserInfo purchaserInfo);
}