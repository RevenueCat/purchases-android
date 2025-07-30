package com.revenuecat.apitester.kotlin.revenuecatui

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontProvider
import com.revenuecat.purchases.ui.revenuecatui.views.OriginalTemplatePaywallFooterView

@Suppress("unused", "UNUSED_VARIABLE")
private class PaywallFooterViewAPI {

    fun checkType(context: Context) {
        val paywallFooterView: FrameLayout = OriginalTemplatePaywallFooterView(context)
    }

    @Suppress("LongParameterList")
    fun checkConstructors(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        offering: Offering,
        listener: PaywallListener,
        fontProvider: FontProvider,
        condensed: Boolean,
        dismissHandler: () -> Unit,
    ) {
        OriginalTemplatePaywallFooterView(context)
        OriginalTemplatePaywallFooterView(context, attrs)
        OriginalTemplatePaywallFooterView(context, attrs, defStyleAttr)
        OriginalTemplatePaywallFooterView(context, offering)
        OriginalTemplatePaywallFooterView(context, offering, listener)
        OriginalTemplatePaywallFooterView(context, offering, listener, fontProvider)
        OriginalTemplatePaywallFooterView(context, offering, listener, fontProvider, condensed)
        OriginalTemplatePaywallFooterView(context, offering, listener, fontProvider, condensed, dismissHandler)
    }

    fun checkMethods(
        paywallFooterView: OriginalTemplatePaywallFooterView,
        paywallListener: PaywallListener,
        dismissHandler: () -> Unit,
    ) {
        paywallFooterView.setPaywallListener(null)
        paywallFooterView.setPaywallListener(paywallListener)
        paywallFooterView.setDismissHandler(null)
        paywallFooterView.setDismissHandler(dismissHandler)
        paywallFooterView.setOfferingId(null)
        paywallFooterView.setOfferingId("offering_id")
    }
}
