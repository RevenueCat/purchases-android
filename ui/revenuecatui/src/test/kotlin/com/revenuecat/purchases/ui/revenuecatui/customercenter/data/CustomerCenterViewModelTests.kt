package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfos
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.SubscriptionInfo
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.HelpPath
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.Screen
import com.revenuecat.purchases.models.Transaction
import com.revenuecat.purchases.ui.revenuecatui.customercenter.dialogs.RestorePurchasesState
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.CustomerCenterViewModelImpl
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
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

    @Test
    fun `onNavigationButtonPressed handles CLOSE and BACK buttons correctly`() = runTest {
        // Setup basic mocks
        every { customerInfo.activeSubscriptions } returns setOf("product-id")
        every { customerInfo.nonSubscriptionTransactions } returns emptyList()
        every { customerInfo.entitlements } returns EntitlementInfos(
            emptyMap(),
            VerificationResult.VERIFIED,
        )
        every { customerInfo.subscriptionsByProductIdentifier } returns emptyMap()

        // Setup customer center data
        coEvery { purchases.awaitCustomerCenterConfigData() } returns configData
        coEvery { purchases.awaitCustomerInfo(any()) } returns customerInfo
        coEvery { purchases.awaitGetProduct(any(), any()) } returns null

        // Setup screen with a path
        val testPath = HelpPath(
            id = "test_path_id",
            title = "Test Path",
            type = HelpPath.PathType.CUSTOM_URL
        )

        val managementScreen = Screen(
            type = Screen.ScreenType.MANAGEMENT,
            title = "Management",
            subtitle = null,
            paths = listOf(testPath)
        )

        every { configData.getManagementScreen() } returns managementScreen

        // Create the ViewModel
        val model = CustomerCenterViewModelImpl(
            purchases = purchases,
            locale = Locale.US,
            colorScheme = TestData.Constants.currentColorScheme,
            isDarkMode = false
        )

        // Track state changes
        val initialLoadingCompleted = CompletableDeferred<Boolean>()
        val onDismissCalled = CompletableDeferred<Boolean>()
        val stateResetToNotLoaded = CompletableDeferred<Boolean>()
        val stateResetToMainScreen = CompletableDeferred<Boolean>()
        val onDismissShouldNotBeCalled = CompletableDeferred<Boolean>()

        val job = launch {
            model.state.collect { state ->
                when (state) {
                    is CustomerCenterState.Success -> {
                        // Track initial load completion
                        if (!initialLoadingCompleted.isCompleted) {
                            initialLoadingCompleted.complete(true)
                        }

                        // Track when state is reset to main screen (BACK button)
                        if (state.navigationButtonType == CustomerCenterState.NavigationButtonType.CLOSE &&
                            !stateResetToMainScreen.isCompleted) {
                            stateResetToMainScreen.complete(true)
                        }
                    }
                    is CustomerCenterState.NotLoaded -> {
                        // Track when state is reset to NotLoaded (CLOSE button)
                        if (onDismissCalled.isCompleted && !stateResetToNotLoaded.isCompleted) {
                            stateResetToNotLoaded.complete(true)
                        }
                    }
                    else -> {}
                }
            }
        }

        // Wait for initial setup to complete
        initialLoadingCompleted.await()

        // Test CLOSE button
        model.onNavigationButtonPressed(mockk()) {
            onDismissCalled.complete(true)
        }

        // Wait for state to be reset to NotLoaded
        stateResetToNotLoaded.await()

        // Reload the state for BACK button test
        model.loadCustomerCenter()
        initialLoadingCompleted.await()

        // Set up state for BACK button test by displaying a feedback survey
        model.pathButtonPressed(
            mockk(),
            HelpPath(
                id = "feedback_id",
                title = "Feedback",
                type = HelpPath.PathType.CUSTOM_URL,
                feedbackSurvey = HelpPath.PathDetail.FeedbackSurvey(
                    title = "Feedback",
                    options = emptyList()
                )
            ),
            null
        )

        // Test BACK button - verify onDismiss is not called
        model.onNavigationButtonPressed(mockk()) {
            onDismissShouldNotBeCalled.complete(true)
        }

        // Wait for state to be reset to main screen
        stateResetToMainScreen.await()

        // Verify the state transitions
        assertThat(onDismissCalled.isCompleted).isTrue()
        assertThat(onDismissShouldNotBeCalled.isCompleted).isFalse()
        assertThat(stateResetToNotLoaded.isCompleted).isTrue()
        assertThat(stateResetToMainScreen.isCompleted).isTrue()

        // Cancel the collection job
        job.cancel()
    }

    @Test
    fun `dismissRestoreDialog reloads customer center`() = runTest {
        // Setup basic mocks
        every { customerInfo.activeSubscriptions } returns setOf("product-id")
        every { customerInfo.nonSubscriptionTransactions } returns emptyList()
        every { customerInfo.entitlements } returns EntitlementInfos(
            emptyMap(),
            VerificationResult.VERIFIED,
        )
        every { customerInfo.subscriptionsByProductIdentifier } returns emptyMap()

        // Setup restore operation to return successful result
        coEvery { purchases.awaitRestore() } returns customerInfo

        // Setup other customer center data
        coEvery { purchases.awaitCustomerCenterConfigData() } returns configData
        coEvery { purchases.awaitCustomerInfo(any()) } returns customerInfo
        coEvery { purchases.awaitGetProduct(any(), any()) } returns null

        // Setup screen with a MISSING_PURCHASE path
        val missingPurchasePath = HelpPath(
            id = "missing_purchase_id",
            title = "Restore Purchases",
            type = HelpPath.PathType.MISSING_PURCHASE
        )

        val managementScreen = Screen(
            type = Screen.ScreenType.MANAGEMENT,
            title = "Management",
            subtitle = null,
            paths = listOf(missingPurchasePath)
        )

        every { configData.getManagementScreen() } returns managementScreen

        // Create a spy on the real ViewModel so we can verify loadCustomerCenter is called
        val model = CustomerCenterViewModelImpl(
            purchases = purchases,
            locale = Locale.US,
            colorScheme = TestData.Constants.currentColorScheme,
            isDarkMode = false
        )

        // Wait for initial state to load
        val initialLoadingCompleted = CompletableDeferred<Boolean>()
        val reloadingCompleted = CompletableDeferred<Boolean>()
        val dialogShownCompleted = CompletableDeferred<Boolean>()
        val restoreCompletedWithSuccess = CompletableDeferred<Boolean>()

        val job = launch {
            model.state.collect { state ->
                if (state is CustomerCenterState.Success) {
                    // Track initial load completion
                    if (!initialLoadingCompleted.isCompleted) {
                        initialLoadingCompleted.complete(true)
                    }

                    // Track when dialog is shown
                    if (state.showRestoreDialog && !dialogShownCompleted.isCompleted) {
                        dialogShownCompleted.complete(true)
                    }

                    // Track reload completion
                    if (restoreCompletedWithSuccess.isCompleted && initialLoadingCompleted.isCompleted && !reloadingCompleted.isCompleted) {
                        reloadingCompleted.complete(true)
                    }

                    // Track when restore completes successfully
                    if (state.showRestoreDialog &&
                        state.restorePurchasesState == RestorePurchasesState.PURCHASES_RECOVERED &&
                        !restoreCompletedWithSuccess.isCompleted) {
                        restoreCompletedWithSuccess.complete(true)
                    }

                }
            }
        }

        // Wait for initial setup to complete
        initialLoadingCompleted.await()

        // 1. First show the restore dialog by calling pathButtonPressed with MISSING_PURCHASE path
        model.pathButtonPressed(mockk(), missingPurchasePath, null)

        // Wait until dialog is shown
        dialogShownCompleted.await()

        // 2. Now perform the restore operation
        model.restorePurchases()

        // Wait until restore completes successfully
        restoreCompletedWithSuccess.await()

        // Reset mock verification counts before our actual test
        clearMocks(purchases, answers = false)

        // 3. Finally, call the method we're testing
        model.dismissRestoreDialog()

        reloadingCompleted.await()

        // Verify loadCustomerCenter was called
        coVerify (exactly = 1) { purchases.awaitCustomerCenterConfigData() }

        // Cancel the collection job
        job.cancel()
    }

}
