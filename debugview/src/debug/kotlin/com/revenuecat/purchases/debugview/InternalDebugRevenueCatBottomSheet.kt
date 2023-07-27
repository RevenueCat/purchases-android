package com.revenuecat.purchases.debugview

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InternalDebugRevenueCatBottomSheet(sheetState: SheetState) {
    if (sheetState.isVisible) {
        val scope = rememberCoroutineScope()

        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { scope.launch { sheetState.hide() } },
        ) {
            InternalDebugRevenueCatScreen()
        }
    }
}
