//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.interfaces;

import com.android.billingclient.api.SkuDetails;

import java.util.List;

import androidx.annotation.NonNull;

/**
 * Used when retrieving SkuDetails
 */
@FunctionalInterface
public interface GetSkusResponseListener {
    /**
     * Will be called after fetching SkuDetails
     *
     * @param skus List of SkuDetails
     */
    void onReceiveSkus(@NonNull List<SkuDetails> skus);
}