package com.revenuecat.purchases.ui.revenuecatui.customercenter

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI

/**
 * Composable offering a bottom sheet Customer Center UI configured from the RevenueCat dashboard.
 */
@ExperimentalPreviewRevenueCatUIPurchasesAPI
@ExperimentalMaterial3Api
@Composable
// CustomerCenter WIP: Make public when ready
internal fun CustomerCenterBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
) {
    CustomerCenterBottomSheetScaffold(
        onDismissRequest,
        sheetState,
    ) {
        InternalCustomerCenter()
    }
}

private const val CUSTOMER_CENTER_BOTTOM_SHEET_MAX_HEIGHT_PERCENTAGE = 0.9f

@ExperimentalMaterial3Api
@Composable
private fun CustomerCenterBottomSheetScaffold(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxHeight(CUSTOMER_CENTER_BOTTOM_SHEET_MAX_HEIGHT_PERCENTAGE),
        sheetState = sheetState,
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
internal fun CustomerCenterBottomSheetPreview() {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    CustomerCenterBottomSheetScaffold(
        onDismissRequest = {},
        sheetState = sheetState,
    ) {
        InternalCustomerCenter(getCustomerCenterViewModel(previewPurchases))
    }
}
