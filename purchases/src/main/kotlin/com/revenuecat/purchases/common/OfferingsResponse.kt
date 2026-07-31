//  Purchases
//
//  Copyright © 2026 RevenueCat, Inc. All rights reserved.

package com.revenuecat.purchases.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import org.json.JSONObject

@OptIn(InternalRevenueCatAPI::class)
internal data class OfferingsResponse(
    val body: JSONObject,
    val bodyString: String,
    val originalDataSource: HTTPResponseOriginalSource,
)
