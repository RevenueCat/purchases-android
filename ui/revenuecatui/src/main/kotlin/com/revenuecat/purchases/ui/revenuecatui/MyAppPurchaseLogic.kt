package com.revenuecat.purchases.ui.revenuecatui

import android.app.Activity
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError

sealed class MyAppPurchaseResult {
    object Success : MyAppPurchaseResult()
    object Cancellation : MyAppPurchaseResult()
    data class Error(val error: PurchasesError) : MyAppPurchaseResult()
}

class MyAppPurchaseLogic(
    val performPurchase: suspend ((Activity, Package) -> MyAppPurchaseResult),
    val performRestore: suspend ((CustomerInfo) -> MyAppPurchaseResult),
)
