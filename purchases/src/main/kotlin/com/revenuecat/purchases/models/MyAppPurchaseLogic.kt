package com.revenuecat.purchases.models

class MyAppPurchaseLogic(
    val performPurchase: (() -> Unit)? = null,
    val performRestore: (() -> Unit)? = null
)