//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.interfaces;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.Purchase;
import com.revenuecat.purchases.CustomerInfo;

/**
 * Interface to be implemented when upgrading or downgrading a purchase.
 */
public interface ProductChangeListener extends PurchaseErrorListener {
    /**
     * Will be called after the product change has been completed
     * @param purchase Purchase object for the purchased product. Will be null if the change is deferred.
     * @param customerInfo Updated [CustomerInfo].
     */
    void onCompleted(@Nullable Purchase purchase, @NonNull CustomerInfo customerInfo);

}
