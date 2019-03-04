//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.interfaces;

import androidx.annotation.NonNull;
import com.android.billingclient.api.Purchase;
import com.revenuecat.purchases.PurchaserInfo;
import com.revenuecat.purchases.PurchasesError;

/**
 * Interface to be implemented when making purchases.
 */
public interface MakePurchaseListener {
    /**
     * Will be called after the purchase has completed
     * @param purchase Purchase object for the purchased product.
     * @param purchaserInfo Updated [PurchaserInfo].
     */
    void onCompleted(@NonNull Purchase purchase, @NonNull PurchaserInfo purchaserInfo);

    /**
     * Will be called after the purchase has completed with error
     * @param error A [PurchasesError] containing the reason for the failure when making the purchase
     */
    void onError(@NonNull PurchasesError error);
}
