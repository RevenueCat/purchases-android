package com.revenuecat.purchases

import android.content.Context
import android.content.pm.ApplicationInfo
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkConstructor
import io.mockk.unmockkObject
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test

internal class PurchasesLifecycleWiringCustomEntitlementTest {

    @After
    fun tearDownMocks() {
        unmockkObject(PurchasesLifecycleEventBus)
        unmockkConstructor(PurchasesFactory::class)
        Purchases.backingFieldSharedInstance = null
    }

    @Test
    fun `configure in custom entitlement mode notifies purchases lifecycle bus`() {
        val context = mockk<Context>()
        val applicationContext = mockk<Context>()
        val configuredPurchases = mockk<Purchases>(relaxed = true)
        val applicationInfo = ApplicationInfo().apply {
            flags = 0
        }
        every { context.applicationContext } returns applicationContext
        every { context.isDeviceProtectedStorage } returns false
        every { applicationContext.applicationContext } returns applicationContext
        every { applicationContext.applicationInfo } returns applicationInfo
        val configuration = PurchasesConfigurationForCustomEntitlementsComputationMode.Builder(
            context = context,
            apiKey = "api_key",
            appUserID = "app_user_id",
        ).build()

        mockkObject(PurchasesLifecycleEventBus)
        mockkConstructor(PurchasesFactory::class)
        every { PurchasesLifecycleEventBus.onConfigured(any()) } just Runs
        every { anyConstructed<PurchasesFactory>().createPurchases(any(), any(), any()) } returns configuredPurchases

        Purchases.configureInCustomEntitlementsComputationMode(configuration)

        verify(exactly = 1) {
            PurchasesLifecycleEventBus.onConfigured(configuredPurchases)
        }
        assertThat(Purchases.sharedInstance).isSameAs(configuredPurchases)
    }
}
