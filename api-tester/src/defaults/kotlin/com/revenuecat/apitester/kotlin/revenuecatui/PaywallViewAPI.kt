package com.revenuecat.apitester.kotlin.revenuecatui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.ui.platform.AbstractComposeView
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallPurchaseLogic
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontProvider
import com.revenuecat.purchases.ui.revenuecatui.views.PaywallView

@Suppress("unused", "UNUSED_VARIABLE")
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
        purchaseLogic: PaywallPurchaseLogic,
    ) {
        PaywallView(context)
        PaywallView(context, attrs)
        PaywallView(context, attrs, defStyleAttr)
        PaywallView(context, offering)
        PaywallView(context, offering, listener)
        PaywallView(context, offering, listener, fontProvider)
        PaywallView(context, offering, listener, fontProvider, shouldDisplayDismissButton)
        PaywallView(context, offering, listener, fontProvider, shouldDisplayDismissButton, dismissHandler = {})
        PaywallView(context, offering, listener, fontProvider, shouldDisplayDismissButton, purchaseLogic)
        PaywallView(context, offering, listener, fontProvider, shouldDisplayDismissButton, purchaseLogic) {
            // dismiss
        }
        PaywallView(
            context,
            offering,
            listener,
            fontProvider,
            shouldDisplayDismissButton,
            purchaseLogic,
            dismissHandler = {},
        )
    }

    @Suppress("LongParameterList")
    fun checkMethods(
        paywallView: PaywallView,
        paywallListener: PaywallListener,
        dismissHandler: () -> Unit,
        presentedOfferingContext: PresentedOfferingContext?,
        customVariables: Map<String, CustomVariableValue>,
        purchaseLogic: PaywallPurchaseLogic,
    ) {
        paywallView.setPaywallListener(null)
        paywallView.setPaywallListener(paywallListener)
        paywallView.setDismissHandler(null)
        paywallView.setDismissHandler(dismissHandler)
        paywallView.setOfferingId(null)
        paywallView.setOfferingId("offering_id")
        paywallView.setOfferingId(null, presentedOfferingContext)
        paywallView.setOfferingId("offering_id", presentedOfferingContext)
        paywallView.setCustomVariables(customVariables)
        paywallView.setPurchaseLogic(null)
        paywallView.setPurchaseLogic(purchaseLogic)
    }
}
