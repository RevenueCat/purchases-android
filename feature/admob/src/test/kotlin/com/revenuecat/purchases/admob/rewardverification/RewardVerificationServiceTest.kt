package com.revenuecat.purchases.admob.rewardverification

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesService
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.ServiceLoader

@OptIn(InternalRevenueCatAPI::class)
@RunWith(RobolectricTestRunner::class)
internal class RewardVerificationServiceTest {

    /**
     * Guards the META-INF/services descriptor against the class being moved or renamed: ServiceLoader must
     * still discover and instantiate [RewardVerificationService] as a [PurchasesService].
     */
    @Test
    fun `is discoverable as a PurchasesService via ServiceLoader`() {
        val services = ServiceLoader.load(
            PurchasesService::class.java,
            RewardVerificationService::class.java.classLoader,
        ).toList()

        assertTrue(services.any { it is RewardVerificationService })
    }
}
