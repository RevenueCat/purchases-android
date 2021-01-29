//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.interfaces;

import androidx.annotation.NonNull;

import com.android.billingclient.api.Purchase;
import com.revenuecat.purchases.PurchaserInfo;

/**
 * Interface to be implemented when making purchases.
 */
@Deprecated
public interface MakePurchaseListener extends PurchaseErrorListener {
    /**
     * Will be called after the purchase has completed
     * @param purchase Purchase object for the purchased product.
     * @param purchaserInfo Updated [PurchaserInfo].
     */
    void onCompleted(@NonNull Purchase purchase, @NonNull PurchaserInfo purchaserInfo);

}
