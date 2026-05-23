package com.revenuecat.purchases.admob.rewardverification

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(InternalRevenueCatAPI::class)
internal class RewardVerificationServiceLocatorTest {

    @Test
    fun `registerHook registers locator listener only once`() {
        val serviceRegistrar = mockk<RewardVerificationServiceRegistrar>(relaxed = true)
        val locator = RewardVerificationServiceLocator(serviceRegistrar)
        val hookOne = mockk<RewardVerificationLifecycleHook>(relaxed = true)
        val hookTwo = mockk<RewardVerificationLifecycleHook>(relaxed = true)

        locator.registerHook(hookOne)
        locator.registerHook(hookTwo)

        verify(exactly = 1) {
            serviceRegistrar.register(locator)
        }
    }

    @Test
    fun `registered hooks receive lifecycle callbacks and unregistered hooks do not`() {
        val serviceRegistrar = mockk<RewardVerificationServiceRegistrar>()
        every { serviceRegistrar.register(any()) } just Runs
        val locator = RewardVerificationServiceLocator(serviceRegistrar)
        val hook = mockk<RewardVerificationLifecycleHook>(relaxed = true)
        val purchases = mockk<Purchases>(relaxed = true)

        locator.registerHook(hook)
        clearMocks(hook)

        locator.initialize(purchases)
        locator.close(purchases)

        verify(exactly = 1) {
            hook.onPurchasesConfigured(purchases)
            hook.onPurchasesClosed(purchases)
        }

        locator.unregisterHook(hook)
        locator.initialize(purchases)
        locator.close(purchases)

        verify(exactly = 1) {
            hook.onPurchasesConfigured(purchases)
            hook.onPurchasesClosed(purchases)
        }
    }

    @Test
    fun `hook callbacks can mutate locator without deadlock and use a hook snapshot`() {
        val serviceRegistrar = mockk<RewardVerificationServiceRegistrar>()
        every { serviceRegistrar.register(any()) } just Runs
        val locator = RewardVerificationServiceLocator(serviceRegistrar)
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

            locator.initialize(purchases)

            assertTrue(callbackCompleted.await(1, TimeUnit.SECONDS))
            verify(exactly = 0) { lateHook.onPurchasesConfigured(any()) }

            locator.initialize(purchases)

            verify(exactly = 1) { lateHook.onPurchasesConfigured(purchases) }
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `replayed configured callback can mutate locator from another thread without deadlock`() {
        val purchases = mockk<Purchases>(relaxed = true)
        lateinit var locator: RewardVerificationServiceLocator
        val serviceRegistrar = RewardVerificationServiceRegistrar { listener ->
            listener.initialize(purchases)
        }
        locator = RewardVerificationServiceLocator(serviceRegistrar)
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

            assertTrue(callbackCompleted.await(1, TimeUnit.SECONDS))
            verify(exactly = 0) { lateHook.onPurchasesConfigured(any()) }

            locator.initialize(purchases)

            verify(exactly = 1) { lateHook.onPurchasesConfigured(purchases) }
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `replayed configured callback that registers a hook does not register locator twice`() {
        val purchases = mockk<Purchases>(relaxed = true)
        lateinit var locator: RewardVerificationServiceLocator
        var registerCount = 0
        val serviceRegistrar = RewardVerificationServiceRegistrar { listener ->
            registerCount += 1
            listener.initialize(purchases)
        }
        locator = RewardVerificationServiceLocator(serviceRegistrar)
        val lateHook = mockk<RewardVerificationLifecycleHook>(relaxed = true)
        var hookCallbackCount = 0
        var lateHookRegistered = false

        val reentrantHook = object : RewardVerificationLifecycleHook {
            override fun onPurchasesConfigured(purchases: Purchases) {
                hookCallbackCount += 1
                if (!lateHookRegistered) {
                    lateHookRegistered = true
                    locator.registerHook(lateHook)
                }
            }

            override fun onPurchasesClosed(purchases: Purchases) = Unit
        }

        locator.registerHook(reentrantHook)

        assertEquals(1, registerCount)
        assertEquals(1, hookCallbackCount)
        verify(exactly = 0) { lateHook.onPurchasesConfigured(any()) }
    }
}
