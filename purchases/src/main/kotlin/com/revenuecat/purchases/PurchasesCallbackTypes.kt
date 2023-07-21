//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import com.revenuecat.purchases.models.StoreTransaction

typealias SuccessfulPurchaseCallback = (StoreTransaction, CustomerInfo) -> Unit
typealias ErrorPurchaseCallback = (StoreTransaction, PurchasesError) -> Unit
