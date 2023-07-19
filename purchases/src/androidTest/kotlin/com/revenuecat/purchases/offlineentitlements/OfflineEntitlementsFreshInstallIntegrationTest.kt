package com.revenuecat.purchases.offlineentitlements

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.helpers.mockQueryProductDetails
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@Suppress("TooManyFunctions")
@RunWith(AndroidJUnit4::class)
class OfflineEntitlementsFreshInstallIntegrationNoPurchasesTest : BaseOfflineEntitlementsIntegrationTest() {

    override val initialForceServerErrors = true

    @Before
    fun setUp() {
        ensureBlockFinishes { latch ->
            setUpTest {
                latch.countDown()
            }
        }
    }

    @Test
    fun doesNotEnterOfflineEntitlementsModeIfNoProductEntitlementMappingAvailable() {
        ensureBlockFinishes { latch ->
            Purchases.sharedInstance.getCustomerInfoWith(
                onError = {
                    assertThat(it.code).isEqualTo(PurchasesErrorCode.UnknownBackendError)
                    latch.countDown()
                },
                onSuccess = {
                    fail("Expected error")
                },
            )
        }
    }
}

@RunWith(AndroidJUnit4::class)
class OfflineEntitlementsFreshInstallInitialPurchasesIntegrationTest : BaseOfflineEntitlementsIntegrationTest() {

    override val initialActivePurchasesToUse = initialActivePurchases
    override val initialForceServerErrors = true

    @Before
    fun setUp() {
        ensureBlockFinishes { latch ->
            setUpTest {
                mockBillingAbstract.mockQueryProductDetails()
                latch.countDown()
            }
        }
    }

    @Test
    fun doesNotEnterOfflineEntitlementsModeIfNoProductEntitlementMappingAvailable() {
        ensureBlockFinishes { latch ->
            Purchases.sharedInstance.getCustomerInfoWith(
                onError = {
                    assertThat(it.code).isEqualTo(PurchasesErrorCode.UnknownBackendError)
                    latch.countDown()
                },
                onSuccess = {
                    fail("Expected error")
                },
            )
        }
    }
}
