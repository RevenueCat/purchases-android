package com.revenuecat.purchases.common.offlineentitlements

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ibm.icu.impl.Assert.fail
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.OwnershipType
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.utils.dummyGoogleRestoredStoreTransaction
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date
import org.assertj.core.api.Assertions.assertThat

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class CustomerInfoResponseHandlerTest {
    private val testDate = Date(1680048626287L) // Tue Mar 28 17:10:26 PDT 2023
    private val tomorrowDate = Date(1680135026287L) // Tue Mar 29 17:10:26 PDT 2023
    private val appUserID = "appUserID"

    private lateinit var billing: BillingAbstract
    private lateinit var testDateProvider: DateProvider

    private lateinit var customerInfoResponseHandler: CustomerInfoResponseHandler

    @Before
    fun setUp() {
        billing = mockk()
        testDateProvider = object : DateProvider {
            override val now: Date
                get() = testDate
        }
        customerInfoResponseHandler = CustomerInfoResponseHandler(billing, testDateProvider)
    }

    @Test
    fun `simple customer info`() {
        val entitlementID = "pro_1"
        val purchasedProduct = mockPurchasedProduct(
            entitlementID = entitlementID,
            expirationDate = tomorrowDate
        )

        var receivedCustomerInfo: CustomerInfo? = null
        customerInfoResponseHandler.computeOfflineCustomerInfo(
            appUserID,
            { receivedCustomerInfo = it },
            { fail("Should've succeeded") }
        )
        assertThat(receivedCustomerInfo).isNotNull
        assertThat(receivedCustomerInfo?.activeSubscriptions).isEqualTo(setOf(purchasedProduct.productIdentifier))
        assertThat(receivedCustomerInfo?.entitlements?.all?.size).isEqualTo(1)

        val receivedEntitlement = receivedCustomerInfo?.entitlements?.get(entitlementID)
        assertThat(receivedEntitlement?.isActive).isTrue
        assertThat(receivedEntitlement?.identifier).isEqualTo(entitlementID)
        assertThat(receivedEntitlement?.productIdentifier).isEqualTo(purchasedProduct.productIdentifier)
        assertThat(receivedEntitlement?.billingIssueDetectedAt).isNull()
        assertThat(receivedEntitlement?.expirationDate).isEqualTo(tomorrowDate)
        assertThat(receivedEntitlement?.isSandbox).isFalse
        assertThat(receivedEntitlement?.originalPurchaseDate).isEqualTo(testDate)
        assertThat(receivedEntitlement?.latestPurchaseDate).isEqualTo(testDate)
        assertThat(receivedEntitlement?.ownershipType).isEqualTo(OwnershipType.UNKNOWN)
        assertThat(receivedEntitlement?.periodType).isEqualTo(PeriodType.NORMAL)
        assertThat(receivedEntitlement?.store).isEqualTo(Store.PLAY_STORE)
        assertThat(receivedEntitlement?.unsubscribeDetectedAt).isNull()
    }

    private fun mockPurchasedProduct(
        productIdentifier: String = "product_identifier",
        entitlementID: String = "pro_1",
        purchaseDate: Date = testDate,
        expirationDate: Date = tomorrowDate
    ): PurchasedProduct {
        val storeTransaction = dummyGoogleRestoredStoreTransaction(
            purchaseDate = testDate
        )
        val purchasedProduct = PurchasedProduct(
            productIdentifier,
            storeTransaction,
            true,
            listOf(entitlementID),
            expiresDate = expirationDate
        )
        every {
            billing.queryPurchasedProducts(
                appUserID,
                captureLambda(),
                any()
            )
        } answers {
            lambda<(List<PurchasedProduct>) -> Unit>().captured.invoke(listOf(purchasedProduct))
        }
        return purchasedProduct
    }
}