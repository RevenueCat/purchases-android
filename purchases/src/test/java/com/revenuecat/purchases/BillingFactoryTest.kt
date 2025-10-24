package com.revenuecat.purchases

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BackendHelper
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.simulatedstore.SimulatedStoreBillingWrapper
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertIs

@RunWith(AndroidJUnit4::class)
class BillingFactoryTest {

    @Test
    fun `BillingWrapper can be created`() {
        val mockApplication = mockk<Application>(relaxed = true)
        val mockBackendHelper = mockk<BackendHelper>(relaxed = true)
        val mockCache = mockk<DeviceCache>(relaxed = true)
        val mockDiagnosticsTracker = mockk<DiagnosticsTracker>(relaxed = true)
        val mockBackend = mockk<Backend>(relaxed = true)

        BillingFactory.createBilling(
            Store.PLAY_STORE,
            mockApplication,
            mockBackendHelper,
            mockCache,
            finishTransactions = true,
            mockDiagnosticsTracker,
            PurchasesStateCache(PurchasesState()),
            pendingTransactionsForPrepaidPlansEnabled = true,
            backend = mockBackend,
        )
    }

    @Test
    fun `SimulatedStoreBillingWrapper gets created when store is test store`() {
        val mockApplication = mockk<Application>(relaxed = true)
        val mockBackendHelper = mockk<BackendHelper>(relaxed = true)
        val mockCache = mockk<DeviceCache>(relaxed = true)
        val mockDiagnosticsTracker = mockk<DiagnosticsTracker>(relaxed = true)
        val mockBackend = mockk<Backend>(relaxed = true)

        val simulatedBilling = BillingFactory.createBilling(
            Store.TEST_STORE,
            mockApplication,
            mockBackendHelper,
            mockCache,
            finishTransactions = true,
            mockDiagnosticsTracker,
            PurchasesStateCache(PurchasesState()),
            pendingTransactionsForPrepaidPlansEnabled = true,
            backend = mockBackend,
        )
        assertIs<SimulatedStoreBillingWrapper>(simulatedBilling)
    }

    @Test
    fun `BillingWrapper can be created without diagnostics tracker`() {
        val mockApplication = mockk<Application>(relaxed = true)
        val mockBackendHelper = mockk<BackendHelper>(relaxed = true)
        val mockCache = mockk<DeviceCache>(relaxed = true)
        val mockBackend = mockk<Backend>(relaxed = true)

        BillingFactory.createBilling(
            Store.PLAY_STORE,
            mockApplication,
            mockBackendHelper,
            mockCache,
            finishTransactions = true,
            diagnosticsTrackerIfEnabled = null,
            PurchasesStateCache(PurchasesState()),
            pendingTransactionsForPrepaidPlansEnabled = true,
            backend = mockBackend,
        )
    }
}
