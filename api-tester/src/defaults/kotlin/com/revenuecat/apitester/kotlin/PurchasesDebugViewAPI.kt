package com.revenuecat.apitester.kotlin

import androidx.compose.runtime.Composable
import com.revenuecat.purchases.debugview.DebugRevenueCatBottomSheet
import com.revenuecat.purchases.debugview.DebugRevenueCatScreen

@Suppress("unused")
private class PurchasesDebugViewAPI {
    @Composable
    fun CheckDebugView(isVisible: Boolean, onDismissCallback: (() -> Unit)? = null) {
        DebugRevenueCatScreen()
        DebugRevenueCatBottomSheet(isVisible = isVisible, onDismissCallback = onDismissCallback)
    }
}
