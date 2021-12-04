//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.interfaces;

import androidx.annotation.NonNull;

import com.android.billingclient.api.Purchase;
import com.revenuecat.purchases.CustomerInfo;

/**
 * Interface to be implemented when making purchases.
 */
public interface MakePurchaseListener extends PurchaseErrorListener {
    /**
     * Will be called after the purchase has completed
     * @param purchase Purchase object for the purchased product.
     * @param customerInfo Updated [CustomerInfo].
     */
    void onCompleted(@NonNull Purchase purchase, @NonNull CustomerInfo customerInfo);

}
