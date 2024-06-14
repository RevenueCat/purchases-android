package com.revenuecat.apitester.kotlin.revenuecatui

import androidx.compose.runtime.Composable
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialog
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialogOptions
import com.revenuecat.purchases.ui.revenuecatui.PaywallFooter
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions

@Suppress("unused", "UNUSED_VARIABLE")
private class PaywallAPI {

    @Composable
    fun check(options: PaywallOptions) {
        Paywall(options = options)
    }

    @Composable
    fun checkFooter(options: PaywallOptions) {
        PaywallFooter(options = options)
        PaywallFooter(options = options, condensed = true)
        PaywallFooter(options = options) {}
        PaywallFooter(options = options, condensed = true) {}
    }

    @Composable
    fun checkDialog(options: PaywallDialogOptions) {
        PaywallDialog(paywallDialogOptions = options)
    }
}
