package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import androidx.compose.runtime.Composable
import com.revenuecat.purchases.ui.revenuecatui.InternalPaywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallDismissReason
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.composables.ModalSheetPresentation
import com.revenuecat.purchases.ui.revenuecatui.composables.ModalSheetState
import com.revenuecat.purchases.ui.revenuecatui.getPaywallViewModel

/**
 * A checkpoint flow, a workflow or an offering's paywall, presented as a [ModalSheetPresentation]. Dismissing the
 * sheet itself, by tapping the scrim or swiping it down, closes the whole flow as a back navigation through the
 * paywall's ViewModel, so it is tracked and reported exactly like system back from the first step; the paywall's
 * own back handling is untouched.
 */
@Composable
internal fun SheetWorkflowContent(sheet: ModalSheetState, options: PaywallOptions) {
    val viewModel = getPaywallViewModel(options)
    ModalSheetPresentation(
        state = sheet,
        onDismissRequest = { viewModel.closePaywall(reason = PaywallDismissReason.NAVIGATED_BACK) },
    ) {
        InternalPaywall(options, viewModel)
    }
}
