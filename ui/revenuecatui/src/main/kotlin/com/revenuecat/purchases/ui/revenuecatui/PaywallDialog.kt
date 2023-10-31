package com.revenuecat.purchases.ui.revenuecatui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.window.core.layout.WindowWidthSizeClass
import com.revenuecat.purchases.ui.revenuecatui.helpers.computeWindowWidthSizeClass
import com.revenuecat.purchases.ui.revenuecatui.helpers.shouldDisplayPaywall
import kotlinx.coroutines.launch

private object UIDialogConstants {
    const val MAX_HEIGHT_PERCENTAGE_TABLET = 0.85f
    const val MAX_ASPECT_RATIO_TO_APPLY_MAX_HEIGHT = 1.5f
}

/**
 * Composable offering a dialog screen Paywall UI configured from the RevenueCat dashboard.
 * This dialog will be shown as a full screen dialog in compact devices and a normal dialog otherwise.
 * @param paywallDialogOptions The options to configure the PaywallDialog and what to do on dismissal.
 */
@ExperimentalPreviewRevenueCatUIPurchasesAPI
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

        Dialog(
            onDismissRequest = {
                dismissRequest()
                paywallDialogOptions.dismissRequest?.invoke()
            },
            properties = DialogProperties(usePlatformDefaultWidth = shouldUsePlatformDefaultWidth()),
        ) {
            DialogScaffold(paywallDialogOptions.toPaywallOptions(dismissRequest))
        }
    }
}

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
@Composable
private fun DialogScaffold(paywallOptions: PaywallOptions) {
    Scaffold(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(getDialogMaxHeightPercentage()),
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Paywall(paywallOptions)
        }
    }
}

@Composable
@ReadOnlyComposable
private fun getDialogMaxHeightPercentage(): Float {
    val aspectRatio = LocalConfiguration.current.screenHeightDp.toFloat() / LocalConfiguration.current.screenWidthDp
    if (aspectRatio < UIDialogConstants.MAX_ASPECT_RATIO_TO_APPLY_MAX_HEIGHT) {
        return 1f
    }
    return computeWindowWidthSizeClass().let {
        when (it) {
            WindowWidthSizeClass.MEDIUM, WindowWidthSizeClass.EXPANDED -> UIDialogConstants.MAX_HEIGHT_PERCENTAGE_TABLET
            else -> 1f
        }
    }
}

@Composable
@ReadOnlyComposable
private fun shouldUsePlatformDefaultWidth(): Boolean {
    return when (computeWindowWidthSizeClass()) {
        WindowWidthSizeClass.MEDIUM, WindowWidthSizeClass.EXPANDED -> true
        else -> false
    }
}
