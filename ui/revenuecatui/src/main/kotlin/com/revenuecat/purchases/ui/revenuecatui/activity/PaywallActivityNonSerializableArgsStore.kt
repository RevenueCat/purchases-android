package com.revenuecat.purchases.ui.revenuecatui.activity

import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogic

internal data class PaywallActivityNonSerializableArgs(
    val purchaseLogic: PurchaseLogic? = null,
    val listener: PaywallListener? = null,
)

internal object PaywallActivityNonSerializableArgsStore {
    private var currentArgs: PaywallActivityNonSerializableArgs? = null

    @Synchronized
    fun set(args: PaywallActivityNonSerializableArgs) {
        currentArgs = args
    }

    @Synchronized
    fun get(): PaywallActivityNonSerializableArgs? = currentArgs

    @Synchronized
    fun clear() {
        currentArgs = null
    }
}
