package com.revenuecat.purchases

import android.content.Context
import android.content.pm.ApplicationInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

internal class PurchasesLifecycleWiringDefaultsTest {
    private lateinit var originalServiceDispatcher: PurchasesServiceDispatcher

    @Before
    fun setUp() {
        originalServiceDispatcher = Purchases.serviceDispatcher
    }

    @After
    fun tearDownMocks() {
        unmockkConstructor(PurchasesFactory::class)
        Purchases.serviceDispatcher = originalServiceDispatcher
        Purchases.backingFieldSharedInstance = null
    }

    @Test
    fun `configure notifies purchases service dispatcher in defaults path`() {
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
        val configuration = PurchasesConfiguration.Builder(context, "api_key").build()
        val serviceDispatcher = mockk<PurchasesServiceDispatcher>(relaxed = true)

        mockkConstructor(PurchasesFactory::class)
        Purchases.serviceDispatcher = serviceDispatcher
        every { anyConstructed<PurchasesFactory>().createPurchases(any(), any(), any()) } returns configuredPurchases

        Purchases.configure(configuration)

        verify(exactly = 1) {
            serviceDispatcher.initialize(configuredPurchases)
        }
        assertThat(Purchases.sharedInstance).isSameAs(configuredPurchases)
    }
}
