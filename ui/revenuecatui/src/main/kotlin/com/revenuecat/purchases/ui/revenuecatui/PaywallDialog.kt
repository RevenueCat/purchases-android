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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
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
    if (shouldDisplayDialog) {
        val dismissRequest = {
            shouldDisplayDialog = false
        }
        val paywallOptions = paywallDialogOptions.toPaywallOptions(dismissRequest)

        val viewModel = getPaywallViewModel(
            options = paywallOptions,
            shouldDisplayBlock = paywallDialogOptions.shouldDisplayBlock,
        )

        // This is needed because of this issue: https://issuetracker.google.com/issues/246909281.
        // This is fixed in a newer version of Compose, but to avoid a breaking change,
        // we are applying a workaround for now.
        // This should be removed once we update Compose in the next major.
        val dialogBottomPadding = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        } else {
            0.dp
        }

        Dialog(
            onDismissRequest = {
                dismissRequest()
                viewModel.closePaywall()
                paywallDialogOptions.dismissRequest?.invoke()
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = shouldUsePlatformDefaultWidth(),
                decorFitsSystemWindows = Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
            ),
        ) {
            DialogScaffold(paywallOptions, dialogBottomPadding)
        }
    }
}

@Composable
private fun DialogScaffold(paywallOptions: PaywallOptions, dialogBottomPadding: Dp) {
    Scaffold(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(getDialogMaxHeightPercentage()),
        containerColor = Color.Black.copy(alpha = 0.4f),
    ) { paddingValues ->
        val shouldApplyDialogBottomPadding = paddingValues.calculateBottomPadding() == 0.dp &&
            paddingValues.calculateTopPadding() == 0.dp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .conditional(Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { padding(paddingValues) }
                .padding(bottom = if (shouldApplyDialogBottomPadding) dialogBottomPadding else 0.dp),
        ) {
            Paywall(paywallOptions)
        }
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
