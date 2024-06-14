package com.revenuecat.apitester.kotlin.revenuecatui

import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontProvider

@Suppress("unused", "UNUSED_VARIABLE")
private class PaywallOptionsAPI {
    fun check(offering: Offering?, listener: PaywallListener?, fontProvider: FontProvider?) {
        val options: PaywallOptions = PaywallOptions.Builder(dismissRequest = {})
            .setOffering(offering)
            .setListener(listener)
            .setShouldDisplayDismissButton(true)
            .setFontProvider(fontProvider)
            .build()
        val listener2: PaywallListener? = options.listener
        val fontProvider2: FontProvider? = options.fontProvider
        val dismissRequest: () -> Unit = options.dismissRequest
    }
}
