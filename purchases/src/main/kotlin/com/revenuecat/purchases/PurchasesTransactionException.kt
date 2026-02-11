package com.revenuecat.purchases

public class PurchasesTransactionException(
    purchasesError: PurchasesError,
    val userCancelled: Boolean,
) : PurchasesException(purchasesError)
