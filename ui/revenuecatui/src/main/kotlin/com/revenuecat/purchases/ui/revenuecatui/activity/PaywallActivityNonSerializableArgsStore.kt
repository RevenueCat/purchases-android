package com.revenuecat.purchases.ui.revenuecatui.activity

import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallPurchaseLogic

internal data class PaywallActivityNonSerializableArgs(
    val purchaseLogic: PaywallPurchaseLogic? = null,
    val listener: PaywallListener? = null,
)

internal object PaywallActivityNonSerializableArgsStore {
    private val argsByHashCode = mutableMapOf<Int, PaywallActivityNonSerializableArgs>()

    @Synchronized
    fun store(args: PaywallActivityNonSerializableArgs): Int {
        val key = System.identityHashCode(args)
        argsByHashCode[key] = args
        return key
    }

    @Synchronized
    fun get(key: Int): PaywallActivityNonSerializableArgs? = argsByHashCode[key]

    @Synchronized
    fun remove(key: Int) {
        argsByHashCode.remove(key)
    }

    @Synchronized
    fun clear() {
        argsByHashCode.clear()
    }
}
