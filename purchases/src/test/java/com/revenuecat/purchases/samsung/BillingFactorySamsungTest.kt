package com.revenuecat.purchases.samsung

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.BillingFactory
import com.revenuecat.purchases.PurchasesState
import com.revenuecat.purchases.PurchasesStateCache
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BackendHelper
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.samsung.SamsungBillingMode
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertIs

@RunWith(AndroidJUnit4::class)
class BillingFactorySamsungTest {
    @Test
    fun `SamsungBillingWrapper gets created when store is Samsung`() {
        val mockApplication = mockk<Application>(relaxed = true)
        val mockBackendHelper = mockk<BackendHelper>(relaxed = true)
        val mockCache = mockk<DeviceCache>(relaxed = true)
        val mockDiagnosticsTracker = mockk<DiagnosticsTracker>(relaxed = true)
        val mockBackend = mockk<Backend>(relaxed = true)

        val simulatedBilling = BillingFactory.createBilling(
            // TODO: Make this Store.Samsung after https://github.com/RevenueCat/purchases-android/pull/2900 is merged
            Store.TEST_STORE,
            mockApplication,
            mockBackendHelper,
            mockCache,
            finishTransactions = true,
            mockDiagnosticsTracker,
            PurchasesStateCache(PurchasesState()),
            pendingTransactionsForPrepaidPlansEnabled = true,
            SamsungBillingMode.TEST,
            backend = mockBackend,
        )
        assertIs<SamsungBillingWrapper>(simulatedBilling)
    }

    @Test
    fun `SamsungBillingWrapper gets created with SamsungBillingMode from function params`() {
        val mockApplication = mockk<Application>(relaxed = true)
        val mockBackendHelper = mockk<BackendHelper>(relaxed = true)
        val mockCache = mockk<DeviceCache>(relaxed = true)
        val mockDiagnosticsTracker = mockk<DiagnosticsTracker>(relaxed = true)
        val mockBackend = mockk<Backend>(relaxed = true)

        val samsungWrapper = BillingFactory.createBilling(
            // TODO: Make this Store.Samsung after https://github.com/RevenueCat/purchases-android/pull/2900 is merged
            Store.TEST_STORE,
            mockApplication,
            mockBackendHelper,
            mockCache,
            finishTransactions = true,
            mockDiagnosticsTracker,
            PurchasesStateCache(PurchasesState()),
            pendingTransactionsForPrepaidPlansEnabled = true,
            SamsungBillingMode.TEST,
            backend = mockBackend,
        )
        assertIs<SamsungBillingWrapper>(samsungWrapper)
        assertEquals(SamsungBillingMode.TEST, samsungWrapper.billingMode)
    }
}
