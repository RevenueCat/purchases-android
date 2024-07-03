package com.revenuecat.purchases.models

class MyAppPurchaseLogic(
    val performPurchase: (() -> Unit),
    val performRestore: (() -> Unit)
)