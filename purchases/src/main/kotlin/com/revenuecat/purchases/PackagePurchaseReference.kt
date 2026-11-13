package com.revenuecat.purchases

import dev.drewhamilton.poko.Poko

/** Identifies a package to resolve for a hybrid purchase. */
@InternalRevenueCatAPI
@Poko
public class PackagePurchaseReference(
    public val offeringIdentifier: String,
    public val packageIdentifier: String,
)
