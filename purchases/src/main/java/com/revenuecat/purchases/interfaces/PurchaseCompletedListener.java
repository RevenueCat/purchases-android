//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.interfaces;

import com.revenuecat.purchases.PurchaserInfo;
import com.revenuecat.purchases.PurchasesError;

import androidx.annotation.NonNull;

/**
 * Interface to be implemented when making purchases.
 *  @deprecated  As of release 2.1.0, replaced by [MakePurchaseListener] to return full Purchase object in the onCompleted function.
 */
@Deprecated
public interface PurchaseCompletedListener {
    /**
     * Will be called after the purchase has completed
     * @param sku Sku for the purchased product.
     * @param purchaserInfo Updated [PurchaserInfo].
     */
    void onCompleted(@NonNull String sku, @NonNull PurchaserInfo purchaserInfo);

    /**
     * Will be called after the purchase has completed with error
     * @param error A [PurchasesError] containing the reason for the failure when making the purchase
     */
    void onError(@NonNull PurchasesError error);
}
