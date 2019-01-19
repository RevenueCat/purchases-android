//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.interfaces;


import com.revenuecat.purchases.PurchaserInfo;
import com.revenuecat.purchases.PurchasesError;

import androidx.annotation.NonNull;

/**
 * Used by calls that send back a purchaser info.
 */
public interface ReceivePurchaserInfoListener {
    /**
     * Will be called after the call has completed.
     * @param purchaserInfo Purchaser info. Null if an error has occurred.
     */
    void onReceived(@NonNull PurchaserInfo purchaserInfo);

     /**
     * Will be called after the call has completed with an error.
     * @param error Not null if there has been an error.
     */
    void onError(@NonNull PurchasesError error);
}
