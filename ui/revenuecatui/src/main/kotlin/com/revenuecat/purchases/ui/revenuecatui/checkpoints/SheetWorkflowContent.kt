package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import androidx.compose.runtime.Composable
import com.revenuecat.purchases.ui.revenuecatui.InternalPaywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.composables.ModalSheetPresentation
import com.revenuecat.purchases.ui.revenuecatui.composables.ModalSheetState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.getPaywallViewModel

/**
 * A checkpoint flow, a workflow or an offering's paywall, presented as a [ModalSheetPresentation]. Dismissing the
 * sheet itself, by tapping the scrim or swiping it down, closes the whole flow through the paywall's ViewModel,
 * like a close action: it is tracked as a close and the checkpoint's callback is invoked. System back is left to
 * the paywall's own back handling, so it still backs out of the flow.
 */
@Composable
internal fun SheetWorkflowContent(sheet: ModalSheetState, options: PaywallOptions) {
    SheetWorkflowContent(sheet, options, getPaywallViewModel(options))
}

@Suppress("ViewModelForwarding")
@Composable
internal fun SheetWorkflowContent(sheet: ModalSheetState, options: PaywallOptions, viewModel: PaywallViewModel) {
    ModalSheetPresentation(
        state = sheet,
        onDismissRequest = { viewModel.closePaywall() },
    ) {
        InternalPaywall(options, viewModel)
    }
}
