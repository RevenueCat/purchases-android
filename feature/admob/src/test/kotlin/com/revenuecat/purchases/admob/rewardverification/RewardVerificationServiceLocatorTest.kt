package com.revenuecat.purchases.admob.rewardverification

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesLifecycleEventBus
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(InternalRevenueCatAPI::class)
internal class RewardVerificationServiceLocatorTest {

    @Before
    fun setUp() {
        resetLocatorState()
    }

    @After
    fun tearDown() {
        unmockkObject(PurchasesLifecycleEventBus)
    }

    @Test
    fun `registerHook registers locator listener only once`() {
        mockkObject(PurchasesLifecycleEventBus)
        every { PurchasesLifecycleEventBus.register(any()) } just Runs
        val hookOne = mockk<RewardVerificationLifecycleHook>(relaxed = true)
        val hookTwo = mockk<RewardVerificationLifecycleHook>(relaxed = true)

        RewardVerificationServiceLocator.registerHook(hookOne)
        RewardVerificationServiceLocator.registerHook(hookTwo)

        verify(exactly = 1) {
            PurchasesLifecycleEventBus.register(RewardVerificationServiceLocator)
        }
    }

    @Test
    fun `registered hooks receive lifecycle callbacks and unregistered hooks do not`() {
        mockkObject(PurchasesLifecycleEventBus)
        every { PurchasesLifecycleEventBus.register(any()) } just Runs
        val hook = mockk<RewardVerificationLifecycleHook>(relaxed = true)
        val purchases = mockk<Purchases>(relaxed = true)

        RewardVerificationServiceLocator.registerHook(hook)
        clearMocks(hook)

        RewardVerificationServiceLocator.onPurchasesConfigured(purchases)
        RewardVerificationServiceLocator.onPurchasesClosed(purchases)

        verify(exactly = 1) {
            hook.onPurchasesConfigured(purchases)
            hook.onPurchasesClosed(purchases)
        }

        RewardVerificationServiceLocator.unregisterHook(hook)
        RewardVerificationServiceLocator.onPurchasesConfigured(purchases)
        RewardVerificationServiceLocator.onPurchasesClosed(purchases)

        verify(exactly = 1) {
            hook.onPurchasesConfigured(purchases)
            hook.onPurchasesClosed(purchases)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun resetLocatorState() {
        val locatorClass = RewardVerificationServiceLocator::class.java

        val registeredField = locatorClass.getDeclaredField("isRegistered").apply { isAccessible = true }
        registeredField.setBoolean(RewardVerificationServiceLocator, false)

        val hooksField = locatorClass.getDeclaredField("hooks").apply { isAccessible = true }
        val hooks = hooksField.get(RewardVerificationServiceLocator) as MutableSet<RewardVerificationLifecycleHook>
        hooks.clear()
    }
}
