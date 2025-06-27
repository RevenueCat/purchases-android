package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfos
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.OwnershipType
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.PurchaseResult
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.SubscriptionInfo
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.SharedConstants
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.HelpPath
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.Screen
import com.revenuecat.purchases.customercenter.CustomerCenterListener
import com.revenuecat.purchases.customercenter.CustomerCenterManagementOption
import com.revenuecat.purchases.models.GoogleSubscriptionOption
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.SubscriptionOptions
import com.revenuecat.purchases.models.Transaction
import com.revenuecat.purchases.ui.revenuecatui.customercenter.dialogs.RestorePurchasesState
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.CustomerCenterViewModelImpl
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallStateLoadedComponentsLocaleTests.Args
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.createGoogleStoreProduct
import com.revenuecat.purchases.ui.revenuecatui.helpers.stubGoogleSubscriptionOption
import com.revenuecat.purchases.ui.revenuecatui.helpers.stubPricingPhase
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Date
import java.util.Locale

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
                    ),
                    HelpPath(
                        id = "id3",
                        title = "title3",
                        type = HelpPath.PathType.CUSTOM_URL,
                        url = "https://example.com"
                    ),
                    HelpPath(
                        id = "id4",
                        title = "title1",
                        type = HelpPath.PathType.CHANGE_PLANS
                    ),
                    HelpPath(
                        id = "id4",
                        title = "title1",
                        type = HelpPath.PathType.REFUND_REQUEST
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
    fun `loadAndDisplayPromotionalOffer returns false when offer is not eligible`() = runTest {
        setupPurchasesMock()

        val model = setupViewModel()

        // Mock the product
        val product = mockk<StoreProduct>(relaxed = true)
        every { product.id } returns "product_id"

        // Mock empty SubscriptionOptions
        val emptySubscriptionOptions = mockk<SubscriptionOptions>()
        every { emptySubscriptionOptions.iterator() } returns emptyList<SubscriptionOption>().iterator()
        every { product.subscriptionOptions } returns emptySubscriptionOptions

        val ineligibleOffer = createPromotionalOffer(
            eligible = false,
            productMapping = mapOf("product_id" to "test_offer_id")
        )

        val originalPath = createOriginalPath()

        setupSuccessLoadScreen(originalPath, model)

        val result = model.loadAndDisplayPromotionalOffer(
            context = mockk(relaxed = true),
            product = product,
            promotionalOffer = ineligibleOffer,
            originalPath = originalPath
        )

        verifyPromotionalOfferResult(
            result = result,
            model = model,
            expectedResult = false
        )
    }

    @Test
    fun `loadAndDisplayPromotionalOffer works if legacy product mapping`() = runTest {
        setupPurchasesMock()

        val model = setupViewModel()

        val subscriptionOption = createSubscriptionOption(
            productId = "paywall_tester.subs",
            basePlanId = "monthly",
            offerId = "rc-cancel-offer",
            firstPhasePrice = 7.99,
            secondPhasePrice = 9.99
        )

        val product = createGoogleStoreProduct(
            productId = "paywall_tester.subs",
            basePlanId = "monthly",
            subscriptionOptions = listOf(subscriptionOption)
        )

        val promotionalOffer = createPromotionalOffer(
            productMapping = mapOf("paywall_tester.subs:monthly" to "rc-cancel-offer")
        )

        val originalPath = createOriginalPath()

        setupSuccessLoadScreen(originalPath, model)

        val result = model.loadAndDisplayPromotionalOffer(
            context = mockk(relaxed = true),
            product = product,
            promotionalOffer = promotionalOffer,
            originalPath = originalPath
        )

        verifyPromotionalOfferResult(
            result = result,
            model = model,
            expectedResult = true,
            expectedPromotionalOffer = promotionalOffer,
            expectedSubscriptionOption = subscriptionOption,
            expectedOriginalPath = originalPath,
            expectedPricingDescription = "1 month for $7.99, then $9.99/mth"
        )
    }

    @Test
    fun `loadAndDisplayPromotionalOffer works if legacy product mapping trial`() = runTest {
        setupPurchasesMock()

        val model = setupViewModel()

        val subscriptionOption = createSubscriptionOption(
            productId = "paywall_tester.subs",
            basePlanId = "monthly",
            offerId = "rc-cancel-offer"
        )

        val product = createGoogleStoreProduct(
            productId = "paywall_tester.subs",
            basePlanId = "monthly",
            subscriptionOptions = listOf(subscriptionOption)
        )

        val promotionalOffer = createPromotionalOffer(
            productMapping = mapOf("paywall_tester.subs:monthly" to "rc-cancel-offer"),
            crossProductPromotions = emptyMap()
        )

        val originalPath = createOriginalPath()

        setupSuccessLoadScreen(originalPath, model)

        val result = model.loadAndDisplayPromotionalOffer(
            context = mockk(relaxed = true),
            product = product,
            promotionalOffer = promotionalOffer,
            originalPath = originalPath
        )

        verifyPromotionalOfferResult(
            result = result,
            model = model,
            expectedResult = true,
            expectedPromotionalOffer = promotionalOffer,
            expectedSubscriptionOption = subscriptionOption,
            expectedOriginalPath = originalPath,
            expectedPricingDescription = "First 1 month free, then $9.99/mth"
        )
    }

    @Test
    fun `loadAndDisplayPromotionalOffer returns false when no matching offer found in legacy product mapping`() = runTest {
        setupPurchasesMock()

        val model = setupViewModel()

        val subscriptionOption = createSubscriptionOption(
            productId = "paywall_tester.subs",
            basePlanId = "monthly",
            offerId = "other-offer",
            firstPhasePrice = 7.99
        )

        val product = createGoogleStoreProduct(
            productId = "paywall_tester.subs",
            basePlanId = "monthly",
            subscriptionOptions = listOf(subscriptionOption)
        )

        val promotionalOffer = createPromotionalOffer(
            productMapping = mapOf("product_id:base_plan_id" to "test_offer_id"),
            crossProductPromotions = emptyMap()
        )

        val originalPath = createOriginalPath()

        setupSuccessLoadScreen(originalPath, model)

        val result = model.loadAndDisplayPromotionalOffer(
            context = mockk(relaxed = true),
            product = product,
            promotionalOffer = promotionalOffer,
            originalPath = originalPath
        )

        verifyPromotionalOfferResult(
            result = result,
            model = model,
            expectedResult = false
        )
    }

    @Test
    fun `loadAndDisplayPromotionalOffer returns true for cross-product promotion`() = runTest {
        setupPurchasesMock()

        val model = setupViewModel()

        val monthlySubscriptionOption = createSubscriptionOption(
            productId = "paywall_tester.subs",
            basePlanId = "monthly",
            offerId = "rc-cancel-offer"
        )

        val annualSubscriptionOption = createSubscriptionOption(
            productId = "paywall_tester.subs",
            basePlanId = "annual",
            offerId = "rc-cancel-offer",
            secondPhasePeriod = Period(value = 1, unit = Period.Unit.YEAR, "P1Y")
        )

        val crossProductPromotion = HelpPath.PathDetail.PromotionalOffer.CrossProductPromotion(
            storeOfferIdentifier = "rc-cancel-offer",
            targetProductId = "paywall_tester.subs:annual"
        )

        val productMonthly = createGoogleStoreProduct(
            productId = "paywall_tester.subs",
            basePlanId = "monthly",
            subscriptionOptions = listOf(monthlySubscriptionOption)
        )
        val productAnnual = createGoogleStoreProduct(
            productId = "paywall_tester.subs",
            basePlanId = "annual",
            subscriptionOptions = listOf(annualSubscriptionOption)
        )
        coEvery { purchases.awaitGetProduct("paywall_tester.subs", "annual") } returns productAnnual

        val promotionalOffer = createPromotionalOffer(
            productMapping = emptyMap(),
            crossProductPromotions = mapOf("paywall_tester.subs:monthly" to crossProductPromotion)
        )

        val originalPath = createOriginalPath()

        setupSuccessLoadScreen(originalPath, model)

        val result = model.loadAndDisplayPromotionalOffer(
            context = mockk(relaxed = true),
            product = productMonthly,
            promotionalOffer = promotionalOffer,
            originalPath = originalPath
        )

        verifyPromotionalOfferResult(
            result = result,
            model = model,
            expectedResult = true,
            expectedPromotionalOffer = promotionalOffer,
            expectedSubscriptionOption = annualSubscriptionOption,
            expectedOriginalPath = originalPath,
            expectedPricingDescription = "First 1 month free, then $9.99/yr"
        )
    }

    @Test
    fun `loadAndDisplayPromotionalOffer returns false when target product not found in cross-product promotion`() = runTest {
        setupPurchasesMock()

        val model = setupViewModel()

        val monthlySubscriptionOption = createSubscriptionOption(
            productId = "paywall_tester.subs",
            basePlanId = "monthly",
            offerId = "rc-cancel-offer"
        )

        val crossProductPromotion = HelpPath.PathDetail.PromotionalOffer.CrossProductPromotion(
            storeOfferIdentifier = "rc-cancel-offer",
            targetProductId = "paywall_tester.subs:annual"
        )

        val productMonthly = createGoogleStoreProduct(
            productId = "paywall_tester.subs",
            basePlanId = "monthly",
            subscriptionOptions = listOf(monthlySubscriptionOption)
        )

        coEvery { purchases.awaitGetProduct("paywall_tester.subs", "annual") } returns null

        val promotionalOffer = createPromotionalOffer(
            productMapping = emptyMap(),
            crossProductPromotions = mapOf("paywall_tester.subs:monthly" to crossProductPromotion)
        )

        val originalPath = createOriginalPath()

        setupSuccessLoadScreen(originalPath, model)

        val result = model.loadAndDisplayPromotionalOffer(
            context = mockk(relaxed = true),
            product = productMonthly,
            promotionalOffer = promotionalOffer,
            originalPath = originalPath
        )

        verifyPromotionalOfferResult(
            result = result,
            model = model,
            expectedResult = false
        )
    }

    @Test
    fun `loadAndDisplayPromotionalOffer returns false when no matching cross-product promotion found`() = runTest {
        setupPurchasesMock()

        val model = setupViewModel()

        val monthlySubscriptionOption = createSubscriptionOption(
            productId = "paywall_tester.subs",
            basePlanId = "monthly",
            offerId = "rc-cancel-offer"
        )

        val annualSubscriptionOption = createSubscriptionOption(
            productId = "paywall_tester.subs",
            basePlanId = "annual",
            offerId = "rc-cancel-offer",
            secondPhasePeriod = Period(value = 1, unit = Period.Unit.YEAR, "P1Y")
        )

        val crossProductPromotion = HelpPath.PathDetail.PromotionalOffer.CrossProductPromotion(
            storeOfferIdentifier = "rc-not-found",
            targetProductId = "paywall_tester.subs:annual"
        )

        val productMonthly = createGoogleStoreProduct(
            productId = "paywall_tester.subs",
            basePlanId = "monthly",
            subscriptionOptions = listOf(monthlySubscriptionOption)
        )
        val productAnnual = createGoogleStoreProduct(
            productId = "paywall_tester.subs",
            basePlanId = "annual",
            subscriptionOptions = listOf(annualSubscriptionOption)
        )
        coEvery { purchases.awaitGetProduct("paywall_tester.subs", "annual") } returns productAnnual

        val promotionalOffer = createPromotionalOffer(
            productMapping = emptyMap(),
            crossProductPromotions = mapOf("paywall_tester.subs:monthly" to crossProductPromotion)
        )

        val originalPath = createOriginalPath()

        setupSuccessLoadScreen(originalPath, model)

        val result = model.loadAndDisplayPromotionalOffer(
            context = mockk(relaxed = true),
            product = productMonthly,
            promotionalOffer = promotionalOffer,
            originalPath = originalPath
        )

        verifyPromotionalOfferResult(
            result = result,
            model = model,
            expectedResult = false
        )
    }

    @Test
    fun `loadAndDisplayPromotionalOffer handles target product without base plan`() = runTest {
        setupPurchasesMock()

        val model = setupViewModel()

        val monthlySubscriptionOption = createSubscriptionOption(
            productId = "paywall_tester.subs",
            basePlanId = "monthly",
            offerId = "rc-cancel-offer"
        )

        val annualSubscriptionOption = createSubscriptionOption(
            productId = "old_product",
            basePlanId = "p1m",
            offerId = "rc-cancel-offer",
            secondPhasePeriod = Period(value = 1, unit = Period.Unit.YEAR, "P1Y")
        )

        val crossProductPromotion = HelpPath.PathDetail.PromotionalOffer.CrossProductPromotion(
            storeOfferIdentifier = "rc-cancel-offer",
            targetProductId = "old_product"
        )

        val productMonthly = createGoogleStoreProduct(
            productId = "paywall_tester.subs",
            basePlanId = "monthly",
            subscriptionOptions = listOf(monthlySubscriptionOption)
        )
        val productAnnual = createGoogleStoreProduct(
            productId = "old_product",
            basePlanId = "p1m",
            subscriptionOptions = listOf(annualSubscriptionOption)
        )
        coEvery { purchases.awaitGetProduct("old_product", null) } returns productAnnual

        val promotionalOffer = createPromotionalOffer(
            productMapping = emptyMap(),
            crossProductPromotions = mapOf("paywall_tester.subs:monthly" to crossProductPromotion)
        )

        val originalPath = createOriginalPath()

        setupSuccessLoadScreen(originalPath, model)

        val result = model.loadAndDisplayPromotionalOffer(
            context = mockk(relaxed = true),
            product = productMonthly,
            promotionalOffer = promotionalOffer,
            originalPath = originalPath
        )

        verifyPromotionalOfferResult(
            result = result,
            model = model,
            expectedResult = true,
            expectedPromotionalOffer = promotionalOffer,
            expectedSubscriptionOption = annualSubscriptionOption,
            expectedOriginalPath = originalPath,
            expectedPricingDescription = "First 1 month free, then $9.99/yr"
        )
    }

    @Test
    fun `onNavigationButtonPressed handles CLOSE and BACK buttons correctly`() = runTest {
        setupPurchasesMock()

        every { customerInfo.activeSubscriptions } returns setOf("product-id")

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
        setupPurchasesMock()
        every { customerInfo.activeSubscriptions } returns setOf("product-id")

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
                    if (state.restorePurchasesState != null && !dialogShownCompleted.isCompleted) {
                        dialogShownCompleted.complete(true)
                    }

                    // Track reload completion
                    if (restoreCompletedWithSuccess.isCompleted
                        && initialLoadingCompleted.isCompleted
                        && !reloadingCompleted.isCompleted) {
                        reloadingCompleted.complete(true)
                    }

                    // Track when restore completes successfully
                    if (state.restorePurchasesState != null &&
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
            productIdentifier = "productIdentifier",
            purchaseDate = Date(),
            originalPurchaseDate = null,
            expiresDate = null,
            store = Store.PLAY_STORE,
            unsubscribeDetectedAt = null,
            isSandbox = false,
            billingIssuesDetectedAt = null,
            gracePeriodExpiresDate = null,
            ownershipType = OwnershipType.PURCHASED,
            periodType = PeriodType.NORMAL,
            refundedAt = null,
            storeTransactionId = null,
            requestDate = Date(),
            autoResumeDate = null,
            displayName = null,
            price = null,
            productPlanIdentifier = "monthly",
            managementURL = Uri.parse("https://example.com/manage"),
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
            CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing
        )

        // Then both listeners should be notified
        verify(exactly = 1) { directListener.onShowingManageSubscriptions() }
        verify(exactly = 1) { purchasesListener.onShowingManageSubscriptions() }
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

        feedbackState?.feedbackSurveyData?.onAnswerSubmitted?.invoke(feedbackSurveyOption)

        // Verify both listeners were called with the correct ID
        verify(exactly = 1) { directListener.onFeedbackSurveyCompleted(feedbackSurveyOptionId) }
        verify(exactly = 1) { purchasesListener.onFeedbackSurveyCompleted(feedbackSurveyOptionId) }
    }

    @Test
    fun `notifyListenersForManagementOptionSelected converts paths to actions and notifies listeners`() = runTest {
        setupPurchasesMock()

        val context = mockk<Context>(relaxed = true)
        every { context.packageName } returns "com.example.app"
        every { context.startActivity(any()) } just Runs

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

        // Test MISSING_PURCHASE path
        val missingPurchasePath = HelpPath(
            id = "missing_purchase_id",
            title = "Missing Purchase",
            type = HelpPath.PathType.MISSING_PURCHASE
        )
        model.pathButtonPressed(mockk(), missingPurchasePath, null)
        verify(exactly = 1) {
            directListener.onManagementOptionSelected(CustomerCenterManagementOption.MissingPurchase)
        }
        verify(exactly = 1) {
            purchasesListener.onManagementOptionSelected(CustomerCenterManagementOption.MissingPurchase)
        }

        // Test CANCEL path
        val cancelPath = HelpPath(
            id = "cancel_id",
            title = "Cancel",
            type = HelpPath.PathType.CANCEL
        )
        model.pathButtonPressed(mockk(), cancelPath, null)
        verify(exactly = 1) { directListener.onManagementOptionSelected(CustomerCenterManagementOption.Cancel) }
        verify(exactly = 1) { purchasesListener.onManagementOptionSelected(CustomerCenterManagementOption.Cancel) }

        // Test CUSTOM_URL path
        val customUrl = "https://example.com"
        val customUrlPath = HelpPath(
            id = "custom_url_id",
            title = "Custom URL",
            type = HelpPath.PathType.CUSTOM_URL,
            url = customUrl
        )

        model.pathButtonPressed(context, customUrlPath, null)
        verify(exactly = 1) { 
            directListener.onManagementOptionSelected(match { 
                it is CustomerCenterManagementOption.CustomUrl && 
                it.uri.toString() == customUrl 
            })
        }
        verify(exactly = 1) { 
            purchasesListener.onManagementOptionSelected(match { 
                it is CustomerCenterManagementOption.CustomUrl && 
                it.uri.toString() == customUrl 
            })
        }

        // Test unsupported path type
        val unsupportedPath = HelpPath(
            id = "unsupported_id",
            title = "Unsupported",
            type = HelpPath.PathType.REFUND_REQUEST
        )
        model.pathButtonPressed(context, unsupportedPath, null)
        verify(exactly = 3) { directListener.onManagementOptionSelected(any()) }
        verify(exactly = 3) { purchasesListener.onManagementOptionSelected(any()) }
    }

    @Test
    fun `loadCustomerCenter is called after successful promotional offer purchase`() = runTest {
        setupPurchasesMock()

        val model = CustomerCenterViewModelImpl(
            purchases = purchases,
            locale = Locale.US,
            colorScheme = TestData.Constants.currentColorScheme,
            isDarkMode = false
        )

        // Create a mock subscription option
        val subscriptionOption = mockk<SubscriptionOption>(relaxed = true)
        val activity = mockk<Activity>(relaxed = true)

        // Wait for initial load to complete
        model.state.first { it is CustomerCenterState.Success }

        // Perform promotional offer purchase
        model.onAcceptedPromotionalOffer(subscriptionOption, activity)

        // Verify the purchase was attempted and loadCustomerCenter was called
        coVerify(exactly = 1) { purchases.awaitPurchase(any()) }
        coVerify(exactly = 2) { purchases.awaitCustomerCenterConfigData() } // Once for initial load, once for reload
    }

    @Test
    fun `loadCustomerCenter uses base plan from active subscription when entitlements are empty`() = runTest {
        setupPurchasesMock()

        val subscription = SubscriptionInfo(
            productIdentifier = "product_identifier",
            purchaseDate = Date(),
            originalPurchaseDate = null,
            expiresDate = null,
            store = Store.PLAY_STORE,
            isSandbox = false,
            unsubscribeDetectedAt = null,
            billingIssuesDetectedAt = null,
            gracePeriodExpiresDate = null,
            ownershipType = OwnershipType.PURCHASED,
            periodType = PeriodType.NORMAL,
            refundedAt = null,
            storeTransactionId = null,
            autoResumeDate = null,
            displayName = null,
            price = null,
            productPlanIdentifier = "monthly",
            managementURL = Uri.parse("https://example.com/manage"),
            requestDate = Date(),
        )

        every { customerInfo.subscriptionsByProductIdentifier } returns mapOf(
            "product_identifier" to subscription
        )
        every { customerInfo.activeSubscriptions } returns setOf("product_identifier")

        val product = createGoogleStoreProduct(
            productId = "product_identifier",
            basePlanId = "monthly",
        )
        coEvery { purchases.awaitGetProduct("product_identifier", "monthly") } returns product

        val model = CustomerCenterViewModelImpl(
            purchases = purchases,
            locale = Locale.US,
            colorScheme = TestData.Constants.currentColorScheme,
            isDarkMode = false,
        )

        model.state.first { it is CustomerCenterState.Success }

        coVerify(exactly = 1) { purchases.awaitGetProduct("product_identifier", "monthly") }
    }

    @Test
    fun `isSupportedPaths allows CANCEL when purchase is not lifetime`() = runTest {
        setupPurchasesMock()
        every { customerInfo.activeSubscriptions } returns setOf(TestData.Packages.monthly.product.id)
        every { customerInfo.subscriptionsByProductIdentifier } returns mapOf(
            "productIdentifier" to SubscriptionInfo(
                productIdentifier = "productIdentifier",
                purchaseDate = Date(),
                originalPurchaseDate = null,
                expiresDate = null,
                store = Store.PLAY_STORE,
                unsubscribeDetectedAt = null,
                isSandbox = false,
                billingIssuesDetectedAt = null,
                gracePeriodExpiresDate = null,
                ownershipType = OwnershipType.PURCHASED,
                periodType = PeriodType.NORMAL,
                refundedAt = null,
                storeTransactionId = null,
                requestDate = Date(),
                autoResumeDate = null,
                displayName = null,
                price = null,
                productPlanIdentifier = "monthly",
                managementURL = Uri.parse("https://example.com/manage"),
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
                    assertThat(paths)
                        .withFailMessage("Expected CANCEL path to be present for non-lifetime purchases. Paths: $paths")
                        .anyMatch { it.type == HelpPath.PathType.CANCEL }
                    cancel()
                }
            }
        }

        job.join()
    }

    @Test
    fun `isSupportedPaths filters CANCEL when management URL is not present and not Google Play Store`() = runTest {
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
                ownershipType = OwnershipType.PURCHASED,
                periodType = PeriodType.NORMAL,
                refundedAt = null,
                storeTransactionId = null,
                requestDate = Date(),
                autoResumeDate = null,
                displayName = null,
                price = null,
                productPlanIdentifier = "monthly",
                managementURL = Uri.parse("https://example.com/manage"),
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
                    assertThat(paths)
                        .withFailMessage(
                            "Expected CANCEL path to not be present when there are no management URL. Paths: $paths")
                        .noneMatch { it.type == HelpPath.PathType.CANCEL }
                    cancel()
                }
            }
        }

        job.join()
    }

    @Test
    fun `isSupportedPaths filters CANCEL when purchase is lifetime`() = runTest {
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
                    assertThat(paths)
                        .withFailMessage("Expected CANCEL path to not be present for lifetime purchases. Paths: $paths")
                        .noneMatch { it.type == HelpPath.PathType.CANCEL }
                    cancel()
                }
            }
        }

        job.join()
    }

    @Test
    fun `isSupportedPaths filters CANCEL when purchase is non Play Store lifetime and management URL`() = runTest {
        setupPurchasesMock()
        every { customerInfo.activeSubscriptions } returns setOf(TestData.Packages.monthly.product.id)
        every { customerInfo.managementURL } returns Uri.parse("https://apple.com/")
        every { customerInfo.nonSubscriptionTransactions } returns listOf(
            Transaction(
                transactionIdentifier = "transactionIdentifier",
                revenuecatId = "revenuecatId",
                productIdentifier = "productIdentifier",
                productId = "productId",
                purchaseDate = Date(),
                storeTransactionId = null,
                store = Store.APP_STORE
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
                    assertThat(paths)
                        .withFailMessage("Expected CANCEL path to not be present for lifetime purchases. Paths: $paths")
                        .noneMatch { it.type == HelpPath.PathType.CANCEL }
                    cancel()
                }
            }
        }

        job.join()
    }

    @Test
    fun `isSupportedPaths filters CANCEL for non-Play Store without management URL`() = runTest {
        setupPurchasesMock()
        every { customerInfo.activeSubscriptions } returns setOf(TestData.Packages.monthly.product.id)
        every { customerInfo.managementURL } returns null
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
                ownershipType = OwnershipType.PURCHASED,
                periodType = PeriodType.NORMAL,
                refundedAt = null,
                storeTransactionId = null,
                requestDate = Date(),
                autoResumeDate = null,
                displayName = null,
                price = null,
                productPlanIdentifier = "monthly",
                managementURL = Uri.parse("https://example.com/manage"),
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
                    assertThat(paths)
                        .withFailMessage("Expected CANCEL path to not be present for non-PLAY_STORE without management URL. Paths: $paths")
                        .noneMatch { it.type == HelpPath.PathType.CANCEL }
                    cancel()
                }
            }
        }

        job.join()
    }

    @Test
    fun `isSupportedPaths allows CUSTOM_URL for Play store`() = runTest {
        setupPurchasesMock()
        every { customerInfo.activeSubscriptions } returns setOf(TestData.Packages.monthly.product.id)
        every { customerInfo.subscriptionsByProductIdentifier } returns mapOf(
            "productIdentifier" to SubscriptionInfo(
                productIdentifier = "productIdentifier",
                purchaseDate = Date(),
                originalPurchaseDate = null,
                expiresDate = null,
                store = Store.PLAY_STORE,
                unsubscribeDetectedAt = null,
                isSandbox = false,
                billingIssuesDetectedAt = null,
                gracePeriodExpiresDate = null,
                ownershipType = OwnershipType.PURCHASED,
                periodType = PeriodType.NORMAL,
                refundedAt = null,
                storeTransactionId = null,
                requestDate = Date(),
                autoResumeDate = null,
                displayName = null,
                price = null,
                productPlanIdentifier = "monthly",
                managementURL = Uri.parse("https://example.com/manage"),
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
                    assertThat(paths)
                        .withFailMessage("Expected CUSTOM_URL path to be present. Paths: $paths")
                        .anyMatch { it.type == HelpPath.PathType.CUSTOM_URL }
                    cancel()
                }
            }
        }

        job.join()
    }

    @Test
    fun `isSupportedPaths allows CUSTOM_URL for App store`() = runTest {
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
                ownershipType = OwnershipType.PURCHASED,
                periodType = PeriodType.NORMAL,
                refundedAt = null,
                storeTransactionId = null,
                requestDate = Date(),
                autoResumeDate = null,
                displayName = null,
                price = null,
                productPlanIdentifier = "monthly",
                managementURL = Uri.parse("https://example.com/manage"),
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
                    assertThat(paths)
                        .withFailMessage("Expected CUSTOM_URL path to be present. Paths: $paths")
                        .anyMatch { it.type == HelpPath.PathType.CUSTOM_URL }
                    cancel()
                }
            }
        }

        job.join()
    }

    @Test
    fun `isSupportedPaths allows MISSING_PURCHASE for App store`() = runTest {
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
                ownershipType = OwnershipType.PURCHASED,
                periodType = PeriodType.NORMAL,
                refundedAt = null,
                storeTransactionId = null,
                requestDate = Date(),
                autoResumeDate = null,
                displayName = null,
                price = null,
                productPlanIdentifier = "monthly",
                managementURL = Uri.parse("https://example.com/manage"),
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
                    assertThat(paths)
                        .withFailMessage("Expected MISSING_PURCHASE path for APP_STORE. Paths: $paths")
                        .anyMatch { it.type == HelpPath.PathType.MISSING_PURCHASE }
                    cancel()
                }
            }
        }

        job.join()
    }

    @Test
    fun `isSupportedPaths allows MISSING_PURCHASE for Play store`() = runTest {
        setupPurchasesMock()
        every { customerInfo.activeSubscriptions } returns setOf(TestData.Packages.monthly.product.id)
        every { customerInfo.subscriptionsByProductIdentifier } returns mapOf(
            "productIdentifier" to SubscriptionInfo(
                productIdentifier = "productIdentifier",
                purchaseDate = Date(),
                originalPurchaseDate = null,
                expiresDate = null,
                store = Store.PLAY_STORE,
                unsubscribeDetectedAt = null,
                isSandbox = false,
                billingIssuesDetectedAt = null,
                gracePeriodExpiresDate = null,
                ownershipType = OwnershipType.PURCHASED,
                periodType = PeriodType.NORMAL,
                refundedAt = null,
                storeTransactionId = null,
                requestDate = Date(),
                autoResumeDate = null,
                displayName = null,
                price = null,
                productPlanIdentifier = "monthly",
                managementURL = Uri.parse("https://example.com/manage"),
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
                    assertThat(paths)
                        .withFailMessage("Expected MISSING_PURCHASE path for PLAY_STORE. Paths: $paths")
                        .anyMatch { it.type == HelpPath.PathType.MISSING_PURCHASE }
                    cancel()
                }
            }
        }

        job.join()
    }

    @Test
    fun `isSupportedPaths filters non compatible paths for App store`() = runTest {
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
                ownershipType = OwnershipType.PURCHASED,
                periodType = PeriodType.NORMAL,
                refundedAt = null,
                storeTransactionId = null,
                requestDate = Date(),
                autoResumeDate = null,
                displayName = null,
                price = null,
                productPlanIdentifier = "monthly",
                managementURL = Uri.parse("https://example.com/manage"),
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
                    assertThat(paths)
                        .withFailMessage("Not expected REFUND_REQUEST path for APP_STORE. Paths: $paths")
                        .noneMatch { it.type == HelpPath.PathType.REFUND_REQUEST }
                    assertThat(paths)
                        .withFailMessage("Not expected CHANGE_PLANS path for APP_STORE. Paths: $paths")
                        .noneMatch { it.type == HelpPath.PathType.CHANGE_PLANS }
                    cancel()
                }
            }
        }

        job.join()
    }

    @Test
    fun `isSupportedPaths filters non compatible paths for Play store`() = runTest {
        setupPurchasesMock()
        every { customerInfo.activeSubscriptions } returns setOf(TestData.Packages.monthly.product.id)
        every { customerInfo.subscriptionsByProductIdentifier } returns mapOf(
            "productIdentifier" to SubscriptionInfo(
                productIdentifier = "productIdentifier",
                purchaseDate = Date(),
                originalPurchaseDate = null,
                expiresDate = null,
                store = Store.PLAY_STORE,
                unsubscribeDetectedAt = null,
                isSandbox = false,
                billingIssuesDetectedAt = null,
                gracePeriodExpiresDate = null,
                ownershipType = OwnershipType.PURCHASED,
                periodType = PeriodType.NORMAL,
                refundedAt = null,
                storeTransactionId = null,
                requestDate = Date(),
                autoResumeDate = null,
                displayName = null,
                price = null,
                productPlanIdentifier = "monthly",
                managementURL = Uri.parse("https://example.com/manage"),
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
                    assertThat(paths)
                        .withFailMessage("Not expected REFUND_REQUEST path for APP_STORE. Paths: $paths")
                        .noneMatch { it.type == HelpPath.PathType.REFUND_REQUEST }
                    assertThat(paths)
                        .withFailMessage("Not expected CHANGE_PLANS path for APP_STORE. Paths: $paths")
                        .noneMatch { it.type == HelpPath.PathType.CHANGE_PLANS }
                    cancel()
                }
            }
        }

        job.join()
    }

    // Helper method to setup common mocks
    private fun setupPurchasesMock() {
        every { purchases.customerCenterListener } returns null
        coEvery { purchases.awaitGetProduct(any(), any()) } returns null
        coEvery { purchases.awaitCustomerInfo(any()) } returns customerInfo
        coEvery { purchases.awaitCustomerCenterConfigData() } returns configData
        coEvery { purchases.awaitRestore() } returns customerInfo
        coEvery { purchases.awaitPurchase(any()) } returns PurchaseResult(mockk(), customerInfo)
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
    private suspend fun TestScope.setupSuccessLoadScreen(
        originalPath: HelpPath,
        model: CustomerCenterViewModelImpl,
    ) {
        // Set up the state as Success
        val managementScreen = Screen(
            type = Screen.ScreenType.MANAGEMENT,
            title = "Management",
            subtitle = null,
            paths = listOf(originalPath)
        )

        val mockScreens = mapOf(
            Screen.ScreenType.MANAGEMENT to managementScreen
        )

        val localization = CustomerCenterConfigData.Localization(
            locale = "en",
            localizedStrings = emptyMap()
        )

        every { configData.screens } returns mockScreens
        every { configData.getManagementScreen() } returns managementScreen
        every { configData.localization } returns localization

        // Wait for initial state to load
        model.loadCustomerCenter()
        val job = launch {
            model.state.collect { state ->
                if (state is CustomerCenterState.Success) {
                    cancel()
                }
            }
        }
        job.join()
    }

    private fun createSubscriptionOption(
        productId: String,
        basePlanId: String,
        offerId: String,
        firstPhasePeriod: Period = Period(value = 1, unit = Period.Unit.MONTH, "P1M"),
        firstPhasePrice: Double = 0.0,
        secondPhasePeriod: Period = Period(value = 1, unit = Period.Unit.MONTH, "P1M"),
        secondPhasePrice: Double = 9.99,
    ): GoogleSubscriptionOption {
        return stubGoogleSubscriptionOption(
            productId = productId,
            basePlanId = basePlanId,
            productDetails = mockk(),
            offerId = offerId,
            pricingPhases = listOf(
                stubPricingPhase(
                    billingCycleCount = 1,
                    billingPeriod = firstPhasePeriod,
                    price = firstPhasePrice,
                    recurrenceMode = ProductDetails.RecurrenceMode.FINITE_RECURRING,
                ),
                stubPricingPhase(
                    billingCycleCount = 0,
                    billingPeriod = secondPhasePeriod,
                    price = secondPhasePrice,
                    recurrenceMode = ProductDetails.RecurrenceMode.INFINITE_RECURRING,
                )
            ),
            tags = listOf(SharedConstants.RC_CUSTOMER_CENTER_TAG)
        )
    }

    private fun createPromotionalOffer(
        eligible: Boolean = true,
        title: String = "Test Offer",
        subtitle: String = "Test Subtitle",
        productMapping: Map<String, String> = emptyMap(),
        crossProductPromotions: Map<String, HelpPath.PathDetail.PromotionalOffer.CrossProductPromotion> = emptyMap()
    ): HelpPath.PathDetail.PromotionalOffer {
        return HelpPath.PathDetail.PromotionalOffer(
            androidOfferId = "",
            eligible = eligible,
            title = title,
            subtitle = subtitle,
            productMapping = productMapping,
            crossProductPromotions = crossProductPromotions
        )
    }

    private fun createOriginalPath(
        id: String = "path_id",
        title: String = "Some Path",
        type: HelpPath.PathType = HelpPath.PathType.CUSTOM_URL
    ): HelpPath {
        return HelpPath(
            id = id,
            title = title,
            type = type
        )
    }

    private fun verifyPromotionalOfferResult(
        result: Boolean,
        model: CustomerCenterViewModelImpl,
        expectedResult: Boolean,
        expectedPromotionalOffer: HelpPath.PathDetail.PromotionalOffer? = null,
        expectedSubscriptionOption: GoogleSubscriptionOption? = null,
        expectedOriginalPath: HelpPath? = null,
        expectedPricingDescription: String? = null
    ) {
        assertThat(result).isEqualTo(expectedResult)

        val updatedState = model.state.value
        assertThat(updatedState).isInstanceOf(CustomerCenterState.Success::class.java)
        val successState = updatedState as CustomerCenterState.Success

        if (expectedResult) {
            assertThat(successState.promotionalOfferData).isNotNull
            successState.promotionalOfferData?.let { data ->
                assertThat(data.configuredPromotionalOffer).isEqualTo(expectedPromotionalOffer)
                assertThat(data.subscriptionOption).isEqualTo(expectedSubscriptionOption)
                assertThat(data.originalPath).isEqualTo(expectedOriginalPath)
                assertThat(data.localizedPricingPhasesDescription).isEqualTo(expectedPricingDescription)
            }
        } else {
            assertThat(successState.promotionalOfferData).isNull()
        }
    }

    private fun setupViewModel(): CustomerCenterViewModelImpl {
        return CustomerCenterViewModelImpl(
            purchases = purchases,
            locale = Locale.US,
            colorScheme = TestData.Constants.currentColorScheme,
            isDarkMode = false
        )
    }

}
