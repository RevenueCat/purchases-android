package com.revenuecat.rcttester

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogic
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivityLaunchIfNeededOptions
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivityLaunchOptions
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivityLauncher
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResult
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResultHandler
import com.revenuecat.rcttester.config.SDKConfiguration
import com.revenuecat.rcttester.ui.configuration.ConfigurationScreen
import com.revenuecat.rcttester.ui.main.MainScreen
import com.revenuecat.rcttester.ui.offerings.OfferingsScreen
import com.revenuecat.rcttester.ui.offerings.PaywallActivityCallbacks
import com.revenuecat.rcttester.ui.theme.AndroidsdkTheme

sealed class Screen {
    data object Configuration : Screen()
    data object Main : Screen()
    data object Offerings : Screen()
}

class MainActivity : ComponentActivity(), PaywallResultHandler {
    private lateinit var paywallActivityLauncher: PaywallActivityLauncher
    val paywallResultState = mutableStateOf<PaywallResult?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        paywallActivityLauncher = PaywallActivityLauncher(this, this)

        val application = application as MainApplication

        setContent {
            AndroidsdkTheme {
                RCTTesterApp(application = application, activity = this)
            }
        }
    }

    override fun onActivityResult(result: PaywallResult) {
        paywallResultState.value = result
    }

    @OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
    fun buildPaywallActivityCallbacks(purchaseLogic: PurchaseLogic?): PaywallActivityCallbacks =
        PaywallActivityCallbacks(
            onLaunchPaywallActivity = { offering ->
                paywallActivityLauncher.launchWithOptions(
                    PaywallActivityLaunchOptions.Builder()
                        .setOffering(offering)
                        .setPurchaseLogic(purchaseLogic)
                        .build(),
                )
            },
            onLaunchPaywallActivityIfNeeded = { entitlement, offering ->
                paywallActivityLauncher.launchIfNeededWithOptions(
                    PaywallActivityLaunchIfNeededOptions.Builder()
                        .setRequiredEntitlementIdentifier(entitlement)
                        .setOffering(offering)
                        .setPurchaseLogic(purchaseLogic)
                        .build(),
                )
            },
            paywallResultState = paywallResultState,
        )
}

@Composable
fun RCTTesterApp(application: MainApplication, activity: MainActivity) {
    val isSDKConfigured by application.isSDKConfiguredState

    var currentScreen by remember {
        mutableStateOf<Screen>(
            if (isSDKConfigured) Screen.Main else Screen.Configuration,
        )
    }
    var configuration by remember {
        mutableStateOf(
            SDKConfiguration.load(application) ?: SDKConfiguration.default(),
        )
    }
    // Navigate to main screen when SDK becomes configured (observes isSDKConfiguredState)
    LaunchedEffect(isSDKConfigured) {
        if (isSDKConfigured && currentScreen == Screen.Configuration) {
            currentScreen = Screen.Main
        }
    }

    when (currentScreen) {
        is Screen.Configuration -> ConfigurationScreen(
            initialConfiguration = configuration,
            onConfigure = { newConfig ->
                application.configureSDK(newConfig)
                configuration = newConfig
                currentScreen = Screen.Main
            },
        )
        is Screen.Main -> MainScreen(
            configuration = configuration,
            onConfigurationUpdate = { configuration = it },
            onReconfigure = { currentScreen = Screen.Configuration },
            onNavigateToOfferings = { currentScreen = Screen.Offerings },
        )
        is Screen.Offerings -> OfferingsScreen(
            purchaseManager = application.purchaseManager,
            onNavigateBack = { currentScreen = Screen.Main },
            paywallActivityCallbacks = activity.buildPaywallActivityCallbacks(
                purchaseLogic = application.purchaseManager?.purchaseLogic,
            ),
        )
    }
}
