package com.revenuecat.purchases.debugview

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugRevenueCatBottomSheet(
    sheetState: SheetState,
) {
    InternalDebugRevenueCatBottomSheet(sheetState)
}
