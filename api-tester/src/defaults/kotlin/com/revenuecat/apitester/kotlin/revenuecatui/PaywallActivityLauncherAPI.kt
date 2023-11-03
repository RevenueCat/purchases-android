package com.revenuecat.apitester.kotlin.revenuecatui

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivityLauncher
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResultHandler
import com.revenuecat.purchases.ui.revenuecatui.fonts.ParcelizableFontProvider

@Suppress("unused", "UNUSED_VARIABLE")
@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
private class PaywallActivityLauncherAPI {
    fun check(
        componentActivity: ComponentActivity,
        fragment: Fragment,
        resultHandler: PaywallResultHandler,
        offering: Offering,
        fontProvider: ParcelizableFontProvider,
    ) {
        val activityLauncher = PaywallActivityLauncher(componentActivity, resultHandler)
        val activityLauncher2 = PaywallActivityLauncher(fragment, resultHandler)
        activityLauncher.launch()
        activityLauncher.launch(offering)
        activityLauncher.launch(
            offering = offering,
            fontProvider = fontProvider,
            shouldDisplayDismissButton = true,
        )
        activityLauncher.launchIfNeeded("requiredEntitlementIdentifier")
        activityLauncher.launchIfNeeded(
            requiredEntitlementIdentifier = "requiredEntitlementIdentifier",
            offering = offering,
            fontProvider = fontProvider,
            shouldDisplayDismissButton = true,
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
}
