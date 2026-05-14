package com.revenuecat.purchases.admob.rewardverification

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesLifecycleListener

internal interface RewardVerificationLifecycleHook {
    fun onPurchasesConfigured(purchases: Purchases)
    fun onPurchasesClosed(purchases: Purchases)
}

@OptIn(InternalRevenueCatAPI::class)
internal fun interface RewardVerificationListenerRegistrar {
    fun register(listener: PurchasesLifecycleListener)
}

@OptIn(InternalRevenueCatAPI::class)
internal class RewardVerificationServiceLocator(
    private val listenerRegistrar: RewardVerificationListenerRegistrar,
) : PurchasesLifecycleListener {

    private var isRegistered = false
    private val hooks = mutableSetOf<RewardVerificationLifecycleHook>()

    @Synchronized
    fun registerHook(hook: RewardVerificationLifecycleHook) {
        hooks.add(hook)
        ensureRegistered()
    }

    @Synchronized
    fun unregisterHook(hook: RewardVerificationLifecycleHook) {
        hooks.remove(hook)
    }

    @Synchronized
    override fun onPurchasesConfigured(@Suppress("UnusedParameter") purchases: Purchases) {
        snapshotHooks().forEach { hook ->
            hook.onPurchasesConfigured(purchases)
        }
    }

    @Synchronized
    override fun onPurchasesClosed(@Suppress("UnusedParameter") purchases: Purchases) {
        snapshotHooks().forEach { hook ->
            hook.onPurchasesClosed(purchases)
        }
    }

    @Synchronized
    private fun ensureRegistered() {
        if (isRegistered) return

        listenerRegistrar.register(listener = this)
        isRegistered = true
    }

    private fun snapshotHooks(): List<RewardVerificationLifecycleHook> {
        return hooks.toList()
    }
}
