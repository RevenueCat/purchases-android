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

    fun registerHook(hook: RewardVerificationLifecycleHook) {
        val shouldRegister = synchronized(this) {
            hooks.add(hook)
            if (isRegistered) {
                false
            } else {
                isRegistered = true
                true
            }
        }

        if (shouldRegister) {
            listenerRegistrar.register(listener = this)
        }
    }

    @Synchronized
    fun unregisterHook(hook: RewardVerificationLifecycleHook) {
        hooks.remove(hook)
    }

    override fun onPurchasesConfigured(purchases: Purchases) {
        snapshotHooks().forEach { hook ->
            hook.onPurchasesConfigured(purchases)
        }
    }

    override fun onPurchasesClosed(purchases: Purchases) {
        snapshotHooks().forEach { hook ->
            hook.onPurchasesClosed(purchases)
        }
    }

    @Synchronized
    private fun snapshotHooks(): List<RewardVerificationLifecycleHook> = hooks.toList()
}
