package com.revenuecat.purchases.debugview

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InternalDebugRevenueCatBottomSheet(
    isVisible: Boolean = false,
    onDismissCallback: (() -> Unit)? = null,
) {
    if (isVisible) {
        val rcDebugMenuSheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )

        val scope = rememberCoroutineScope()

        ModalBottomSheet(
            sheetState = rcDebugMenuSheetState,
            onDismissRequest = {
                scope.launch {
                    rcDebugMenuSheetState.hide()
                    onDismissCallback?.invoke()
                }
            },
        ) {
            InternalDebugRevenueCatScreen()
        }
    }
}
