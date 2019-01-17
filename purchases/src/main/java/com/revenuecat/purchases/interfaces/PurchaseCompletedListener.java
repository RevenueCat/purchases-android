//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.interfaces;

import android.support.annotation.Nullable;

import com.revenuecat.purchases.PurchaserInfo;
import com.revenuecat.purchases.PurchasesError;

/**
 * Used when making a purchase
 */
public interface PurchaseCompletedListener {
    /**
     * Will be called after the purchase has completed
     * @param sku Sku for the purchased product. Null if an error has occurred.
     * @param purchaserInfo Updated purchaser info. Null if an error has occurred.
     * @param error Not null if there has been an error when making the purchase
     */
    void onCompleted(@Nullable String sku, @Nullable PurchaserInfo purchaserInfo, @Nullable PurchasesError error);
}
