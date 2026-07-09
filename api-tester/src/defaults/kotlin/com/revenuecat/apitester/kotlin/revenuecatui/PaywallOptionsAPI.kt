package com.revenuecat.apitester.kotlin.revenuecatui

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.PaywallPurchaseLogic
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontProvider

@Suppress("unused", "UNUSED_VARIABLE")
private class PaywallOptionsAPI {

    suspend fun check(
        offering: Offering?,
        listener: PaywallListener?,
        fontProvider: FontProvider?,
        purchaseLogic: PaywallPurchaseLogic?,
        customVariables: Map<String, CustomVariableValue>,
    ) {
        val options: PaywallOptions = PaywallOptions.Builder(dismissRequest = {})
            .setOffering(offering)
            .setListener(listener)
            .setShouldDisplayDismissButton(true)
            .setFontProvider(fontProvider)
            .setPurchaseLogic(purchaseLogic)
            .setCustomVariables(customVariables)
            .build()
        val listener2: PaywallListener? = options.listener
        val fontProvider2: FontProvider? = options.fontProvider
        val dismissRequest: () -> Unit = options.dismissRequest
        val purchaseLogic2: PaywallPurchaseLogic? = options.purchaseLogic
        val customVariables2: Map<String, CustomVariableValue> = options.customVariables
    }

    @OptIn(InternalRevenueCatAPI::class)
    private fun checkInjectedWorkflow(
        builder: PaywallOptions.Builder,
        workflow: PublishedWorkflow,
        offering: Offering?,
    ) {
        val configured: PaywallOptions.Builder = builder.injectedWorkflow(workflow, offering)
    }
}
