package com.revenuecat.purchases.ui.revenuecatui

import androidx.compose.runtime.Composable

class MyAppPurchaseLogic(
    val performPurchase: (() -> Unit),
    val performRestore: (() -> Unit)
)