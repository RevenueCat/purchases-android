//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.interfaces;

import androidx.annotation.NonNull;
import com.android.billingclient.api.SkuDetails;
import com.revenuecat.purchases.PurchasesError;

import java.util.List;

/**
 * Interface to be implemented when making calls to fetch [SkuDetails]
 */
@Deprecated
public interface GetSkusResponseListener {
    /**
     * Will be called after SkuDetails have been fetched successfully
     *
     * @param skus List of [SkuDetails] retrieved after a successful call to fetch [SkuDetails]
     */
    void onReceived(@NonNull List<SkuDetails> skus);

    /**
     * Will be called after the purchase has completed with error
     *
     * @param error A [PurchasesError] containing the reason for the failure when fetching the [SkuDetails]
     */
    void onError(@NonNull PurchasesError error);
}