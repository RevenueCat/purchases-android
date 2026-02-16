//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import com.revenuecat.purchases.models.StoreTransaction

public typealias SuccessfulPurchaseCallback = (StoreTransaction, CustomerInfo) -> Unit
public typealias ErrorPurchaseCallback = (StoreTransaction, PurchasesError) -> Unit
