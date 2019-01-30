//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.interfaces;

import androidx.annotation.NonNull;
import com.android.billingclient.api.SkuDetails;

import java.util.List;

/**
 * Interface to be implemented when making calls to fetch [SkuDetails]
 */
@FunctionalInterface
public interface GetSkusResponseListener {
    /**
     * Will be called after SkuDetails have been fetched successfully
     *
     * @param skus List of [SkuDetails] retrieved after a successful call to fetch [SkuDetails]
     */
    void onReceiveSkus(@NonNull List<SkuDetails> skus);
}