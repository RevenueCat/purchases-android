package com.revenuecat.purchases.ui.revenuecatui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.window.core.layout.WindowWidthSizeClass
import com.revenuecat.purchases.ui.revenuecatui.helpers.computeWindowWidthSizeClass
import com.revenuecat.purchases.ui.revenuecatui.helpers.shouldDisplayPaywall
import kotlinx.coroutines.launch

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
        val dismissRequest = { shouldDisplayDialog = false }

        Dialog(
            onDismissRequest = dismissRequest,
            properties = DialogProperties(usePlatformDefaultWidth = shouldUsePlatformDefaultWidth()),
        ) {
            DialogScaffold(paywallDialogOptions.toPaywallOptions(dismissRequest))
        }
    }
}

@Composable
private fun DialogScaffold(paywallOptions: PaywallOptions) {
    Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
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
private fun shouldUsePlatformDefaultWidth(): Boolean {
    return when (computeWindowWidthSizeClass()) {
        WindowWidthSizeClass.MEDIUM, WindowWidthSizeClass.EXPANDED -> true
        else -> false
    }
}
