package com.revenuecat.purchases.ui.revenuecatui

import android.app.Activity
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package

class MyAppPurchaseLogic(
    val performPurchase: suspend ((Activity, Package) -> Unit),
    val performRestore: suspend ((CustomerInfo) -> Unit),
)
