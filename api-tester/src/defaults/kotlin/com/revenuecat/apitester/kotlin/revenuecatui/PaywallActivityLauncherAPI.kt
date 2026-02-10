package com.revenuecat.apitester.kotlin.revenuecatui

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.fragment.app.Fragment
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogic
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivityLaunchIfNeededOptions
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivityLaunchOptions
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivityLauncher
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallDisplayCallback
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResultHandler
import com.revenuecat.purchases.ui.revenuecatui.fonts.ParcelizableFontProvider

@Suppress("unused", "UNUSED_VARIABLE", "LongParameterList", "LongMethod")
private class PaywallActivityLauncherAPI {
    fun check(
        componentActivity: ComponentActivity,
        fragment: Fragment,
        resultCaller: ActivityResultCaller,
        resultHandler: PaywallResultHandler,
        offering: Offering,
        fontProvider: ParcelizableFontProvider,
        offeringIdentifier: String,
        paywallDisplayCallback: PaywallDisplayCallback,
    ) {
        val activityLauncher = PaywallActivityLauncher(componentActivity, resultHandler)
        val activityLauncher2 = PaywallActivityLauncher(fragment, resultHandler)
        val activityLauncher3 = PaywallActivityLauncher(resultCaller, resultHandler)
        activityLauncher.launch()
        activityLauncher.launch(offering)
        activityLauncher.launch(
            offering = offering,
            fontProvider = fontProvider,
        )
        activityLauncher.launch(
            offering = offering,
            fontProvider = fontProvider,
            shouldDisplayDismissButton = true,
            edgeToEdge = true,
        )
        activityLauncher.launch(
            offering = offering,
            fontProvider = fontProvider,
            shouldDisplayDismissButton = true,
            edgeToEdge = true,
            customVariables = mapOf("key" to CustomVariableValue.String("value")),
        )
        activityLauncher.launch(offeringIdentifier)
        activityLauncher.launch(
            offeringIdentifier = offeringIdentifier,
            fontProvider = fontProvider,
        )
        activityLauncher.launch(
            offeringIdentifier = offeringIdentifier,
            fontProvider = fontProvider,
            shouldDisplayDismissButton = true,
            edgeToEdge = true,
        )
        activityLauncher.launchIfNeeded("requiredEntitlementIdentifier")
        activityLauncher.launchIfNeeded(
            requiredEntitlementIdentifier = "requiredEntitlementIdentifier",
            offering = offering,
        )
        activityLauncher.launchIfNeeded(
            requiredEntitlementIdentifier = "requiredEntitlementIdentifier",
            offering = offering,
            fontProvider = fontProvider,
        )
        activityLauncher.launchIfNeeded(
            requiredEntitlementIdentifier = "requiredEntitlementIdentifier",
            offering = offering,
            fontProvider = fontProvider,
            shouldDisplayDismissButton = true,
            edgeToEdge = true,
        )
        activityLauncher.launchIfNeeded(
            requiredEntitlementIdentifier = "requiredEntitlementIdentifier",
            offering = offering,
            fontProvider = fontProvider,
            shouldDisplayDismissButton = true,
            edgeToEdge = true,
            paywallDisplayCallback = paywallDisplayCallback,
        )
        activityLauncher.launchIfNeeded(
            requiredEntitlementIdentifier = "requiredEntitlementIdentifier",
            offeringIdentifier = offeringIdentifier,
        )
        activityLauncher.launchIfNeeded(
            requiredEntitlementIdentifier = "requiredEntitlementIdentifier",
            offeringIdentifier = offeringIdentifier,
            fontProvider = fontProvider,
        )
        activityLauncher.launchIfNeeded(
            requiredEntitlementIdentifier = "requiredEntitlementIdentifier",
            offeringIdentifier = offeringIdentifier,
            fontProvider = fontProvider,
            shouldDisplayDismissButton = true,
            edgeToEdge = true,
        )
        activityLauncher.launchIfNeeded(
            requiredEntitlementIdentifier = "requiredEntitlementIdentifier",
            offeringIdentifier = offeringIdentifier,
            fontProvider = fontProvider,
            shouldDisplayDismissButton = true,
            edgeToEdge = true,
            paywallDisplayCallback = paywallDisplayCallback,
        )
        // Trailing lambda syntax works correctly
        activityLauncher.launchIfNeeded {
            val customerInfo: CustomerInfo = it
            true
        }
        activityLauncher.launchIfNeeded(
            offering = offering,
            fontProvider = fontProvider,
            shouldDisplayDismissButton = true,
        ) {
            val customerInfo: CustomerInfo = it
            true
        }
        activityLauncher.launchIfNeeded(
            offering = offering,
            fontProvider = fontProvider,
            shouldDisplayDismissButton = true,
            edgeToEdge = true,
        ) {
            val customerInfo: CustomerInfo = it
            true
        }
    }

    fun checkBuilderPattern(
        activityLauncher: PaywallActivityLauncher,
        offering: Offering,
        fontProvider: ParcelizableFontProvider,
        paywallDisplayCallback: PaywallDisplayCallback,
        purchaseLogic: PurchaseLogic,
        listener: PaywallListener,
    ) {
        val customVariables = mapOf("key" to CustomVariableValue.String("value"))

        // Basic launch with builder
        val options = PaywallActivityLaunchOptions.Builder()
            .setOffering(offering)
            .setFontProvider(fontProvider)
            .setShouldDisplayDismissButton(true)
            .setEdgeToEdge(true)
            .setCustomVariables(customVariables)
            .setPurchaseLogic(purchaseLogic)
            .setListener(listener)
            .build()
        activityLauncher.launchWithOptions(options)

        // LaunchIfNeeded with requiredEntitlementIdentifier (all builder methods)
        val optionsWithEntitlement = PaywallActivityLaunchIfNeededOptions.Builder()
            .setRequiredEntitlementIdentifier("premium")
            .setOffering(offering)
            .setFontProvider(fontProvider)
            .setShouldDisplayDismissButton(true)
            .setEdgeToEdge(true)
            .setCustomVariables(customVariables)
            .setPaywallDisplayCallback(paywallDisplayCallback)
            .setPurchaseLogic(purchaseLogic)
            .setListener(listener)
            .build()
        activityLauncher.launchIfNeededWithOptions(optionsWithEntitlement)

        // LaunchIfNeeded with shouldDisplayBlock (all builder methods)
        val optionsWithBlock = PaywallActivityLaunchIfNeededOptions.Builder()
            .setShouldDisplayBlock { customerInfo ->
                customerInfo.entitlements.active.isEmpty()
            }
            .setOffering(offering)
            .setFontProvider(fontProvider)
            .setShouldDisplayDismissButton(true)
            .setEdgeToEdge(true)
            .setCustomVariables(customVariables)
            .setPaywallDisplayCallback(paywallDisplayCallback)
            .setPurchaseLogic(null)
            .setListener(null)
            .build()
        activityLauncher.launchIfNeededWithOptions(optionsWithBlock)
    }

    fun checkPaywallDisplayCallback() {
        @Suppress("EmptyFunctionBlock")
        val paywallDisplayCallback = object : PaywallDisplayCallback {
            override fun onPaywallDisplayResult(wasDisplayed: Boolean) {}
        }
    }
}
