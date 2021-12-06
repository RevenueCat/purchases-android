//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.interfaces;


import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.PurchasesError;

import androidx.annotation.NonNull;

/**
 * Interface to be implemented when making calls that return a [CustomerInfo]
 */
public interface ReceiveCustomerInfoListener {
    /**
     * Will be called after the call has completed.
     * @param customerInfo [CustomerInfo] class sent back when the call has completed
     */
    void onReceived(@NonNull CustomerInfo customerInfo);

     /**
     * Will be called after the call has completed with an error.
      * @param error A [PurchasesError] containing the reason for the failure of the call
     */
    void onError(@NonNull PurchasesError error);
}
