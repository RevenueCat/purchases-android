package com.revenuecat.purchases.debugview

import androidx.compose.runtime.Composable

@Composable
fun DebugRevenueCatBottomSheet(
    isVisible: Boolean = false,
    onDismissCallback: (() -> Unit)? = null,
) {
    InternalDebugRevenueCatBottomSheet(
        isVisible,
        onDismissCallback,
    )
}
