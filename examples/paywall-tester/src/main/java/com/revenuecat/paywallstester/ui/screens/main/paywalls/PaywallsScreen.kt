package com.revenuecat.paywallstester.ui.screens.main.paywalls

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.revenuecat.paywallstester.SamplePaywalls
import com.revenuecat.paywallstester.SamplePaywallsLoader
import com.revenuecat.paywallstester.ui.screens.paywallfooter.SamplePaywall
import com.revenuecat.paywallstester.ui.theme.bundledLobsterTwoFontFamily
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogic
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogicResult
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogicWithCallback
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialog
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialogOptions
import com.revenuecat.purchases.ui.revenuecatui.PaywallFooter
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.fonts.CustomFontProvider
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontProvider

private class TestAppPurchaseLogicSuspend : PurchaseLogic {

    companion object { private const val TAG = "PaywallTester" }

    override suspend fun performPurchase(
        activity: Activity,
        rcPackage: com.revenuecat.purchases.Package,
    ): PurchaseLogicResult {
        // Implement your app's custom purchase logic here.
        // If you are using Google Play, RevenueCat will automatically call `purchases.syncPurchases()` if
        // you return `.Success`. If you are using Amazon, you must call `purchases.syncAmazonPurchase()`.
        Log.d(TAG, "Custom purchase code in performPurchase was called.")
        return PurchaseLogicResult.Success
    }

    override suspend fun performRestore(customerInfo: CustomerInfo): PurchaseLogicResult {
        // Implement your app's custom restore logic here.
        // If you are using Google Play, RevenueCat will automatically call `purchases.syncPurchases()` if
        // you return `.Success`. If you are using Amazon, you must call `purchases.syncAmazonPurchase()`.
        Log.d(TAG, "Custom restore code in performRestore was called.")
        return PurchaseLogicResult.Error(PurchasesError(PurchasesErrorCode.PurchaseCancelledError))
    }
}

private class TestAppPurchaseLogicCallbacks : PurchaseLogicWithCallback() {

    companion object { private const val TAG = "PaywallTester" }

    override fun performPurchaseWithCompletion(
        activity: Activity,
        rcPackage: Package,
        completion: (PurchaseLogicResult) -> Unit,
    ) {
        // Implement your app's custom purchase logic here.
        // If you are using Google Play, RevenueCat will automatically call `purchases.syncPurchases()` if
        // you return `.Success`. If you are using Amazon, you must call `purchases.syncAmazonPurchase()`.
        Log.d(TAG, "Custom purchase code in performPurchaseWithCompletion was called.")
        completion(PurchaseLogicResult.Success)
    }

    override fun performRestoreWithCompletion(customerInfo: CustomerInfo, completion: (PurchaseLogicResult) -> Unit) {
        // Implement your app's custom restore logic here.
        // If you are using Google Play, RevenueCat will automatically call `purchases.syncPurchases()` if
        // you return `.Success`. If you are using Amazon, you must call `purchases.syncAmazonPurchase()`.
        Log.d(TAG, "Custom restore code in performRestoreWithCompletion was called.")
        completion(PurchaseLogicResult.Success)
    }
}

@Suppress("LongMethod")
@Composable
fun PaywallsScreen(
    samplePaywallsLoader: SamplePaywallsLoader = SamplePaywallsLoader(),
) {
    var displayPaywallState by remember { mutableStateOf<DisplayPaywallState>(DisplayPaywallState.None) }

    // This should be part of a view model so it survives activity recreations.
    // Temporarily holding this here for this test app.
    val useCallbackPurchaseLogic = true

    val myAppPurchaseLogic = remember {
        if (useCallbackPurchaseLogic) {
            TestAppPurchaseLogicCallbacks()
        } else {
            TestAppPurchaseLogicSuspend()
        }
    }

    LazyColumn {
        items(SamplePaywalls.SampleTemplate.values()) { template ->
            val offering = samplePaywallsLoader.offeringForTemplate(template)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = template.displayableName,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(8.dp),
                )
                ButtonWithEmoji(
                    onClick = {
                        displayPaywallState = DisplayPaywallState.FullScreen(
                            offering,
                            purchaseLogic = myAppPurchaseLogic,
                        )
                    },
                    emoji = "\uD83D\uDCF1",
                    label = "Full screen",
                )
                ButtonWithEmoji(
                    onClick = {
                        displayPaywallState = DisplayPaywallState.Footer(
                            offering,
                            condensed = false,
                            purchaseLogic = myAppPurchaseLogic,
                        )
                    },
                    emoji = "\uD83D\uDD3D",
                    label = "Footer",
                )
                ButtonWithEmoji(
                    onClick = {
                        displayPaywallState = DisplayPaywallState.Footer(
                            offering,
                            condensed = true,
                            purchaseLogic = myAppPurchaseLogic,
                        )
                    },
                    emoji = "\uD83D\uDDDC️",
                    label = "Condenser footer",
                )
                ButtonWithEmoji(
                    onClick = {
                        displayPaywallState = DisplayPaywallState.FullScreen(
                            offering,
                            CustomFontProvider(bundledLobsterTwoFontFamily),
                            purchaseLogic = myAppPurchaseLogic,
                        )
                    },
                    emoji = "\uD83C\uDD70️",
                    label = "Custom font",
                )
            }
        }
    }
    val currentState = displayPaywallState
    if (currentState is DisplayPaywallState.FullScreen) {
        FullScreenDialog(currentState) {
            displayPaywallState = DisplayPaywallState.None
        }
    } else if (currentState is DisplayPaywallState.Footer) {
        FooterDialog(currentState) {
            displayPaywallState = DisplayPaywallState.None
        }
    }
}

@Composable
private fun FullScreenDialog(currentState: DisplayPaywallState.FullScreen, onDismiss: () -> Unit) {
    PaywallDialog(
        PaywallDialogOptions.Builder()
            .setDismissRequest(onDismiss)
            .setOffering(currentState.offering)
            .setFontProvider(currentState.fontProvider)
            .AppPurchaseLogic(currentState.purchaseLogic)
            .build(),
    )
}

@Composable
private fun FooterDialog(currentState: DisplayPaywallState.Footer, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Scaffold { scaffoldPadding ->
            Box(modifier = Modifier.padding(scaffoldPadding)) {
                PaywallFooter(
                    options = PaywallOptions.Builder(dismissRequest = onDismiss)
                        .setOffering(currentState.offering)
                        .setPurchaseLogic(currentState.purchaseLogic)
                        .build(),
                    condensed = currentState.condensed,
                ) { footerPadding ->
                    SamplePaywall(paddingValues = footerPadding)
                }
            }
        }
    }
}

private sealed class DisplayPaywallState {
    object None : DisplayPaywallState()
    data class FullScreen
    constructor(
        val offering: Offering? = null,
        val fontProvider: FontProvider? = null,
        var purchaseLogic: PurchaseLogic? = null,
    ) : DisplayPaywallState()
    data class Footer(
        val offering: Offering? = null,
        val condensed: Boolean = false,
        var purchaseLogic: PurchaseLogic? = null,
    ) : DisplayPaywallState()
}

@Composable
private fun ButtonWithEmoji(
    emoji: String,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
        onClick = onClick,
        enabled = enabled,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 10.dp,
            pressedElevation = 15.dp,
            disabledElevation = 0.dp,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(text = emoji, modifier = Modifier.padding(end = 8.dp))
            Text(text = label)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PaywallsScreenPreview() {
    PaywallsScreen()
}
