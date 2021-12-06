//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.interfaces;

import androidx.annotation.NonNull;
import com.revenuecat.purchases.CustomerInfo;

/**
 * Used to handle async updates from [Purchases]. This interface should be implemented to receive updates
 * when the [CustomerInfo] changes.
 */
public interface UpdatedCustomerInfoListener {
    /**
     * Called when a new purchaser info has been received
     * @param customerInfo Updated customer info
     */
    void onReceived(@NonNull CustomerInfo customerInfo);
}
