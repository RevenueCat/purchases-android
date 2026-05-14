package com.revenuecat.purchases.admob.rewardverification

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesLifecycle
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(InternalRevenueCatAPI::class)
internal class RewardVerificationServiceLocatorTest {

    @Test
    fun `default registrar uses purchases lifecycle registration`() {
        mockkObject(PurchasesLifecycle)
        every { PurchasesLifecycle.register(any()) } just Runs
        val locator = RewardVerificationServiceLocator()
        val hook = mockk<RewardVerificationLifecycleHook>(relaxed = true)

        try {
            locator.registerHook(hook)

            verify(exactly = 1) {
                PurchasesLifecycle.register(locator)
            }
        } finally {
            unmockkObject(PurchasesLifecycle)
        }
    }

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

    @Test
    fun `hook callbacks can mutate locator without deadlock and use a hook snapshot`() {
        val listenerRegistrar = mockk<RewardVerificationListenerRegistrar>()
        every { listenerRegistrar.register(any()) } just Runs
        val locator = RewardVerificationServiceLocator(listenerRegistrar)
        val purchases = mockk<Purchases>(relaxed = true)
        val lateHook = mockk<RewardVerificationLifecycleHook>(relaxed = true)
        val callbackCompleted = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()

        val reentrantHook = object : RewardVerificationLifecycleHook {
            override fun onPurchasesConfigured(purchases: Purchases) {
                val currentHook = this
                executor.submit<Unit> {
                    locator.unregisterHook(currentHook)
                    locator.registerHook(lateHook)
                }.get(1, TimeUnit.SECONDS)
                callbackCompleted.countDown()
            }

            override fun onPurchasesClosed(purchases: Purchases) = Unit
        }

        try {
            locator.registerHook(reentrantHook)

            locator.onPurchasesConfigured(purchases)

            assertTrue(callbackCompleted.await(1, TimeUnit.SECONDS))
            verify(exactly = 0) { lateHook.onPurchasesConfigured(any()) }

            locator.onPurchasesConfigured(purchases)

            verify(exactly = 1) { lateHook.onPurchasesConfigured(purchases) }
        } finally {
            executor.shutdownNow()
        }
    }
}
