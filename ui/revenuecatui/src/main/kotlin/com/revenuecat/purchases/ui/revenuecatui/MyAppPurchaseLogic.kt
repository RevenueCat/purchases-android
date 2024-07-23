package com.revenuecat.purchases.ui.revenuecatui
import android.app.Activity
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import androidx.compose.runtime.Composable

class MyAppPurchaseLogic(
    val performPurchase: ((Activity, Package) -> Unit),
    val performRestore: ((CustomerInfo) -> Unit)
)