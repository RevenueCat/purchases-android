package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfos
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.OwnershipType
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.SubscriptionInfo
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.HelpPath
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.HelpPath.PathDetail
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.HelpPath.PathType
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.Localization
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.Screen
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.Support
import com.revenuecat.purchases.models.Transaction
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.CustomerCenterViewModel
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.CustomerCenterViewModelImpl
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesImpl
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.utils.DateFormatter
import com.revenuecat.purchases.ui.revenuecatui.utils.DefaultDateFormatter
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import junit.framework.TestCase.fail
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@RunWith(AndroidJUnit4::class)
class CustomerCenterViewModelTests {

    private lateinit var purchases: PurchasesType
    private lateinit var customerInfo: CustomerInfo
    private lateinit var configData: CustomerCenterConfigData

    private lateinit var screens: Map<Screen.ScreenType, Screen>

    @Before
    fun setUp() {
        purchases = mockk()
        customerInfo = mockk()
        configData = mockk()

        screens = mapOf(
            Screen.ScreenType.MANAGEMENT to CustomerCenterConfigData.Screen(
                type = CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT,
                title = "title",
                subtitle = null,
                paths = listOf(
                    HelpPath(
                        id = "id1",
                        title = "title1",
                        type = HelpPath.PathType.MISSING_PURCHASE
                    ),
                    HelpPath(
                        id = "id2",
                        title = "title2",
                        type = HelpPath.PathType.CANCEL,
                    )
                )
            )
        )
    }

    @After
    internal fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `isSupportedPaths does not filter CANCEL when purchase is not lifetime`() = runTest {
        every { configData.getManagementScreen() } returns screens[Screen.ScreenType.MANAGEMENT]

        coEvery { purchases.awaitGetProduct(any(), any()) } returns null
        coEvery { purchases.awaitCustomerInfo(any()) } returns customerInfo
        coEvery { purchases.awaitCustomerCenterConfigData() } returns configData
        every { purchases.purchasesAreCompletedBy } returns PurchasesAreCompletedBy.REVENUECAT

        every { customerInfo.managementURL } returns null
        every { customerInfo.activeSubscriptions } returns setOf(TestData.Packages.monthly.product.id)
        every { customerInfo.entitlements } returns EntitlementInfos(
            emptyMap(),
            VerificationResult.VERIFIED,
        )
        every { customerInfo.subscriptionsByProductIdentifier } returns mapOf(
            "productIdentifier" to SubscriptionInfo(
                productIdentifier = "productIdentifier",
                purchaseDate = Date(),
                originalPurchaseDate = null,
                expiresDate = null,
                store = Store.APP_STORE,
                unsubscribeDetectedAt = null,
                isSandbox = false,
                billingIssuesDetectedAt = null,
                gracePeriodExpiresDate = null,
                periodType = PeriodType.NORMAL,
                refundedAt = null,
                storeTransactionId = null,
                requestDate = Date()
            )
        )

        every { customerInfo.nonSubscriptionTransactions } returns listOf()
        every { purchases.storefrontCountryCode } returns "US"
        every { purchases.track(any()) } just Runs
        every { purchases.syncPurchases() } just Runs

        val model = CustomerCenterViewModelImpl(
            purchases = purchases,
            locale = Locale.US,
            colorScheme = TestData.Constants.currentColorScheme,
            isDarkMode = false
        )

        val job = launch {
            model.state.collect { state ->
                if (state is CustomerCenterState.Success) {
                    val paths = state.supportedPathsForManagementScreen ?: emptyList()
                    assertThat(paths.size).isEqualTo(2)
                    cancel()
                }
            }
        }

        job.join()
    }

    @Test
    fun `isSupportedPaths does filter CANCEL when purchase is lifetime`() = runTest {
        every { configData.getManagementScreen() } returns screens[Screen.ScreenType.MANAGEMENT]

        coEvery { purchases.awaitGetProduct(any(), any()) } returns null
        coEvery { purchases.awaitCustomerInfo(any()) } returns customerInfo
        coEvery { purchases.awaitCustomerCenterConfigData() } returns configData
        every { purchases.purchasesAreCompletedBy } returns PurchasesAreCompletedBy.REVENUECAT

        every { customerInfo.managementURL } returns null
        every { customerInfo.activeSubscriptions } returns setOf(TestData.Packages.monthly.product.id)
        every { customerInfo.entitlements } returns EntitlementInfos(
            emptyMap(),
            VerificationResult.VERIFIED,
        )
        every { customerInfo.subscriptionsByProductIdentifier } returns emptyMap()
        every { customerInfo.nonSubscriptionTransactions } returns listOf(
            Transaction(
                transactionIdentifier = "transactionIdentifier",
                revenuecatId = "revenuecatId",
                productIdentifier = "productIdentifier",
                productId = "productId",
                purchaseDate = Date(),
                storeTransactionId = null,
                store = Store.PLAY_STORE
            )
        )

        every { purchases.storefrontCountryCode } returns "US"
        every { purchases.track(any()) } just Runs
        every { purchases.syncPurchases() } just Runs

        val model = CustomerCenterViewModelImpl(
            purchases = purchases,
            locale = Locale.US,
            colorScheme = TestData.Constants.currentColorScheme,
            isDarkMode = false
        )

        val job = launch {
            model.state.collect { state ->
                if (state is CustomerCenterState.Success) {
                    val paths = state.supportedPathsForManagementScreen ?: emptyList()
                    assertThat(paths.size).isEqualTo(1)
                    assertThat(paths.first()).isEqualTo(HelpPath(
                        id = "id1",
                        title = "title1",
                        type = HelpPath.PathType.MISSING_PURCHASE
                    ))
                    cancel()
                }
            }
        }

        job.join()
    }
}
