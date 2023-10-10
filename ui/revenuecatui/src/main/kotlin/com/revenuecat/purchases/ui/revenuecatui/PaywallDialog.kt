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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.window.core.layout.WindowWidthSizeClass
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.computeWindowWidthSizeClass
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
    var shouldDisplayDialog by remember { mutableStateOf(shouldDisplayBlock == null) }
    if (shouldDisplayBlock != null) {
        LaunchedEffect(paywallDialogOptions) {
            launch {
                shouldDisplayDialog = try {
                    shouldDisplayBlock.invoke(Purchases.sharedInstance.awaitCustomerInfo())
                } catch (e: PurchasesException) {
                    Logger.e("Error fetching customer info to display paywall dialog", e)
                    false
                }
                if (shouldDisplayDialog) {
                    Logger.d("Displaying paywall dialog according to display logic")
                } else {
                    Logger.d("Not displaying paywall dialog according to display logic")
                }
            }
        }
    }
    if (shouldDisplayDialog) {
        Dialog(
            onDismissRequest = paywallDialogOptions.dismissRequest,
            properties = DialogProperties(usePlatformDefaultWidth = shouldUsePlatformDefaultWidth()),
        ) {
            DialogScaffold(paywallDialogOptions)
        }
    }
}

@Composable
private fun DialogScaffold(paywallDialogOptions: PaywallDialogOptions) {
    Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            PaywallView(paywallDialogOptions.toPaywallViewOptions())
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
