package com.revenuecat.purchases

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.caching.DeviceCache
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BillingFactoryTest {

    @Test
    fun `BillingWrapper can be created`() {
        val mockApplication = mockk<Application>(relaxed = true)
        val mockBackend = mockk<Backend>(relaxed = true)
        val mockCache = mockk<DeviceCache>(relaxed = true)

        BillingFactory.createBilling(Store.PLAY_STORE, mockApplication, mockBackend, mockCache, false)
    }

}
