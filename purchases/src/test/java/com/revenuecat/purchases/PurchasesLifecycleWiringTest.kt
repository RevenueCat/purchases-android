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

internal class PurchasesLifecycleWiringTest {
    private lateinit var originalLifecycleListener: PurchasesLifecycleListener

    @Before
    fun setUp() {
        originalLifecycleListener = Purchases.lifecycleListener
    }

    @After
    fun tearDownMocks() {
        unmockkConstructor(PurchasesFactory::class)
        Purchases.lifecycleListener = originalLifecycleListener
        Purchases.backingFieldSharedInstance = null
    }

    @Test
    fun `close notifies purchases lifecycle bus`() {
        val orchestrator = mockk<PurchasesOrchestrator>(relaxed = true)
        val purchases = Purchases(orchestrator)
        val lifecycleListener = mockk<PurchasesLifecycleListener>(relaxed = true)
        Purchases.backingFieldSharedInstance = purchases
        Purchases.lifecycleListener = lifecycleListener

        purchases.close()

        verify(exactly = 1) {
            lifecycleListener.onPurchasesClosed(purchases)
            orchestrator.close()
        }
    }

    @Test
    fun `configure notifies purchases lifecycle bus in defaults path`() {
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
        val lifecycleListener = mockk<PurchasesLifecycleListener>(relaxed = true)

        mockkConstructor(PurchasesFactory::class)
        Purchases.lifecycleListener = lifecycleListener
        every { anyConstructed<PurchasesFactory>().createPurchases(any(), any(), any()) } returns configuredPurchases

        val configureMethod = Purchases.Companion::class.java.methods.firstOrNull {
            it.name == "configure" && it.parameterTypes.contentEquals(arrayOf(PurchasesConfiguration::class.java))
        } ?: error("Could not find Purchases.configure(PurchasesConfiguration) via reflection")
        configureMethod.invoke(Purchases.Companion, configuration)

        verify(exactly = 1) {
            lifecycleListener.onPurchasesConfigured(configuredPurchases)
        }
        assertThat(Purchases.sharedInstance).isSameAs(configuredPurchases)
    }
}
