package com.revenuecat.apitester.kotlin.revenuecatui

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.fragment.app.Fragment
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivityLauncher
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallDisplayCallback
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResultHandler
import com.revenuecat.purchases.ui.revenuecatui.fonts.ParcelizableFontProvider

@Suppress("unused", "UNUSED_VARIABLE", "LongParameterList", "LongMethod")
@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
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
        )
        activityLauncher.launchIfNeeded(
            requiredEntitlementIdentifier = "requiredEntitlementIdentifier",
            offering = offering,
            fontProvider = fontProvider,
            shouldDisplayDismissButton = true,
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
        )
        activityLauncher.launchIfNeeded(
            requiredEntitlementIdentifier = "requiredEntitlementIdentifier",
            offeringIdentifier = offeringIdentifier,
            fontProvider = fontProvider,
            shouldDisplayDismissButton = true,
            paywallDisplayCallback = paywallDisplayCallback,
        )
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
    }

    fun checkPaywallDisplayCallback() {
        @Suppress("EmptyFunctionBlock")
        val paywallDisplayCallback = object : PaywallDisplayCallback {
            override fun onPaywallShouldDisplay(shouldDisplayPaywall: Boolean) {}
        }
    }
}
