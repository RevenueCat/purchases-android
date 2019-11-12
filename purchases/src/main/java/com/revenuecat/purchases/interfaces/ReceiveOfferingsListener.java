//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.interfaces;


import androidx.annotation.NonNull;

import com.revenuecat.purchases.Offerings;
import com.revenuecat.purchases.PurchasesError;

/**
 * Interface to be implemented when making calls to fetch [Offering].
 */
public interface ReceiveOfferingsListener {

    /**
     * Will be called after a successful fetch of offerings.
     *
     * @param offerings
     */
    void onReceived(@NonNull Offerings offerings);

    /**
     * Will be called after an error fetching offerings
     *
     * @param error A [PurchasesError] containing the reason for the failure when fetching offerings.
     */
    void onError(@NonNull PurchasesError error);
}
