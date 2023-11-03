package com.revenuecat.apitester.kotlin.revenuecatui

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialogOptions
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontProvider

@Suppress("unused", "UNUSED_VARIABLE")
@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
private class PaywallDialogOptionsAPI {
    fun check(
        requiredEntitlementIdentifier: String?,
        shouldDisplayBlock: ((CustomerInfo) -> Boolean)?,
        dismissRequest: () -> Unit,
        offering: Offering?,
        listener: PaywallListener?,
        fontProvider: FontProvider?,
    ) {
        val options: PaywallDialogOptions = PaywallDialogOptions.Builder()
            .setRequiredEntitlementIdentifier(requiredEntitlementIdentifier)
            .setShouldDisplayBlock(shouldDisplayBlock)
            .setDismissRequest(dismissRequest)
            .setOffering(offering)
            .setListener(listener)
            .setShouldDisplayDismissButton(true)
            .setFontProvider(fontProvider)
            .build()
        val shouldDisplayBlock2 = options.shouldDisplayBlock
        val offering2: Offering? = options.offering
        val listener2: PaywallListener? = options.listener
        val fontProvider2: FontProvider? = options.fontProvider
        val dismissRequest = options.dismissRequest
    }
}
