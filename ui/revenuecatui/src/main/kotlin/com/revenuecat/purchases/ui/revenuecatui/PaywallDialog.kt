package com.revenuecat.purchases.ui.revenuecatui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResult
import com.revenuecat.purchases.ui.revenuecatui.composables.RevenueCatDialogScaffold
import com.revenuecat.purchases.ui.revenuecatui.helpers.shouldDisplayPaywall
import kotlinx.coroutines.launch

/**
 * Composable offering a dialog screen Paywall UI configured from the RevenueCat dashboard.
 * This dialog will be shown as a full screen dialog in compact devices and a normal dialog otherwise.
 * @param paywallDialogOptions The options to configure the PaywallDialog and what to do on dismissal.
 */
@Composable
public fun PaywallDialog(
    paywallDialogOptions: PaywallDialogOptions,
) {
    val shouldDisplayBlock = paywallDialogOptions.shouldDisplayBlock
    var shouldDisplayDialog by rememberSaveable { mutableStateOf(shouldDisplayBlock == null) }
    if (shouldDisplayBlock != null) {
        LaunchedEffect(paywallDialogOptions) {
            launch {
                shouldDisplayDialog = shouldDisplayPaywall(shouldDisplayBlock)
            }
        }
    }

    var currentDialogOffering by remember {
        mutableStateOf<OfferingSelection?>(
            if (shouldDisplayDialog) paywallDialogOptions.offeringSelection else null,
        )
    }
    var pendingExitOffering by remember { mutableStateOf<OfferingSelection?>(null) }

    LaunchedEffect(shouldDisplayDialog) {
        if (shouldDisplayDialog && currentDialogOffering == null) {
            currentDialogOffering = paywallDialogOptions.offeringSelection
        }
    }

    val dismissDialog: () -> Unit = {
        currentDialogOffering = null
        shouldDisplayDialog = false
        paywallDialogOptions.dismissRequest?.invoke()
    }

    // When current dialog is dismissed and there's a pending exit offering, check shouldDisplayBlock before showing
    LaunchedEffect(currentDialogOffering, pendingExitOffering) {
        if (currentDialogOffering == null && pendingExitOffering != null) {
            if (shouldDisplayBlock != null) {
                val shouldShow = shouldDisplayPaywall(shouldDisplayBlock)
                if (shouldShow) {
                    currentDialogOffering = pendingExitOffering
                } else {
                    dismissDialog()
                }
                pendingExitOffering = null
            } else {
                currentDialogOffering = pendingExitOffering
                pendingExitOffering = null
            }
        }
    }

    currentDialogOffering?.let { offeringSelection ->
        PaywallDialogContent(
            paywallDialogOptions = paywallDialogOptions,
            offeringSelection = offeringSelection,
            onDismissRequest = { exitOffering ->
                if (exitOffering != null) {
                    pendingExitOffering = exitOffering
                    currentDialogOffering = null
                } else {
                    dismissDialog()
                }
            },
        )
    }
}

@Composable
private fun PaywallDialogContent(
    paywallDialogOptions: PaywallDialogOptions,
    offeringSelection: OfferingSelection,
    onDismissRequest: (OfferingSelection?) -> Unit,
) {
    val paywallOptions = remember(paywallDialogOptions, offeringSelection) {
        buildPaywallOptions(
            paywallDialogOptions = paywallDialogOptions,
            offeringSelection = offeringSelection,
            dismissRequest = { onDismissRequest(null) },
            dismissRequestWithExitOffering = { exitOffering, _ ->
                onDismissRequest(exitOffering?.let { OfferingSelection.OfferingType(it) })
            },
        )
    }

    val viewModel = getPaywallViewModel(
        options = paywallOptions,
        shouldDisplayBlock = paywallDialogOptions.shouldDisplayBlock,
    )

    LaunchedEffect(Unit) {
        viewModel.preloadExitOffering()
    }

    RevenueCatDialogScaffold(
        handleCloseRequest = viewModel::closePaywall,
    ) {
        InternalPaywall(paywallOptions, viewModel)
    }
}

private fun buildPaywallOptions(
    paywallDialogOptions: PaywallDialogOptions,
    offeringSelection: OfferingSelection,
    dismissRequest: () -> Unit,
    dismissRequestWithExitOffering: ((Offering?, PaywallResult?) -> Unit)? = null,
): PaywallOptions {
    return PaywallOptions.Builder(dismissRequest = dismissRequest)
        .setOfferingSelection(offeringSelection)
        .setShouldDisplayDismissButton(paywallDialogOptions.shouldDisplayDismissButton)
        .setFontProvider(paywallDialogOptions.fontProvider)
        .setListener(paywallDialogOptions.listener)
        .setPurchaseLogic(paywallDialogOptions.purchaseLogic)
        .setDismissRequestWithExitOffering(dismissRequestWithExitOffering)
        .setCustomVariables(paywallDialogOptions.customVariables)
        .build()
}
