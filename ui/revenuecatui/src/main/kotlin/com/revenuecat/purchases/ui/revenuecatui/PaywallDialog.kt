package com.revenuecat.purchases.ui.revenuecatui

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional
import com.revenuecat.purchases.ui.revenuecatui.helpers.hasCompactDimension
import com.revenuecat.purchases.ui.revenuecatui.helpers.shouldDisplayPaywall
import com.revenuecat.purchases.ui.revenuecatui.helpers.windowAspectRatio
import kotlinx.coroutines.launch

private object UIDialogConstants {
    const val MAX_HEIGHT_PERCENTAGE_TABLET = 0.85f
    const val MAX_ASPECT_RATIO_TO_APPLY_MAX_HEIGHT = 1.25f
}

/**
 * Composable offering a dialog screen Paywall UI configured from the RevenueCat dashboard.
 * This dialog will be shown as a full screen dialog in compact devices and a normal dialog otherwise.
 * @param paywallDialogOptions The options to configure the PaywallDialog and what to do on dismissal.
 */
@Composable
fun PaywallDialog(
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
            dismissRequest = {},
        )
    }

    val viewModel = getPaywallViewModel(
        options = paywallOptions,
        shouldDisplayBlock = paywallDialogOptions.shouldDisplayBlock,
    )

    LaunchedEffect(Unit) {
        viewModel.preloadExitOffering()
    }

    val purchaseCompleted by viewModel.purchaseCompleted
    val preloadedExitOffering by viewModel.preloadedExitOffering

    val handleCloseRequest: () -> Unit = {
        val exitOffering = if (!purchaseCompleted && preloadedExitOffering != null) {
            OfferingSelection.OfferingType(preloadedExitOffering!!)
        } else {
            null
        }
        onDismissRequest(exitOffering)
    }

    val paywallOptionsWithDismiss = paywallOptions.copy(dismissRequest = handleCloseRequest)

    PaywallDialogScaffold(handleCloseRequest, paywallOptionsWithDismiss)
}

@Composable
private fun PaywallDialogScaffold(
    handleCloseRequest: () -> Unit,
    paywallOptions: PaywallOptions,
) {
    val dialogBottomPadding = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
            WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    } else {
        0.dp
    }

    BackHandler(onBack = handleCloseRequest)

    Dialog(
        onDismissRequest = handleCloseRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = shouldUsePlatformDefaultWidth(),
            decorFitsSystemWindows = Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        ),
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(getDialogMaxHeightPercentage()),
            // This is needed for Android 35+ but using an older version of Compose. In those cases,
            // the dialog doesn't properly extend edge to edge, leaving some spacing at the bottom since we changed
            // the decorFitsSystemWindows setting of the Dialog. This is added to mimic the dim effect that we get
            // at the top of the dialog in this case. This should be removed once we update Compose in the next major.
            containerColor = Color.Black.copy(alpha = 0.4f),
        ) { paddingValues ->
            val shouldApplyDialogBottomPadding = paddingValues.calculateBottomPadding() == 0.dp &&
                paddingValues.calculateTopPadding() == 0.dp
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .conditional(
                        Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
                    ) { padding(paddingValues) }
                    .padding(bottom = if (shouldApplyDialogBottomPadding) dialogBottomPadding else 0.dp),
            ) {
                Paywall(paywallOptions)
            }
        }
    }
}

private fun buildPaywallOptions(
    paywallDialogOptions: PaywallDialogOptions,
    offeringSelection: OfferingSelection,
    dismissRequest: () -> Unit,
): PaywallOptions {
    return PaywallOptions.Builder(dismissRequest = dismissRequest)
        .setOfferingSelection(offeringSelection)
        .setShouldDisplayDismissButton(paywallDialogOptions.shouldDisplayDismissButton)
        .setFontProvider(paywallDialogOptions.fontProvider)
        .setListener(paywallDialogOptions.listener)
        .setPurchaseLogic(paywallDialogOptions.purchaseLogic)
        .setCustomVariables(paywallDialogOptions.customVariables)
        .build()
}

@Composable
@ReadOnlyComposable
private fun getDialogMaxHeightPercentage(): Float {
    if (windowAspectRatio() < UIDialogConstants.MAX_ASPECT_RATIO_TO_APPLY_MAX_HEIGHT) {
        return 1f
    }
    return if (hasCompactDimension()) 1f else UIDialogConstants.MAX_HEIGHT_PERCENTAGE_TABLET
}

@Composable
@ReadOnlyComposable
private fun shouldUsePlatformDefaultWidth(): Boolean {
    return !hasCompactDimension()
}
