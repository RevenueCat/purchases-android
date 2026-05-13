package com.revenuecat.purchases.admob.rewardverification

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesLifecycleEventBus
import com.revenuecat.purchases.PurchasesLifecycleListener
import java.util.Collections

internal interface RewardVerificationLifecycleHook {
    fun onPurchasesConfigured(purchases: Purchases)
    fun onPurchasesClosed(purchases: Purchases)
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
internal object RewardVerificationServiceLocator : PurchasesLifecycleListener {

    private var isRegistered = false
    private val hooks = Collections.synchronizedSet(mutableSetOf<RewardVerificationLifecycleHook>())

    @Synchronized
    fun registerHook(hook: RewardVerificationLifecycleHook) {
        ensureRegistered()
        hooks.add(hook)
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

        PurchasesLifecycleEventBus.register(listener = this)
        isRegistered = true
    }

    private fun snapshotHooks(): List<RewardVerificationLifecycleHook> {
        synchronized(hooks) {
            return hooks.toList()
        }
    }
}
