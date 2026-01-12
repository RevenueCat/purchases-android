package com.revenuecat.purchases.galaxy

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
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertIs

//@RunWith(AndroidJUnit4::class)
//class BillingFactoryGalaxyTest {
//    @Test
//    fun `GalaxyBillingWrapper gets created when store is Galaxy`() {
//        val mockApplication = mockk<Application>(relaxed = true)
//        val mockBackendHelper = mockk<BackendHelper>(relaxed = true)
//        val mockCache = mockk<DeviceCache>(relaxed = true)
//        val mockDiagnosticsTracker = mockk<DiagnosticsTracker>(relaxed = true)
//        val mockBackend = mockk<Backend>(relaxed = true)
//
//        val simulatedBilling = BillingFactory.createBilling(
//            Store.GALAXY,
//            mockApplication,
//            mockBackendHelper,
//            mockCache,
//            finishTransactions = true,
//            mockDiagnosticsTracker,
//            PurchasesStateCache(PurchasesState()),
//            pendingTransactionsForPrepaidPlansEnabled = true,
//            GalaxyBillingMode.TEST,
//            backend = mockBackend,
//        )
//        assertIs<GalaxyBillingWrapper>(simulatedBilling)
//    }
//
//    @Test
//    fun `GalaxyBillingWrapper gets created with GalaxyBillingMode from function params`() {
//        val mockApplication = mockk<Application>(relaxed = true)
//        val mockBackendHelper = mockk<BackendHelper>(relaxed = true)
//        val mockCache = mockk<DeviceCache>(relaxed = true)
//        val mockDiagnosticsTracker = mockk<DiagnosticsTracker>(relaxed = true)
//        val mockBackend = mockk<Backend>(relaxed = true)
//
//        val galaxyBillingWrapper = BillingFactory.createBilling(
//            Store.GALAXY,
//            mockApplication,
//            mockBackendHelper,
//            mockCache,
//            finishTransactions = true,
//            mockDiagnosticsTracker,
//            PurchasesStateCache(PurchasesState()),
//            pendingTransactionsForPrepaidPlansEnabled = true,
//            GalaxyBillingMode.TEST,
//            backend = mockBackend,
//        )
//        assertIs<GalaxyBillingWrapper>(galaxyBillingWrapper)
//        assertEquals(GalaxyBillingMode.TEST, galaxyBillingWrapper.billingMode)
//    }
//}
