package com.revenuecat.apitester.kotlin.revenuecatui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.ui.platform.AbstractComposeView
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontProvider
import com.revenuecat.purchases.ui.revenuecatui.views.PaywallView

@Suppress("unused", "UNUSED_VARIABLE")
@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
private class PaywallViewAPI {

    fun checkType(context: Context) {
        val paywallView: AbstractComposeView = PaywallView(context)
    }

    @Suppress("LongParameterList")
    fun checkConstructors(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        offering: Offering,
        listener: PaywallListener,
        fontProvider: FontProvider,
        shouldDisplayDismissButton: Boolean?,
        dismissHandler: () -> Unit,
    ) {
        PaywallView(context)
        PaywallView(context, attrs)
        PaywallView(context, attrs, defStyleAttr)
        PaywallView(context, offering)
        PaywallView(context, offering, listener)
        PaywallView(context, offering, listener, fontProvider)
        PaywallView(context, offering, listener, fontProvider, shouldDisplayDismissButton)
        PaywallView(context, offering, listener, fontProvider, shouldDisplayDismissButton, dismissHandler)
    }

    fun checkMethods(
        paywallView: PaywallView,
        paywallListener: PaywallListener,
        dismissHandler: () -> Unit,
    ) {
        paywallView.setPaywallListener(null)
        paywallView.setPaywallListener(paywallListener)
        paywallView.setDismissHandler(null)
        paywallView.setDismissHandler(dismissHandler)
        paywallView.setOfferingId(null)
        paywallView.setOfferingId("offering_id")
    }
}
