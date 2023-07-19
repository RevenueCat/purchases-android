package com.revenuecat.purchases

class PurchasesTransactionException(
    purchasesError: PurchasesError,
    val userCancelled: Boolean,
) : PurchasesException(purchasesError)
