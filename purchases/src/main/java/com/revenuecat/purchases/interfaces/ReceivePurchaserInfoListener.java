//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.interfaces;

import android.support.annotation.Nullable;

import com.revenuecat.purchases.PurchaserInfo;
import com.revenuecat.purchases.PurchasesError;

/**
 * Used by calls that send back a purchaser info.
 */
public interface ReceivePurchaserInfoListener {
    /**
     * Will be called after the call has completed.
     * @param purchaserInfo Purchaser info. Null if an error has occurred.
     * @param error Not null if there has been an error.
     */
    void onReceived(@Nullable PurchaserInfo purchaserInfo, @Nullable PurchasesError error);
}
