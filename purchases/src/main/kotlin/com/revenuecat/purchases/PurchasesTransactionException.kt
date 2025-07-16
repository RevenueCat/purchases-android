package com.revenuecat.purchases

public class PurchasesTransactionException(
    purchasesError: PurchasesError,
    public val userCancelled: Boolean,
) : PurchasesException(purchasesError)
