package com.revenuecat.purchases.ui.revenuecatui

import android.os.Build
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
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

    // When current dialog is dismissed and there's a pending exit offering, show it
    LaunchedEffect(currentDialogOffering, pendingExitOffering) {
        if (currentDialogOffering == null && pendingExitOffering != null) {
            currentDialogOffering = pendingExitOffering
            pendingExitOffering = null
        }
    }

    currentDialogOffering?.let { offeringSelection ->
        PaywallDialogContent(
            paywallDialogOptions = paywallDialogOptions,
            offeringSelection = offeringSelection,
            onDismissWithExitOffer = { exitOffering ->
                pendingExitOffering = exitOffering
                currentDialogOffering = null
            },
            onDismiss = {
                currentDialogOffering = null
                shouldDisplayDialog = false
                // Now invoke the user's dismiss request since we're truly dismissing
                paywallDialogOptions.dismissRequest?.invoke()
            },
        )
    }
}

private sealed class ExitOfferState {
    object Loading : ExitOfferState()
    data class Loaded(val exitOffering: OfferingSelection.OfferingType?) : ExitOfferState()
}

@Composable
private fun PaywallDialogContent(
    paywallDialogOptions: PaywallDialogOptions,
    offeringSelection: OfferingSelection,
    onDismissWithExitOffer: (OfferingSelection) -> Unit,
    onDismiss: () -> Unit,
) {
    // Use rememberUpdatedState to safely reference lambdas in LaunchedEffect
    val currentOnDismissWithExitOffer by rememberUpdatedState(onDismissWithExitOffer)
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    var purchaseCompleted by remember { mutableStateOf(false) }
    var exitOfferState by remember { mutableStateOf<ExitOfferState>(ExitOfferState.Loading) }
    var pendingDismiss by remember { mutableStateOf(false) }

    PreloadExitOffering(offeringSelection) { state -> exitOfferState = state }

    HandlePendingDismiss(
        exitOfferState = exitOfferState,
        pendingDismiss = pendingDismiss,
        onHandle = { pendingDismiss = false },
        onDismissWithExitOffer = currentOnDismissWithExitOffer,
        onDismiss = currentOnDismiss,
    )

    val handleCloseRequest = createCloseRequestHandler(
        purchaseCompleted = purchaseCompleted,
        exitOfferState = exitOfferState,
        onPendingDismiss = { pendingDismiss = true },
        onDismissWithExitOffer = onDismissWithExitOffer,
        onDismiss = onDismiss,
    )

    val paywallOptions = buildPaywallOptions(
        paywallDialogOptions = paywallDialogOptions,
        offeringSelection = offeringSelection,
        handleCloseRequest = handleCloseRequest,
        onPurchaseCompleted = { purchaseCompleted = true },
    )

    PaywallDialogScaffold(handleCloseRequest, paywallOptions)
}

@Composable
private fun PreloadExitOffering(
    offeringSelection: OfferingSelection,
    onStateChange: (ExitOfferState) -> Unit,
) {
    val currentOnStateChange by rememberUpdatedState(onStateChange)
    LaunchedEffect(offeringSelection) {
        currentOnStateChange(ExitOfferState.Loading)
        try {
            val offerings = Purchases.sharedInstance.awaitOfferings()
            val offeringId = offeringSelection.offeringIdentifier
            val currentOffering = if (offeringId != null) offerings[offeringId] else offerings.current
            val exitOfferId = currentOffering?.paywallComponents?.data?.exitOffers?.dismiss?.offeringId
            val exitOffering = if (exitOfferId != null && exitOfferId.isNotBlank()) {
                offerings[exitOfferId]?.let { OfferingSelection.OfferingType(it) }
            } else {
                null
            }
            currentOnStateChange(ExitOfferState.Loaded(exitOffering))
        } catch (e: PurchasesException) {
            Logger.e("Failed to preload exit offering", e)
            currentOnStateChange(ExitOfferState.Loaded(null))
        }
    }
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
            // the decorFitsSystemWindows setting of the Dialog. This is added to mimick the dim effect that we get
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

@Composable
private fun HandlePendingDismiss(
    exitOfferState: ExitOfferState,
    pendingDismiss: Boolean,
    onHandle: () -> Unit,
    onDismissWithExitOffer: (OfferingSelection) -> Unit,
    onDismiss: () -> Unit,
) {
    val currentOnHandle by rememberUpdatedState(onHandle)
    val currentOnDismissWithExitOffer by rememberUpdatedState(onDismissWithExitOffer)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(exitOfferState, pendingDismiss) {
        if (pendingDismiss && exitOfferState is ExitOfferState.Loaded) {
            currentOnHandle()
            val exitOffering = exitOfferState.exitOffering
            if (exitOffering != null) {
                currentOnDismissWithExitOffer(exitOffering)
            } else {
                currentOnDismiss()
            }
        }
    }
}

private fun createCloseRequestHandler(
    purchaseCompleted: Boolean,
    exitOfferState: ExitOfferState,
    onPendingDismiss: () -> Unit,
    onDismissWithExitOffer: (OfferingSelection) -> Unit,
    onDismiss: () -> Unit,
): () -> Unit = {
    if (purchaseCompleted) {
        onDismiss()
    } else {
        when (exitOfferState) {
            is ExitOfferState.Loading -> onPendingDismiss()
            is ExitOfferState.Loaded -> {
                val exitOffering = exitOfferState.exitOffering
                if (exitOffering != null) {
                    onDismissWithExitOffer(exitOffering)
                } else {
                    onDismiss()
                }
            }
        }
    }
}

private fun buildPaywallOptions(
    paywallDialogOptions: PaywallDialogOptions,
    offeringSelection: OfferingSelection,
    handleCloseRequest: () -> Unit,
    onPurchaseCompleted: () -> Unit,
): PaywallOptions {
    return PaywallOptions.Builder(dismissRequest = handleCloseRequest)
        .setOffering(paywallDialogOptions.offering)
        .setShouldDisplayDismissButton(paywallDialogOptions.shouldDisplayDismissButton)
        .setFontProvider(paywallDialogOptions.fontProvider)
        .setPurchaseLogic(paywallDialogOptions.purchaseLogic)
        .build()
        .copy(
            offeringSelection = offeringSelection,
            listener = createPaywallListener(paywallDialogOptions, onPurchaseCompleted),
        )
}

private fun createPaywallListener(
    paywallDialogOptions: PaywallDialogOptions,
    onPurchaseCompleted: () -> Unit,
): PaywallListener = object : PaywallListener {
    override fun onPurchaseStarted(rcPackage: com.revenuecat.purchases.Package) {
        paywallDialogOptions.listener?.onPurchaseStarted(rcPackage)
    }

    override fun onPurchaseCompleted(
        customerInfo: com.revenuecat.purchases.CustomerInfo,
        storeTransaction: com.revenuecat.purchases.models.StoreTransaction,
    ) {
        onPurchaseCompleted()
        paywallDialogOptions.listener?.onPurchaseCompleted(customerInfo, storeTransaction)
    }

    override fun onPurchaseError(error: com.revenuecat.purchases.PurchasesError) {
        paywallDialogOptions.listener?.onPurchaseError(error)
    }

    override fun onPurchaseCancelled() {
        paywallDialogOptions.listener?.onPurchaseCancelled()
    }

    override fun onRestoreStarted() {
        paywallDialogOptions.listener?.onRestoreStarted()
    }

    override fun onRestoreCompleted(customerInfo: com.revenuecat.purchases.CustomerInfo) {
        paywallDialogOptions.listener?.onRestoreCompleted(customerInfo)
    }

    override fun onRestoreError(error: com.revenuecat.purchases.PurchasesError) {
        paywallDialogOptions.listener?.onRestoreError(error)
    }
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
