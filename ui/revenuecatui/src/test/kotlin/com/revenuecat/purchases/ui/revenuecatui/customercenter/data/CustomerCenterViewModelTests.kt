package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfos
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.SubscriptionInfo
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.HelpPath
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.Screen
import com.revenuecat.purchases.customercenter.CustomerCenterListener
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.Transaction
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.CustomerCenterViewModelImpl
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@RunWith(AndroidJUnit4::class)
class CustomerCenterViewModelTests {

    private lateinit var purchases: PurchasesType
    private lateinit var customerInfo: CustomerInfo
    private lateinit var configData: CustomerCenterConfigData
    private lateinit var customerCenterListener: CustomerCenterListener

    private lateinit var screens: Map<Screen.ScreenType, Screen>

    @Before
    fun setUp() {
        purchases = mockk()
        customerInfo = mockk()
        configData = mockk()
        customerCenterListener = mockk(relaxed = true)

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
        setupPurchasesMock()
        every { customerInfo.activeSubscriptions } returns setOf(TestData.Packages.monthly.product.id)
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
        setupPurchasesMock()
        every { customerInfo.activeSubscriptions } returns setOf(TestData.Packages.monthly.product.id)
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

    @Test
    fun `notifyListenersForRestoreStarted calls both listeners`() = runTest {
        setupPurchasesMock()
        
        // Create two separate listeners to verify they're both called
        val directListener = mockk<CustomerCenterListener>(relaxed = true)
        val purchasesListener = mockk<CustomerCenterListener>(relaxed = true)
        
        every { purchases.customerCenterListener } returns purchasesListener
        
        val model = CustomerCenterViewModelImpl(
            purchases = purchases,
            locale = Locale.US,
            colorScheme = TestData.Constants.currentColorScheme,
            isDarkMode = false,
            listener = directListener
        )

        // When user initiates a restore
        model.restorePurchases()

        // Then both listeners should be notified
        verify(exactly = 1) { directListener.onRestoreStarted() }
        verify(exactly = 1) { purchasesListener.onRestoreStarted() }
    }

    @Test
    fun `notifyListenersForRestoreCompleted calls both listeners with correct customer info`() = runTest {
        setupPurchasesMock()
        
        val directListener = mockk<CustomerCenterListener>(relaxed = true)
        val purchasesListener = mockk<CustomerCenterListener>(relaxed = true)
        
        every { purchases.customerCenterListener } returns purchasesListener
        
        val model = CustomerCenterViewModelImpl(
            purchases = purchases,
            locale = Locale.US,
            colorScheme = TestData.Constants.currentColorScheme,
            isDarkMode = false,
            listener = directListener
        )

        // When user successfully restores purchases
        model.restorePurchases()

        // Then both listeners should be notified with the correct customer info
        verify(exactly = 1) { directListener.onRestoreCompleted(customerInfo) }
        verify(exactly = 1) { purchasesListener.onRestoreCompleted(customerInfo) }
    }

    @Test
    fun `notifyListenersForRestoreFailed calls both listeners with correct error`() = runTest {
        setupPurchasesMock()
        
        val directListener = mockk<CustomerCenterListener>(relaxed = true)
        val purchasesListener = mockk<CustomerCenterListener>(relaxed = true)
        
        every { purchases.customerCenterListener } returns purchasesListener
        
        val error = PurchasesError(PurchasesErrorCode.NetworkError, "Network error")
        coEvery { purchases.awaitRestore() } throws PurchasesException(error)
        
        val model = CustomerCenterViewModelImpl(
            purchases = purchases,
            locale = Locale.US,
            colorScheme = TestData.Constants.currentColorScheme,
            isDarkMode = false,
            listener = directListener
        )

        // When user fails to restore purchases
        model.restorePurchases()

        // Then both listeners should be notified with the correct error
        verify(exactly = 1) { directListener.onRestoreFailed(error) }
        verify(exactly = 1) { purchasesListener.onRestoreFailed(error) }
    }

    @Test
    fun `notifyListenersForManageSubscription calls both listeners`() = runTest {
        setupPurchasesMock()
        
        val directListener = mockk<CustomerCenterListener>(relaxed = true)
        val purchasesListener = mockk<CustomerCenterListener>(relaxed = true)
        
        every { purchases.customerCenterListener } returns purchasesListener
        
        // Set up the CustomerInfo to have a subscription
        val subscription = SubscriptionInfo(
            productIdentifier = "test_product_id",
            purchaseDate = Date(),
            originalPurchaseDate = null,
            expiresDate = null,
            store = Store.PLAY_STORE,
            unsubscribeDetectedAt = null,
            isSandbox = false,
            billingIssuesDetectedAt = null,
            gracePeriodExpiresDate = null,
            periodType = PeriodType.NORMAL,
            refundedAt = null,
            storeTransactionId = null,
            requestDate = Date()
        )
        
        every { customerInfo.subscriptionsByProductIdentifier } returns mapOf("test_product_id" to subscription)
        every { customerInfo.activeSubscriptions } returns setOf("test_product_id")
        
        // Create a mock product that will be returned from awaitGetProduct
        val mockProduct = mockk<StoreProduct>(relaxed = true)
        every { mockProduct.id } returns "test_product_id"
        coEvery { purchases.awaitGetProduct(any(), any()) } returns mockProduct
        
        // Create the context mock
        val context = mockk<Context>(relaxed = true)
        every { context.packageName } returns "com.example.app"
        every { context.startActivity(any()) } just Runs
        
        val model = CustomerCenterViewModelImpl(
            purchases = purchases,
            locale = Locale.US,
            colorScheme = TestData.Constants.currentColorScheme,
            isDarkMode = false,
            listener = directListener
        )
        
        // Load the customer center to get things initialized
        model.loadCustomerCenter()
        
        // When Cancel path is triggered
        model.pathButtonPressed(
            context, 
            HelpPath(
                id = "test_id",
                title = "Cancel",
                type = HelpPath.PathType.CANCEL
            ),
            mockProduct
        )

        // Then both listeners should be notified
        verify(exactly = 1) { directListener.onManageSubscriptionRequested() }
        verify(exactly = 1) { purchasesListener.onManageSubscriptionRequested() }
    }

    @Test
    fun `feedback survey completion notifies listeners with correct ID`() = runTest {
        setupPurchasesMock()
        
        val directListener = mockk<CustomerCenterListener>(relaxed = true)
        val purchasesListener = mockk<CustomerCenterListener>(relaxed = true)
        
        every { purchases.customerCenterListener } returns purchasesListener
        
        val feedbackSurveyOptionId = "test_option_id"
        
        // Create the feedback survey option and path
        val feedbackSurveyOption = HelpPath.PathDetail.FeedbackSurvey.Option(
            id = feedbackSurveyOptionId,
            title = "Option Title",
            promotionalOffer = null
        )
        
        val feedbackSurvey = HelpPath.PathDetail.FeedbackSurvey(
            title = "Survey Title",
            options = listOf(feedbackSurveyOption)
        )
        
        val path = HelpPath(
            id = "test_path_id",
            title = "Test Path",
            type = HelpPath.PathType.CUSTOM_URL,
            url = "https://example.com",
            feedbackSurvey = feedbackSurvey
        )
        
        // Ensure we return a proper config
        val mockManagementScreen = Screen(
            type = CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT,
            title = "Management",
            subtitle = null,
            paths = listOf(path)
        )
        
        val mockScreens = mapOf(
            CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT to mockManagementScreen
        )
        
        every { configData.getManagementScreen() } returns mockManagementScreen
        every { configData.screens } returns mockScreens
        
        val model = CustomerCenterViewModelImpl(
            purchases = purchases,
            locale = Locale.US,
            colorScheme = TestData.Constants.currentColorScheme,
            isDarkMode = false,
            listener = directListener
        )
        
        // Initialize the ViewModel and wait for it to load
        model.loadCustomerCenter()
        
        // Wait for the state to be in Success state before proceeding
        var successState: CustomerCenterState.Success? = null
        val job = launch {
            model.state.collect { state ->
                if (state is CustomerCenterState.Success) {
                    successState = state
                    cancel()
                }
            }
        }
        job.join()
        
        assertThat(successState).isNotNull
        
        val context = mockk<Context>(relaxed = true)
        
        model.pathButtonPressed(context, path, null)
        
        // Wait for the state to update with feedback survey data
        var feedbackState: CustomerCenterState.Success? = null
        val feedbackJob = launch {
            model.state.collect { state ->
                if (state is CustomerCenterState.Success && state.feedbackSurveyData != null) {
                    feedbackState = state
                    cancel()
                }
            }
        }
        feedbackJob.join()
        
        // Ensure we have a state with feedback survey data
        assertThat(feedbackState).isNotNull
        assertThat(feedbackState?.feedbackSurveyData).isNotNull

        feedbackState?.feedbackSurveyData?.onOptionSelected?.invoke(feedbackSurveyOption)

        // Verify both listeners were called with the correct ID
        verify(exactly = 1) { directListener.onFeedbackSurveyCompleted(feedbackSurveyOptionId) }
        verify(exactly = 1) { purchasesListener.onFeedbackSurveyCompleted(feedbackSurveyOptionId) }
    }

    // Helper method to setup common mocks
    private fun setupPurchasesMock() {
        coEvery { purchases.awaitGetProduct(any(), any()) } returns null
        coEvery { purchases.awaitCustomerInfo(any()) } returns customerInfo
        coEvery { purchases.awaitCustomerCenterConfigData() } returns configData
        coEvery { purchases.awaitRestore() } returns customerInfo
        every { purchases.purchasesAreCompletedBy } returns PurchasesAreCompletedBy.REVENUECAT
        every { purchases.storefrontCountryCode } returns "US"
        every { purchases.track(any()) } just Runs
        every { purchases.syncPurchases() } just Runs
        
        every { configData.getManagementScreen() } returns screens[Screen.ScreenType.MANAGEMENT]
        
        every { customerInfo.managementURL } returns null
        every { customerInfo.activeSubscriptions } returns setOf()
        every { customerInfo.entitlements } returns EntitlementInfos(emptyMap(), VerificationResult.VERIFIED)
        every { customerInfo.subscriptionsByProductIdentifier } returns emptyMap()
        every { customerInfo.nonSubscriptionTransactions } returns emptyList()
    }
}
