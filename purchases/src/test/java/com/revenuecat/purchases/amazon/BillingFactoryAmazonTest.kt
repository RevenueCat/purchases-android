package com.revenuecat.purchases.amazon

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.BillingFactory
import com.revenuecat.purchases.PurchasesStateCache
import com.revenuecat.purchases.PurchasesStateProvider
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.common.BackendHelper
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BillingFactoryAmazonTest {

    @Test
    fun `AmazonBilling can be created`() {
        val mockApplication = mockk<Application>(relaxed = true)
        val mockBackendHelper = mockk<BackendHelper>(relaxed = true)
        val mockCache = mockk<DeviceCache>(relaxed = true)
        val mockDiagnosticsTracker = mockk<DiagnosticsTracker>(relaxed = true)

        BillingFactory.createBilling(
            Store.AMAZON,
            mockApplication,
            mockBackendHelper,
            mockCache,
            observerMode = false,
            mockDiagnosticsTracker,
            stateProvider = PurchasesStateCache()
        )
    }

    @Test
    fun `AmazonBilling can be created without diagnostics tracker`() {
        val mockApplication = mockk<Application>(relaxed = true)
        val mockBackendHelper = mockk<BackendHelper>(relaxed = true)
        val mockCache = mockk<DeviceCache>(relaxed = true)

        BillingFactory.createBilling(
            Store.AMAZON,
            mockApplication,
            mockBackendHelper,
            mockCache,
            observerMode = false,
            diagnosticsTrackerIfEnabled = null,
            stateProvider = PurchasesStateCache()
        )
    }
}
