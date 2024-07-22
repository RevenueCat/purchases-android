package com.revenuecat.purchases.ui.revenuecatui
import com.revenuecat.purchases.CustomerInfo
import androidx.compose.runtime.Composable

class MyAppPurchaseLogic(
    val performPurchase: ((CustomerInfo) -> Unit),
    val performRestore: ((CustomerInfo) -> Unit)
)