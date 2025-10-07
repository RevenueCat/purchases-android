package com.revenuecat.apitester.kotlin.revenuecatui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.ui.platform.AbstractComposeView
import com.revenuecat.purchases.customercenter.CustomerCenterListener
import com.revenuecat.purchases.ui.revenuecatui.views.CustomerCenterView

@Suppress("unused", "UNUSED_VARIABLE")
private class CustomerCenterViewAPI {

    fun checkType(context: Context) {
        val view: AbstractComposeView = CustomerCenterView(context)
    }

    fun checkConstructors(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        dismissHandler: () -> Unit,
        listener: CustomerCenterListener,
    ) {
        CustomerCenterView(context)
        CustomerCenterView(context, attrs)
        CustomerCenterView(context, attrs, defStyleAttr)
        CustomerCenterView(context, dismissHandler)
        CustomerCenterView(context, dismissHandler, listener)
    }

    fun checkMethods(
        view: CustomerCenterView,
        dismissHandler: () -> Unit,
        listener: CustomerCenterListener,
    ) {
        view.setDismissHandler(null)
        view.setDismissHandler(dismissHandler)
        view.setCustomerCenterListener(null)
        view.setCustomerCenterListener(listener)
    }
}
