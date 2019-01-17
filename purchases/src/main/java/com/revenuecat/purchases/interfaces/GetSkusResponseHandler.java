//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.interfaces;

import com.android.billingclient.api.SkuDetails;

import java.util.List;

/**
 * Used when retrieving subscriptions
 */
public interface GetSkusResponseHandler {
    /**
     * Will be called after fetching subscriptions
     *
     * @param skus List of SkuDetails
     */
    void onReceiveSkus(List<SkuDetails> skus);
}