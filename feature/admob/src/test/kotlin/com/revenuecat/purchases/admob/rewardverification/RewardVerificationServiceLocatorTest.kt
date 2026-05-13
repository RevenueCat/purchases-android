package com.revenuecat.purchases.admob.rewardverification

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

@OptIn(InternalRevenueCatAPI::class)
internal class RewardVerificationServiceLocatorTest {

    @Test
    fun `registerHook registers locator listener only once`() {
        val listenerRegistrar = mockk<RewardVerificationListenerRegistrar>(relaxed = true)
        val locator = RewardVerificationServiceLocator(listenerRegistrar)
        val hookOne = mockk<RewardVerificationLifecycleHook>(relaxed = true)
        val hookTwo = mockk<RewardVerificationLifecycleHook>(relaxed = true)

        locator.registerHook(hookOne)
        locator.registerHook(hookTwo)

        verify(exactly = 1) {
            listenerRegistrar.register(locator)
        }
    }

    @Test
    fun `registered hooks receive lifecycle callbacks and unregistered hooks do not`() {
        val listenerRegistrar = mockk<RewardVerificationListenerRegistrar>()
        every { listenerRegistrar.register(any()) } just Runs
        val locator = RewardVerificationServiceLocator(listenerRegistrar)
        val hook = mockk<RewardVerificationLifecycleHook>(relaxed = true)
        val purchases = mockk<Purchases>(relaxed = true)

        locator.registerHook(hook)
        clearMocks(hook)

        locator.onPurchasesConfigured(purchases)
        locator.onPurchasesClosed(purchases)

        verify(exactly = 1) {
            hook.onPurchasesConfigured(purchases)
            hook.onPurchasesClosed(purchases)
        }

        locator.unregisterHook(hook)
        locator.onPurchasesConfigured(purchases)
        locator.onPurchasesClosed(purchases)

        verify(exactly = 1) {
            hook.onPurchasesConfigured(purchases)
            hook.onPurchasesClosed(purchases)
        }
    }
}
