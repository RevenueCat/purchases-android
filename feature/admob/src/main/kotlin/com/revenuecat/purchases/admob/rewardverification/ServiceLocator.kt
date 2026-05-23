package com.revenuecat.purchases.admob.rewardverification

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesService

internal interface RewardVerificationLifecycleHook {
    fun onPurchasesConfigured(purchases: Purchases)
    fun onPurchasesClosed(purchases: Purchases)
}

@OptIn(InternalRevenueCatAPI::class)
internal fun interface RewardVerificationListenerRegistrar {
    fun register(listener: PurchasesService)
}

@OptIn(InternalRevenueCatAPI::class)
internal class RewardVerificationServiceLocator(
    private val listenerRegistrar: RewardVerificationListenerRegistrar,
) : PurchasesService {

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

    override fun initialize(purchases: Purchases) {
        snapshotHooks().forEach { hook ->
            hook.onPurchasesConfigured(purchases)
        }
    }

    override fun close(purchases: Purchases) {
        snapshotHooks().forEach { hook ->
            hook.onPurchasesClosed(purchases)
        }
    }

    @Synchronized
    private fun snapshotHooks(): List<RewardVerificationLifecycleHook> = hooks.toList()
}
