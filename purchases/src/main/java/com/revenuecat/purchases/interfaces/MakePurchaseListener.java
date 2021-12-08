//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.interfaces;

import androidx.annotation.NonNull;

import com.android.billingclient.api.Purchase;
import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.PurchaserInfo;
import com.revenuecat.purchases.PurchasesError;

/**
 * Interface to be implemented when making purchases.
 * @deprecated
 * <p> Use {@link PurchaseCallback}, which returns a
 * {@link com.revenuecat.purchases.models.StoreTransaction} instead of a {@link Purchase}, and
 * a {@link com.revenuecat.purchases.CustomerInfo} instead of a {@link com.revenuecat.purchases.PurchaserInfo}</p>
 * <p>You can use `MakePurchaseListener.toPurchaseCallback()` in Kotlin for an easy migration.</p>
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
