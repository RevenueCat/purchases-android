package com.revenuecat.purchases.ui.revenuecatui.activity

import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogic

internal data class PaywallActivityNonSerializableArgs(
    val purchaseLogic: PurchaseLogic? = null,
    val listener: PaywallListener? = null,
)

internal object PaywallActivityNonSerializableArgsStore {
    private val argsMap = mutableMapOf<Int, PaywallActivityNonSerializableArgs>()

    @Synchronized
    fun store(args: PaywallActivityNonSerializableArgs): Int {
        val key = System.identityHashCode(args)
        argsMap[key] = args
        return key
    }

    @Synchronized
    fun get(key: Int): PaywallActivityNonSerializableArgs? = argsMap[key]

    @Synchronized
    fun remove(key: Int) {
        argsMap.remove(key)
    }

    @Synchronized
    fun clear() {
        argsMap.clear()
    }
}
