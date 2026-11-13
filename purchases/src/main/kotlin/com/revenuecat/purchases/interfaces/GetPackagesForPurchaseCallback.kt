package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError

/** Receives the main package and ordered add-on packages resolved from one offerings snapshot. */
@InternalRevenueCatAPI
public interface GetPackagesForPurchaseCallback {
    public fun onReceived(packageToPurchase: Package, addOnPackages: List<Package>)
    public fun onError(error: PurchasesError)
}
