package com.revenuecat.apitester.kotlin.revenuecatui

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.PaywallInteractionEvent
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.utils.Resumable

@Suppress("unused", "UNUSED_VARIABLE", "EmptyFunctionBlock")
private class PaywallListenerAPI {
    fun check() {
        val listener = object : PaywallListener {
            override fun onPurchasePackageInitiated(rcPackage: Package, resume: Resumable) {}

            override fun onPurchaseStarted(rcPackage: Package) {}

            override fun onPurchaseError(error: PurchasesError) {}

            override fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {}

            override fun onPurchaseCancelled() {}

            override fun onRestoreInitiated(resume: Resumable) {}

            override fun onRestoreStarted() {}

            override fun onRestoreError(error: PurchasesError) {}

            override fun onRestoreCompleted(customerInfo: CustomerInfo) {}

            override fun onWebCheckoutOpened() {}

            override fun onUrlOpened(url: String) {}

            override fun onInteraction(event: PaywallInteractionEvent) {
                val rawProperties: Map<String, Any> = event.rawProperties
                val string: String? = event.getProperty(PaywallInteractionEvent.Keys.COMPONENT_TYPE)
                val int: Int? = event.getProperty(PaywallInteractionEvent.Keys.ORIGIN_INDEX)
                val long: Long? = event.getProperty(PaywallInteractionEvent.Keys.TIMESTAMP)
                val boolean: Boolean? = event.getProperty(PaywallInteractionEvent.Keys.DARK_MODE)
                val name: String = PaywallInteractionEvent.Keys.COMPONENT_TYPE.name
            }
        }

        val keys: List<PaywallInteractionEvent.Key<*>> = listOf(
            PaywallInteractionEvent.Keys.TIMESTAMP,
            PaywallInteractionEvent.Keys.SESSION_ID,
            PaywallInteractionEvent.Keys.OFFERING_ID,
            PaywallInteractionEvent.Keys.PAYWALL_ID,
            PaywallInteractionEvent.Keys.PAYWALL_REVISION,
            PaywallInteractionEvent.Keys.DISPLAY_MODE,
            PaywallInteractionEvent.Keys.DARK_MODE,
            PaywallInteractionEvent.Keys.LOCALE,
            PaywallInteractionEvent.Keys.COMPONENT_TYPE,
            PaywallInteractionEvent.Keys.COMPONENT_VALUE,
            PaywallInteractionEvent.Keys.COMPONENT_NAME,
            PaywallInteractionEvent.Keys.COMPONENT_URL,
            PaywallInteractionEvent.Keys.ORIGIN_INDEX,
            PaywallInteractionEvent.Keys.DESTINATION_INDEX,
            PaywallInteractionEvent.Keys.ORIGIN_CONTEXT_NAME,
            PaywallInteractionEvent.Keys.DESTINATION_CONTEXT_NAME,
            PaywallInteractionEvent.Keys.DEFAULT_INDEX,
            PaywallInteractionEvent.Keys.ORIGIN_PACKAGE_ID,
            PaywallInteractionEvent.Keys.DESTINATION_PACKAGE_ID,
            PaywallInteractionEvent.Keys.DEFAULT_PACKAGE_ID,
            PaywallInteractionEvent.Keys.CURRENT_PACKAGE_ID,
            PaywallInteractionEvent.Keys.RESULTING_PACKAGE_ID,
            PaywallInteractionEvent.Keys.ORIGIN_PRODUCT_ID,
            PaywallInteractionEvent.Keys.DESTINATION_PRODUCT_ID,
            PaywallInteractionEvent.Keys.DEFAULT_PRODUCT_ID,
            PaywallInteractionEvent.Keys.CURRENT_PRODUCT_ID,
            PaywallInteractionEvent.Keys.RESULTING_PRODUCT_ID,
        )
        val componentTypes: List<String> = listOf(
            PaywallInteractionEvent.ComponentTypes.TAB,
            PaywallInteractionEvent.ComponentTypes.SWITCH,
            PaywallInteractionEvent.ComponentTypes.CAROUSEL,
            PaywallInteractionEvent.ComponentTypes.BUTTON,
            PaywallInteractionEvent.ComponentTypes.TEXT,
            PaywallInteractionEvent.ComponentTypes.PACKAGE,
            PaywallInteractionEvent.ComponentTypes.PACKAGE_SELECTION_SHEET,
            PaywallInteractionEvent.ComponentTypes.PURCHASE_BUTTON,
        )
    }
}
