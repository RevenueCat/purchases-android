// Created by Monika on 2026-10-02

@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.customercenter

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CreateSupportTicketResult
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.PurchaseResult
import com.revenuecat.purchases.common.events.FeatureEvent
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import java.net.URL
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomerCenterPreviewPurchasesTest {
    @Test
    fun `preview loads scenario without a configured Purchases singleton`(): Unit = runBlocking {
        val provider = mockk<CustomerCenterPreviewProvider>()
        val customer = mockk<CustomerInfo>()
        val config = mockk<CustomerCenterConfigData>()
        coEvery { provider.customerInfo() } returns customer
        coEvery { provider.configuration() } returns config
        val purchases = CustomerCenterPreviewPurchases(provider)

        assertThat(purchases.awaitCustomerInfo(CacheFetchPolicy.FETCH_CURRENT)).isSameAs(customer)
        assertThat(purchases.awaitCustomerCenterConfigData()).isSameAs(config)
        assertThat(purchases.awaitSyncPurchases()).isSameAs(customer)
        coVerify(exactly = 2) { provider.customerInfo() }
    }

    @Test
    fun `purchase restore and support submissions delegate only to simulated operations`(): Unit = runBlocking {
        val provider = mockk<CustomerCenterPreviewProvider>()
        val params = mockk<PurchaseParams.Builder>()
        val purchaseResult = mockk<PurchaseResult>()
        val customer = mockk<CustomerInfo>()
        val ticket = mockk<CreateSupportTicketResult>()
        coEvery { provider.purchase(params) } returns purchaseResult
        coEvery { provider.restorePurchases() } returns customer
        coEvery { provider.createSupportTicket("email", "description") } returns ticket
        val purchases = CustomerCenterPreviewPurchases(provider)

        assertThat(purchases.awaitPurchase(params)).isSameAs(purchaseResult)
        assertThat(purchases.awaitRestore()).isSameAs(customer)
        assertThat(purchases.awaitCreateSupportTicket("email", "description")).isSameAs(ticket)
        coVerify(exactly = 1) { provider.purchase(params) }
        coVerify(exactly = 1) { provider.restorePurchases() }
        coVerify(exactly = 1) { provider.createSupportTicket("email", "description") }
    }

    @Test
    fun `preview videos resolve remote assets without a Purchases singleton`(): Unit = runBlocking {
        val purchases = CustomerCenterPreviewPurchases(mockk())
        val url = URL("https://example.com/preview.mp4")
        assertThat(purchases.fileRepositoryOverride.getFile(url)).isNull()
        assertThat(purchases.fileRepositoryOverride.generateOrGetCachedFileURL(url)).isEqualTo(url.toURI())
    }

    @Test
    fun `preview suppresses SDK feature events and cache invalidation`() {
        val provider = mockk<CustomerCenterPreviewProvider>()
        val purchases = CustomerCenterPreviewPurchases(provider)
        purchases.track(mockk<FeatureEvent>())
        purchases.invalidateVirtualCurrenciesCache()
        verify { provider wasNot Called }
    }
}
