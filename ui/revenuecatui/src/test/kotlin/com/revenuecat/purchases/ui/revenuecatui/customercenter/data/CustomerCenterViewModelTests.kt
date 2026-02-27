package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfos
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
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.SubscriptionOptions
import com.revenuecat.purchases.models.Transaction
import com.revenuecat.purchases.ui.revenuecatui.customercenter.dialogs.RestorePurchasesState
import com.revenuecat.purchases.ui.revenuecatui.customercenter.navigation.CustomerCenterDestination
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.CustomerCenterViewModelImpl
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.createGoogleStoreProduct
import com.revenuecat.purchases.ui.revenuecatui.helpers.stubGoogleSubscriptionOption
import com.revenuecat.purchases.ui.revenuecatui.helpers.stubPricingPhase
import com.revenuecat.purchases.ui.revenuecatui.helpers.subtract
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencies
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
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import java.util.Locale
import kotlin.time.Duration

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
            ),
            Screen.ScreenType.NO_ACTIVE to CustomerCenterConfigData.Screen(
                type = CustomerCenterConfigData.Screen.ScreenType.NO_ACTIVE,
                title = "No Active Subscription",
                subtitle = "You don't have an active subscription.",
                paths = listOf(
                    HelpPath(
                        id = "restore_id",
                        title = "Restore Purchases",
                        type = HelpPath.PathType.MISSING_PURCHASE
                    ),
                    HelpPath(
                        id = "support_id",
                        title = "Contact Support",
                        type = HelpPath.PathType.CUSTOM_URL,
                        url = "https://support.example.com"
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
    fun `loadAndDisplayPromotionalOffer returns false when offer is not eligible`(): Unit = runBlocking {
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
    fun `loadAndDisplayPromotionalOffer works if legacy product mapping`(): Unit = runBlocking {
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
    fun `loadAndDisplayPromotionalOffer works if legacy product mapping trial`(): Unit = runBlocking {
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
    fun `loadAndDisplayPromotionalOffer returns false when no matching offer found in legacy product mapping`(): Unit = runBlocking {
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
    fun `loadAndDisplayPromotionalOffer returns true for cross-product promotion`(): Unit = runBlocking {
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
    fun `loadAndDisplayPromotionalOffer returns false when target product not found in cross-product promotion`(): Unit = runBlocking {
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
    fun `loadAndDisplayPromotionalOffer returns false when no matching cross-product promotion found`(): Unit = runBlocking {
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
    fun `loadAndDisplayPromotionalOffer loads target product with same base plan if not specified`(): Unit = runBlocking {
        setupPurchasesMock()

        val model = setupViewModel()
        val regularPricingPhase = stubPricingPhase(
            billingCycleCount = 0,
            billingPeriod = Period(value = 1, unit = Period.Unit.MONTH, "P1M"),
            price = 9.99,
            recurrenceMode = ProductDetails.RecurrenceMode.INFINITE_RECURRING,
        )
        val subscriptionOption = createSubscriptionOption(
            productId = "paywall_tester.subs",
            basePlanId = "monthly",
            offerId = "promotional-offer-id",
            pricingPhases = listOf(
                stubPricingPhase(
                    billingCycleCount = 1,
                    billingPeriod = Period(value = 1, unit = Period.Unit.MONTH, "P1M"),
                    price = 0.0,
                    recurrenceMode = ProductDetails.RecurrenceMode.FINITE_RECURRING,
                ),
                regularPricingPhase
            )
        )
        val subscriptionOptionBase = createSubscriptionOption(
            productId = "paywall_tester.subs",
            basePlanId = "monthly",
            offerId = null,
            pricingPhases = listOf(regularPricingPhase)
        )
        val monthlyProduct = createGoogleStoreProduct(
            productId = "paywall_tester.subs",
            basePlanId = "monthly",
            subscriptionOptions = listOf(subscriptionOption, subscriptionOptionBase)
        )

        coEvery { purchases.awaitGetProduct("paywall_tester.subs", "monthly") } returns monthlyProduct

        val promotionalOffer = createPromotionalOffer(
            crossProductPromotions = mapOf("paywall_tester.subs" to
                HelpPath.PathDetail.PromotionalOffer.CrossProductPromotion(
                    storeOfferIdentifier = "promotional-offer-id",
                    targetProductId = "paywall_tester.subs"
                ))
        )

        val originalPath = createOriginalPath()

        setupSuccessLoadScreen(originalPath, model)

        val result = model.loadAndDisplayPromotionalOffer(
            context = mockk(relaxed = true),
            product = monthlyProduct,
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
    fun `loadAndDisplayPromotionalOffer handles source product without base plan`(): Unit = runBlocking {
        setupPurchasesMock()

        val model = setupViewModel()
        val regularPricingPhase = stubPricingPhase(
            billingCycleCount = 0,
            billingPeriod = Period(value = 1, unit = Period.Unit.MONTH, "P1M"),
            price = 9.99,
            recurrenceMode = ProductDetails.RecurrenceMode.INFINITE_RECURRING,
        )

        val subscriptionOption = createSubscriptionOption(
            productId = "paywall_tester.subs",
            basePlanId = "monthly",
            offerId = "promotional-offer-id",
            pricingPhases = listOf(
                stubPricingPhase(
                    billingCycleCount = 1,
                    billingPeriod = Period(value = 1, unit = Period.Unit.MONTH, "P1M"),
                    price = 0.0,
                    recurrenceMode = ProductDetails.RecurrenceMode.FINITE_RECURRING,
                ),
                regularPricingPhase
            )
        )
        val subscriptionOptionBase = createSubscriptionOption(
            productId = "paywall_tester.subs",
            basePlanId = "monthly",
            offerId = null,
            pricingPhases = listOf(regularPricingPhase)
        )

        val product = createGoogleStoreProduct(
            productId = "paywall_tester.subs",
            basePlanId = "monthly",
            subscriptionOptions = listOf(subscriptionOption, subscriptionOptionBase)
        )

        val promotionalOffer = createPromotionalOffer(
            crossProductPromotions = mapOf("paywall_tester.subs" to
                HelpPath.PathDetail.PromotionalOffer.CrossProductPromotion(
                    storeOfferIdentifier = "promotional-offer-id",
                    targetProductId = "paywall_tester.subs"
                ))
        )

        val originalPath = createOriginalPath()
        setupSuccessLoadScreen(originalPath, model)

        coEvery { purchases.awaitGetProduct("paywall_tester.subs", "monthly") } returns product

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
    fun `onNavigationButtonPressed handles CLOSE and BACK buttons correctly`(): Unit = runBlocking {
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
    fun `dismissRestoreDialog reloads customer center`(): Unit = runBlocking {
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
    fun `notifyListenersForRestoreStarted calls both listeners`(): Unit = runBlocking {
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
    fun `notifyListenersForRestoreCompleted calls both listeners with correct customer info`(): Unit = runBlocking {
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
    fun `notifyListenersForRestoreFailed calls both listeners with correct error`(): Unit = runBlocking {
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
    fun `notifyListenersForManageSubscription calls both listeners`(): Unit = runBlocking {
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

        // Wait for the initial state to be loaded
        val initialState = model.state.first { it is CustomerCenterState.Success } as CustomerCenterState.Success

        // First, select a purchase to navigate to the detail view
        val purchaseInformation = CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing
        model.selectPurchase(purchaseInformation)

        // Wait for the navigation to complete
        model.state.first { state ->
            state is CustomerCenterState.Success &&
            state.currentDestination is CustomerCenterDestination.SelectedPurchaseDetail
        }

        // When Cancel path is triggered
        model.pathButtonPressed(
            context,
            HelpPath(
                id = "test_id",
                title = "Cancel",
                type = HelpPath.PathType.CANCEL
            ),
            purchaseInformation
        )

        // Then both listeners should be notified
        verify(exactly = 1) { directListener.onShowingManageSubscriptions() }
        verify(exactly = 1) { purchasesListener.onShowingManageSubscriptions() }
    }

    @Test
    fun `feedback survey completion notifies listeners with correct ID`(): Unit = runBlocking {
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
                if (state is CustomerCenterState.Success && state.currentDestination is CustomerCenterDestination.FeedbackSurvey) {
                    feedbackState = state
                    cancel()
                }
            }
        }
        feedbackJob.join()

        // Ensure we have a state with feedback survey destination
        assertThat(feedbackState).isNotNull
        assertThat(feedbackState?.currentDestination).isInstanceOf(CustomerCenterDestination.FeedbackSurvey::class.java)

        val feedbackDestination = feedbackState?.currentDestination as? CustomerCenterDestination.FeedbackSurvey
        feedbackDestination?.data?.onAnswerSubmitted?.invoke(feedbackSurveyOption)

        // Verify both listeners were called with the correct ID
        verify(exactly = 1) { directListener.onFeedbackSurveyCompleted(feedbackSurveyOptionId) }
        verify(exactly = 1) { purchasesListener.onFeedbackSurveyCompleted(feedbackSurveyOptionId) }
    }

    @Test
    fun `notifyListenersForManagementOptionSelected converts paths to actions and notifies listeners`(): Unit = runBlocking {
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
    fun `loadCustomerCenter is called after successful promotional offer purchase`(): Unit = runBlocking {
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
    fun `loadCustomerCenter uses base plan from active subscription when entitlements are empty`(): Unit = runBlocking {
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
    fun `isSupportedPaths allows CANCEL when purchase is not lifetime`(): Unit = runBlocking {
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

        // Wait for the initial load to complete
        val initialState = model.state.first { it is CustomerCenterState.Success } as CustomerCenterState.Success

        // Select a purchase (the first one from the loaded purchases)
        val purchaseInformation = initialState.purchases.first()
        model.selectPurchase(purchaseInformation)

        val job = launch {
            model.state.collect { state ->
                if (state is CustomerCenterState.Success &&
                    state.currentDestination is CustomerCenterDestination.SelectedPurchaseDetail) {
                    val paths = state.mainScreenPaths
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
    fun `isSupportedPaths filters CANCEL when management URL is not present`(): Unit = runBlocking {
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
                managementURL = null,
            )
        )
        every { customerInfo.nonSubscriptionTransactions } returns listOf()

        val model = CustomerCenterViewModelImpl(
            purchases = purchases,
            locale = Locale.US,
            colorScheme = TestData.Constants.currentColorScheme,
            isDarkMode = false
        )

        val state = model.state.filterIsInstance<CustomerCenterState.Success>().first()
        val paths = state.mainScreenPaths
        assertThat(paths)
            .withFailMessage("Expected CANCEL path to not be present when there are no management URL. Paths: $paths")
            .noneMatch { it.type == HelpPath.PathType.CANCEL }
    }

    @Test
    fun `isSupportedPaths filters CANCEL when purchase is lifetime`(): Unit = runBlocking {
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

        val state = model.state.filterIsInstance<CustomerCenterState.Success>().first()
        val paths = state.mainScreenPaths
        assertThat(paths)
            .withFailMessage("Expected CANCEL path to not be present for lifetime purchases. Paths: $paths")
            .noneMatch { it.type == HelpPath.PathType.CANCEL }
    }

    @Test
    fun `isSupportedPaths filters CANCEL when purchase is non Play Store lifetime and management URL`(): Unit = runBlocking {
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

        val state = model.state.filterIsInstance<CustomerCenterState.Success>().first()
        val paths = state.mainScreenPaths
        assertThat(paths)
            .withFailMessage("Expected CANCEL path to not be present for lifetime purchases. Paths: $paths")
            .noneMatch { it.type == HelpPath.PathType.CANCEL }
    }

    @Test
    fun `isSupportedPaths allows CUSTOM_URL for Play store`(): Unit = runBlocking {
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

        val state = model.state.filterIsInstance<CustomerCenterState.Success>().first()
        val paths = state.mainScreenPaths
        assertThat(paths)
            .withFailMessage("Expected CUSTOM_URL path to be present. Paths: $paths")
            .anyMatch { it.type == HelpPath.PathType.CUSTOM_URL }
    }

    @Test
    fun `isSupportedPaths allows CUSTOM_URL for App store`(): Unit = runBlocking {
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

        val state = model.state.filterIsInstance<CustomerCenterState.Success>().first()
        val paths = state.mainScreenPaths
        assertThat(paths)
            .withFailMessage("Expected CUSTOM_URL path to be present. Paths: $paths")
            .anyMatch { it.type == HelpPath.PathType.CUSTOM_URL }
    }

    @Test
    fun `isSupportedPaths allows MISSING_PURCHASE for App store`(): Unit = runBlocking {
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

        val state = model.state.filterIsInstance<CustomerCenterState.Success>().first()
        val paths = state.mainScreenPaths
        assertThat(paths)
            .withFailMessage("Expected MISSING_PURCHASE path for APP_STORE. Paths: $paths")
            .anyMatch { it.type == HelpPath.PathType.MISSING_PURCHASE }
    }

    @Test
    fun `isSupportedPaths allows MISSING_PURCHASE for Play store`(): Unit = runBlocking {
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

        val state = model.state.filterIsInstance<CustomerCenterState.Success>().first()
        val paths = state.mainScreenPaths
        assertThat(paths)
            .withFailMessage("Expected MISSING_PURCHASE path for PLAY_STORE. Paths: $paths")
            .anyMatch { it.type == HelpPath.PathType.MISSING_PURCHASE }
    }

    @Test
    fun `isSupportedPaths filters non compatible paths for App store`(): Unit = runBlocking {
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

        val state = model.state.filterIsInstance<CustomerCenterState.Success>().first()
        val paths = state.mainScreenPaths
        assertThat(paths)
            .withFailMessage("Not expected REFUND_REQUEST path for APP_STORE. Paths: $paths")
            .noneMatch { it.type == HelpPath.PathType.REFUND_REQUEST }
        assertThat(paths)
            .withFailMessage("Not expected CHANGE_PLANS path for APP_STORE. Paths: $paths")
            .noneMatch { it.type == HelpPath.PathType.CHANGE_PLANS }
    }

    @Test
    fun `transformPathsOnSubscriptionState converts CANCEL to RESUBSCRIBE for cancelled subs`(): Unit = runBlocking {
        setupPurchasesMock()

        val cancelPath = HelpPath(
            id = "cancel_id",
            title = "Cancel",
            type = HelpPath.PathType.CANCEL,
            feedbackSurvey = HelpPath.PathDetail.FeedbackSurvey(
                title = "Why are you cancelling?",
                options = listOf(
                    HelpPath.PathDetail.FeedbackSurvey.Option(
                        id = "1",
                        title = "Too expensive",
                        promotionalOffer = null
                    )
                )
            ),
            promotionalOffer = examplePromotionalOffer()
        )

        val managementScreen = Screen(
            type = Screen.ScreenType.MANAGEMENT,
            title = "Management",
            subtitle = null,
            paths = listOf(cancelPath)
        )

        every { configData.getManagementScreen() } returns managementScreen
        every { configData.localization } returns CustomerCenterConfigData.Localization(
            locale = "en_US",
            localizedStrings = mapOf(
                "cancel" to "Cancel",
                "resubscribe" to "Resubscribe"
            )
        )

        val model = CustomerCenterViewModelImpl(
            purchases = purchases,
            locale = Locale.US,
            colorScheme = TestData.Constants.currentColorScheme,
            isDarkMode = false
        )

        // Wait for the initial load to complete
        model.state.filterIsInstance<CustomerCenterState.Success>().first()

        // Select the cancelled purchase
        val cancelledPurchase = CustomerCenterConfigTestData.purchaseInformationYearlyExpiring
        model.selectPurchase(cancelledPurchase)

        val state = model.state.filterIsInstance<CustomerCenterState.Success>()
            .first { it.currentDestination is CustomerCenterDestination.SelectedPurchaseDetail }
        val paths = state.detailScreenPaths

        val transformedPath = paths.find { it.id == "cancel_id" }
        assertThat(transformedPath).isNotNull
        assertThat(transformedPath?.title).isEqualTo("Resubscribe")
        assertThat(transformedPath?.type).isEqualTo(HelpPath.PathType.CANCEL)
        assertThat(transformedPath?.feedbackSurvey).isNull() // Should be removed
        assertThat(transformedPath?.promotionalOffer).isNull() // Should be removed
    }

    @Test
    fun `transformPathsOnSubscriptionState keeps CANCEL unchanged for active subscriptions`(): Unit = runBlocking {
        setupPurchasesMock()

        val originalFeedbackSurvey = HelpPath.PathDetail.FeedbackSurvey(
            title = "Why are you cancelling?",
            options = listOf(
                HelpPath.PathDetail.FeedbackSurvey.Option(
                    id = "1",
                    title = "Too expensive",
                    promotionalOffer = null
                )
            )
        )

        val originalPromotionalOffer = examplePromotionalOffer()

        val cancelPath = HelpPath(
            id = "cancel_id",
            title = "Cancel",
            type = HelpPath.PathType.CANCEL,
            feedbackSurvey = originalFeedbackSurvey,
            promotionalOffer = originalPromotionalOffer
        )

        val managementScreen = Screen(
            type = Screen.ScreenType.MANAGEMENT,
            title = "Management",
            subtitle = null,
            paths = listOf(cancelPath)
        )

        every { configData.getManagementScreen() } returns managementScreen
        every { configData.localization } returns CustomerCenterConfigData.Localization(
            locale = "en_US",
            localizedStrings = mapOf(
                "cancel" to "Cancel",
                "resubscribe" to "Resubscribe"
            )
        )

        val model = CustomerCenterViewModelImpl(
            purchases = purchases,
            locale = Locale.US,
            colorScheme = TestData.Constants.currentColorScheme,
            isDarkMode = false
        )

        // Wait for the initial load to complete
        model.state.filterIsInstance<CustomerCenterState.Success>().first()

        val activePurchase = CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing
        model.selectPurchase(activePurchase)

        val state = model.state.filterIsInstance<CustomerCenterState.Success>()
            .first { it.currentDestination is CustomerCenterDestination.SelectedPurchaseDetail }
        val paths = state.detailScreenPaths

        // Find the path that should remain unchanged
        val unchangedPath = paths.find { it.id == "cancel_id" }
        assertThat(unchangedPath).isNotNull
        assertThat(unchangedPath?.title).isEqualTo("Cancel") // Title should remain Cancel
        assertThat(unchangedPath?.type).isEqualTo(HelpPath.PathType.CANCEL)
        assertThat(unchangedPath?.feedbackSurvey).isEqualTo(originalFeedbackSurvey) // Should be preserved
        assertThat(unchangedPath?.promotionalOffer).isEqualTo(originalPromotionalOffer) // Should be preserved
    }

    @Test
    fun `transformPathsOnSubscriptionState uses localized RESUBSCRIBE string`(): Unit = runBlocking {
        setupPurchasesMock()
        every { customerInfo.activeSubscriptions } returns setOf("product_id")

        val cancelPath = HelpPath(
            id = "cancel_id",
            title = "Cancel",
            type = HelpPath.PathType.CANCEL
        )

        val managementScreen = Screen(
            type = Screen.ScreenType.MANAGEMENT,
            title = "Management",
            subtitle = null,
            paths = listOf(cancelPath)
        )

        every { configData.getManagementScreen() } returns managementScreen
        every { configData.localization } returns CustomerCenterConfigData.Localization(
            locale = "es_ES",
            localizedStrings = mapOf(
                "cancel" to "Cancelar",
                "resubscribe" to "Resuscribirse"
            )
        )

        val model = CustomerCenterViewModelImpl(
            purchases = purchases,
            locale = Locale.US,
            colorScheme = TestData.Constants.currentColorScheme,
            isDarkMode = false
        )

        // Wait for the initial load to complete
        model.state.filterIsInstance<CustomerCenterState.Success>().first()

        // Select the cancelled purchase
        val cancelledPurchase = CustomerCenterConfigTestData.purchaseInformationYearlyExpiring
        model.selectPurchase(cancelledPurchase)

        val state = model.state.filterIsInstance<CustomerCenterState.Success>()
            .first { it.currentDestination is CustomerCenterDestination.SelectedPurchaseDetail }
        val paths = state.detailScreenPaths

        // Find the transformed path
        val transformedPath = paths.find { it.id == "cancel_id" }
        assertThat(transformedPath).isNotNull
        assertThat(transformedPath?.title).isEqualTo("Resuscribirse") // Should use localized string
    }

    @Test
    fun `transformPathsOnSubscriptionState falls back to default when localization missing`(): Unit = runBlocking {
        setupPurchasesMock()
        every { customerInfo.activeSubscriptions } returns setOf("product_id")

        val cancelPath = HelpPath(
            id = "cancel_id",
            title = "Cancel",
            type = HelpPath.PathType.CANCEL
        )

        val managementScreen = Screen(
            type = Screen.ScreenType.MANAGEMENT,
            title = "Management",
            subtitle = null,
            paths = listOf(cancelPath)
        )

        every { configData.getManagementScreen() } returns managementScreen
        every { configData.localization } returns CustomerCenterConfigData.Localization(
            locale = "en_US",
            localizedStrings = mapOf(
                "cancel" to "Cancel"
                // No "resubscribe" string provided
            )
        )

        val model = CustomerCenterViewModelImpl(
            purchases = purchases,
            locale = Locale.US,
            colorScheme = TestData.Constants.currentColorScheme,
            isDarkMode = false
        )

        // Wait for the initial load to complete
        model.state.filterIsInstance<CustomerCenterState.Success>().first()

        // Select the cancelled purchase
        val cancelledPurchase = CustomerCenterConfigTestData.purchaseInformationYearlyExpiring
        model.selectPurchase(cancelledPurchase)


        val state = model.state.filterIsInstance<CustomerCenterState.Success>()
            .first { it.currentDestination is CustomerCenterDestination.SelectedPurchaseDetail }
        val paths = state.detailScreenPaths

        val transformedPath = paths.find { it.id == "cancel_id" }
        assertThat(transformedPath).isNotNull
        assertThat(transformedPath?.title).isEqualTo("Resubscribe")
    }

    @Test
    fun `isSupportedPaths filters non compatible paths for Play store`(): Unit = runBlocking {
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

        // Wait for the initial state to load
        model.state.first { it is CustomerCenterState.Success }

        val job = launch {
            model.state.collect { state ->
                if (state is CustomerCenterState.Success) {
                    val paths = state.mainScreenPaths
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
    fun `loadCustomerCenter shows latest expired subscription when no active purchases`(): Unit = runBlocking {
        setupPurchasesMock()
        every { customerInfo.activeSubscriptions } returns setOf()
        every { customerInfo.nonSubscriptionTransactions } returns emptyList()

        val expiredDate1 = Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000) // 7 days ago
        val purchaseDate1 = expiredDate1.subtract(Duration.parse("7d"))

        val expiredDate2 = Date(System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000) // 3 days ago (latest)
        val purchaseDate2 = expiredDate2.subtract(Duration.parse("7d"))

        val olderExpiredSubscription = SubscriptionInfo(
            productIdentifier = "old_product",
            purchaseDate = purchaseDate1,
            originalPurchaseDate = purchaseDate1,
            expiresDate = expiredDate1,
            store = Store.PLAY_STORE,
            unsubscribeDetectedAt = Date(),
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
            productPlanIdentifier = null,
            managementURL = null,
        )

        val latestExpiredSubscription = SubscriptionInfo(
            productIdentifier = "latest_product",
            purchaseDate = purchaseDate2,
            originalPurchaseDate = purchaseDate2,
            expiresDate = expiredDate2,
            store = Store.PLAY_STORE,
            unsubscribeDetectedAt = Date(),
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
            productPlanIdentifier = null,
            managementURL = null,
        )

        every { customerInfo.subscriptionsByProductIdentifier } returns mapOf(
            "old_product" to olderExpiredSubscription,
            "latest_product" to latestExpiredSubscription
        )

        val mockProduct = createGoogleStoreProduct(
            productId = "latest_product",
            basePlanId = "monthly",
            name = "Latest Product"
        )
        coEvery { purchases.awaitGetProduct("latest_product", null) } returns mockProduct

        val model = setupViewModel()

        val successState = model.state.filterIsInstance<CustomerCenterState.Success>().first()

        assertThat(successState.purchases).hasSize(1)
        val purchase = successState.purchases.first()
        assertThat(purchase.product?.id).isEqualTo("latest_product:monthly")
        assertThat(purchase.isExpired).isTrue()
    }

    @Test
    fun `loadCustomerCenter returns empty when no purchases at all`(): Unit = runBlocking {
        setupPurchasesMock()
        every { customerInfo.activeSubscriptions } returns setOf()
        every { customerInfo.nonSubscriptionTransactions } returns emptyList()
        every { customerInfo.subscriptionsByProductIdentifier } returns emptyMap()

        val model = setupViewModel()

        val successState = model.state.filterIsInstance<CustomerCenterState.Success>().first()

        assertThat(successState.purchases).isEmpty()
    }

    @Test
    fun `expired subscription should not show CANCEL path`(): Unit = runBlocking {
        setupPurchasesMock()
        every { customerInfo.activeSubscriptions } returns setOf()
        every { customerInfo.nonSubscriptionTransactions } returns emptyList()

        val expiredDate = Date(System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000) // 3 days ago
        val purchaseDate = expiredDate.subtract(Duration.parse("7d"))

        val expiredSubscription = SubscriptionInfo(
            productIdentifier = "expired_product",
            purchaseDate = purchaseDate,
            originalPurchaseDate = purchaseDate,
            expiresDate = expiredDate,
            store = Store.PLAY_STORE,
            unsubscribeDetectedAt = Date(),
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
            productPlanIdentifier = null,
            managementURL = Uri.parse("https://play.google.com/store/account/subscriptions"),
        )

        every { customerInfo.subscriptionsByProductIdentifier } returns mapOf(
            "expired_product" to expiredSubscription
        )

        val mockProduct = createGoogleStoreProduct(
            productId = "expired_product",
            basePlanId = "monthly",
            name = "Expired Product"
        )
        coEvery { purchases.awaitGetProduct("expired_product", null) } returns mockProduct

        val model = setupViewModel()

        val successState = model.state.filterIsInstance<CustomerCenterState.Success>().first()

        assertThat(successState.purchases).hasSize(1)
        val purchase = successState.purchases.first()
        assertThat(purchase.isExpired).isTrue()

        val paths = successState.mainScreenPaths
        assertThat(paths)
            .withFailMessage("Expected CANCEL path to not be present for expired subscription. Paths: $paths")
            .noneMatch { it.type == CustomerCenterConfigData.HelpPath.PathType.CANCEL }
        
        // Should have paths from NO_ACTIVE screen (MISSING_PURCHASE and CUSTOM_URL)
        assertThat(paths)
            .withFailMessage("Expected MISSING_PURCHASE path from NO_ACTIVE screen. Paths: $paths")
            .anyMatch { it.type == CustomerCenterConfigData.HelpPath.PathType.MISSING_PURCHASE && it.id == "restore_id" }
        assertThat(paths)
            .withFailMessage("Expected CUSTOM_URL path from NO_ACTIVE screen. Paths: $paths")
            .anyMatch { it.type == CustomerCenterConfigData.HelpPath.PathType.CUSTOM_URL && it.id == "support_id" }
    }

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
        coEvery { purchases.awaitSyncPurchases() } returns customerInfo
        every { purchases.preferredUILocaleOverride } returns null
        coEvery { purchases.awaitGetVirtualCurrencies() } returns mockk()
        every { purchases.invalidateVirtualCurrenciesCache() } just Runs

        every { configData.getManagementScreen() } returns screens[Screen.ScreenType.MANAGEMENT]
        every { configData.getNoActiveScreen() } returns screens[Screen.ScreenType.NO_ACTIVE]
        every { configData.localization } returns CustomerCenterConfigData.Localization(
            locale = "en_US",
            localizedStrings = mapOf(
                "cancel" to "Cancel",
            )
        )
        every { configData.support } returns CustomerCenterConfigData.Support(
            displayVirtualCurrencies = false,
            supportTickets = CustomerCenterConfigData.Support.SupportTickets(),
        )

        every { customerInfo.managementURL } returns null
        every { customerInfo.activeSubscriptions } returns setOf()
        every { customerInfo.entitlements } returns EntitlementInfos(emptyMap(), VerificationResult.VERIFIED)
        every { customerInfo.subscriptionsByProductIdentifier } returns emptyMap()
        every { customerInfo.nonSubscriptionTransactions } returns emptyList()
    }

    private suspend fun setupSuccessLoadScreen(
        originalPath: HelpPath,
        model: CustomerCenterViewModelImpl,
    ): CustomerCenterState.Success {
        // Set up the state as Success
        val managementScreen = Screen(
            type = Screen.ScreenType.MANAGEMENT,
            title = "Management",
            subtitle = null,
            paths = listOf(originalPath)
        )

        val noActiveScreen = Screen(
            type = Screen.ScreenType.NO_ACTIVE,
            title = "No Active Subscription",
            subtitle = "You don't have an active subscription.",
            paths = emptyList()
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
        every { configData.getNoActiveScreen() } returns noActiveScreen
        every { configData.localization } returns localization

        // Wait for initial state to load
        model.loadCustomerCenter()
        val state = model.state.filterIsInstance<CustomerCenterState.Success>().first()
        return state
    }

    private fun createSubscriptionOption(
        productId: String,
        basePlanId: String,
        offerId: String?,
        firstPhasePeriod: Period = Period(value = 1, unit = Period.Unit.MONTH, "P1M"),
        firstPhasePrice: Double = 0.0,
        secondPhasePeriod: Period = Period(value = 1, unit = Period.Unit.MONTH, "P1M"),
        secondPhasePrice: Double = 9.99,
        pricingPhases: List<PricingPhase> = listOf(
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
    ): GoogleSubscriptionOption {
        return stubGoogleSubscriptionOption(
            productId = productId,
            basePlanId = basePlanId,
            productDetails = mockk(),
            offerId = offerId,
            pricingPhases = pricingPhases,
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
            assertThat(successState.currentDestination).isInstanceOf(CustomerCenterDestination.PromotionalOffer::class.java)
            val promotionalOfferDestination = successState.currentDestination as? CustomerCenterDestination.PromotionalOffer
            promotionalOfferDestination?.data?.let { data ->
                assertThat(data.configuredPromotionalOffer).isEqualTo(expectedPromotionalOffer)
                assertThat(data.subscriptionOption).isEqualTo(expectedSubscriptionOption)
                assertThat(data.originalPath).isEqualTo(expectedOriginalPath)
                assertThat(data.localizedPricingPhasesDescription).isEqualTo(expectedPricingDescription)
            }
        } else {
            assertThat(successState.currentDestination).isNotInstanceOf(CustomerCenterDestination.PromotionalOffer::class.java)
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

    private fun examplePromotionalOffer() = CustomerCenterConfigTestData.customerCenterData()
        .screens[Screen.ScreenType.MANAGEMENT]!!.paths[1].promotionalOffer

    @Test
    fun `onCustomActionSelected calls listener with correct action identifier from actionIdentifier field`(): Unit = runBlocking {
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
        
        val customActionData = com.revenuecat.purchases.customercenter.CustomActionData(
            actionIdentifier = "delete_user",
            purchaseIdentifier = "monthly_sub"
        )
        
        model.onCustomActionSelected(customActionData)
        
        verify { directListener.onCustomActionSelected("delete_user", "monthly_sub") }
        verify { purchasesListener.onCustomActionSelected("delete_user", "monthly_sub") }
    }

    @Test
    fun `pathButtonPressed with CUSTOM_ACTION uses actionIdentifier when available`(): Unit = runBlocking {
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
        
        val customActionPath = HelpPath(
            id = "path_123",
            title = "Delete Account",
            type = HelpPath.PathType.CUSTOM_ACTION,
            actionIdentifier = "delete_user"
        )
        
        val purchaseInfo = createMockPurchaseInformation("monthly_sub")
        
        model.pathButtonPressed(mockk(relaxed = true), customActionPath, purchaseInfo)
        
        verify { directListener.onCustomActionSelected("delete_user", "monthly_sub") }
        verify { purchasesListener.onCustomActionSelected("delete_user", "monthly_sub") }
    }

    @Test
    fun `pathButtonPressed with CUSTOM_ACTION ignores action when actionIdentifier is null`(): Unit = runBlocking {
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
        
        val customActionPath = HelpPath(
            id = "legacy_action_id",
            title = "Rate App",
            type = HelpPath.PathType.CUSTOM_ACTION,
            actionIdentifier = null
        )
        
        model.pathButtonPressed(mockk(relaxed = true), customActionPath, null)
        
        verify(exactly = 0) { directListener.onCustomActionSelected(any(), any()) }
        verify(exactly = 0) { purchasesListener.onCustomActionSelected(any(), any()) }
    }

    @Test
    fun `pathButtonPressed with CUSTOM_ACTION includes purchase identifier when available`(): Unit = runBlocking {
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
        
        val customActionPath = HelpPath(
            id = "path_id",
            title = "Contact Support",
            type = HelpPath.PathType.CUSTOM_ACTION,
            actionIdentifier = "contact_support"
        )
        
        val purchaseInfo = createMockPurchaseInformation("annual_plan")
        
        model.pathButtonPressed(mockk(relaxed = true), customActionPath, purchaseInfo)
        
        verify { directListener.onCustomActionSelected("contact_support", "annual_plan") }
        verify { purchasesListener.onCustomActionSelected("contact_support", "annual_plan") }
    }

    @Test
    fun `pathButtonPressed with CUSTOM_ACTION passes null purchase identifier when no purchase info`(): Unit = runBlocking {
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
        
        val customActionPath = HelpPath(
            id = "path_id",
            title = "General Action",
            type = HelpPath.PathType.CUSTOM_ACTION,
            actionIdentifier = "general_action"
        )
        
        model.pathButtonPressed(mockk(relaxed = true), customActionPath, null)
        
        verify { directListener.onCustomActionSelected("general_action", null) }
        verify { purchasesListener.onCustomActionSelected("general_action", null) }
    }

    private fun createMockPurchaseInformation(productId: String): PurchaseInformation {
        val mockProduct = mockk<StoreProduct>()
        every { mockProduct.id } returns productId
        
        return mockk<PurchaseInformation>().apply {
            every { product } returns mockProduct
        }
    }

    @Test
    fun `loadCustomerCenter loads virtual currencies when displayVirtualCurrencies is true`(): Unit = runBlocking {
        setupPurchasesMock()
        
        val mockVirtualCurrencies = mockk<VirtualCurrencies>()
        coEvery { purchases.awaitGetVirtualCurrencies() } returns mockVirtualCurrencies
        every { purchases.invalidateVirtualCurrenciesCache() } just Runs
        every { configData.support } returns CustomerCenterConfigData.Support(
            displayVirtualCurrencies = true,
            supportTickets = CustomerCenterConfigData.Support.SupportTickets(),
        )

        val model = setupViewModel()
        val state = model.state.filterIsInstance<CustomerCenterState.Success>().first()

        assertThat(state.virtualCurrencies).isEqualTo(mockVirtualCurrencies)
        verify(exactly = 1) { purchases.invalidateVirtualCurrenciesCache() }
        coVerify(exactly = 1) { purchases.awaitGetVirtualCurrencies() }
    }

    @Test
    fun `loadCustomerCenter does not load virtual currencies when displayVirtualCurrencies is false`(): Unit = runBlocking {
        setupPurchasesMock()
        every { configData.support } returns CustomerCenterConfigData.Support(displayVirtualCurrencies = false)

        val model = setupViewModel()
        val state = model.state.filterIsInstance<CustomerCenterState.Success>().first()

        assertThat(state.virtualCurrencies).isNull()
        verify(exactly = 0) { purchases.invalidateVirtualCurrenciesCache() }
        coVerify(exactly = 0) { purchases.awaitGetVirtualCurrencies() }
    }

    @Test
    fun `loadCustomerCenter does not load virtual currencies when displayVirtualCurrencies is null`(): Unit = runBlocking {
        setupPurchasesMock()
        every { configData.support } returns CustomerCenterConfigData.Support(
            displayVirtualCurrencies = null,
            supportTickets = CustomerCenterConfigData.Support.SupportTickets(),
        )

        val model = setupViewModel()
        val state = model.state.filterIsInstance<CustomerCenterState.Success>().first()

        assertThat(state.virtualCurrencies).isNull()
        verify(exactly = 0) { purchases.invalidateVirtualCurrenciesCache() }
        coVerify(exactly = 0) { purchases.awaitGetVirtualCurrencies() }
    }

    @Test
    fun `showVirtualCurrencyBalances navigates to virtual currency balances screen when displayVirtualCurrencies is true`(): Unit = runBlocking {
        setupPurchasesMock()
        every { configData.support } returns CustomerCenterConfigData.Support(
            displayVirtualCurrencies = true,
            supportTickets = CustomerCenterConfigData.Support.SupportTickets(),
        )
        val model = setupViewModel()

        model.state.filterIsInstance<CustomerCenterState.Success>().first()

        model.showVirtualCurrencyBalances()
        
        val updatedState = model.state.value as CustomerCenterState.Success
        assertThat(updatedState.currentDestination).isInstanceOf(CustomerCenterDestination.VirtualCurrencyBalances::class.java)
        assertThat(updatedState.navigationButtonType).isEqualTo(CustomerCenterState.NavigationButtonType.BACK)
        
        val destination = updatedState.currentDestination as CustomerCenterDestination.VirtualCurrencyBalances
        assertThat(destination.title).isEqualTo("In-App Currencies")
    }

    @Test
    fun `showVirtualCurrencyBalances does nothing when displayVirtualCurrencies is false`(): Unit = runBlocking {
        setupPurchasesMock()
        every { configData.support } returns CustomerCenterConfigData.Support(displayVirtualCurrencies = false)

        val model = setupViewModel()
        
        val initialState = model.state.filterIsInstance<CustomerCenterState.Success>().first()
        val initialDestination = initialState.currentDestination
        
        model.showVirtualCurrencyBalances()
        
        val updatedState = model.state.value as CustomerCenterState.Success
        assertThat(updatedState.currentDestination).isEqualTo(initialDestination)
    }

    @Test
    fun `showVirtualCurrencyBalances does nothing when displayVirtualCurrencies is null`(): Unit = runBlocking {
        setupPurchasesMock()
        every { configData.support } returns CustomerCenterConfigData.Support(
            displayVirtualCurrencies = null,
            supportTickets = CustomerCenterConfigData.Support.SupportTickets(),
        )

        val model = setupViewModel()
        
        val initialState = model.state.filterIsInstance<CustomerCenterState.Success>().first()
        val initialDestination = initialState.currentDestination
        
        model.showVirtualCurrencyBalances()
        
        val updatedState = model.state.value as CustomerCenterState.Success
        assertThat(updatedState.currentDestination).isEqualTo(initialDestination)
    }

    @Test
    fun `showVirtualCurrencyBalances does nothing when state is not Success`(): Unit = runBlocking {
        setupPurchasesMock()
        coEvery { purchases.awaitCustomerCenterConfigData() } throws PurchasesException(
            PurchasesError(PurchasesErrorCode.UnknownError, "Test error")
        )
        
        val model = setupViewModel()
        
        val errorState = model.state.filterIsInstance<CustomerCenterState.Error>().first()
        
        model.showVirtualCurrencyBalances()
        
        val currentState = model.state.value
        assertThat(currentState).isEqualTo(errorState)
        assertThat(currentState).isInstanceOf(CustomerCenterState.Error::class.java)
    }

    @Test
    fun `onActivityResumed refreshes customer center after launching manage subscriptions`(): Unit = runBlocking {
        setupPurchasesMock()

        var customerCenterConfigCalls = 0
        coEvery { purchases.awaitCustomerCenterConfigData() } coAnswers {
            customerCenterConfigCalls++
            configData
        }

        val model = setupViewModel()
        model.state.filterIsInstance<CustomerCenterState.Success>().first()
        val initialCalls = customerCenterConfigCalls

        val context = mockk<Context>(relaxed = true)
        every { context.packageName } returns "com.revenuecat.test"
        val product = mockk<StoreProduct> {
            every { id } returns "monthly_sub"
        }
        val purchaseInformation = mockk<PurchaseInformation> {
            every { store } returns Store.PLAY_STORE
            every { this@mockk.product } returns product
            every { managementURL } returns null
        }
        val cancelPath = HelpPath(
            id = "cancel",
            title = "Cancel",
            type = HelpPath.PathType.CANCEL,
        )

        model.pathButtonPressed(context, cancelPath, purchaseInformation)
        verify(timeout = 2_000) { context.startActivity(any()) }

        model.onActivityResumed()

        val deadline = System.currentTimeMillis() + 2_000
        while (System.currentTimeMillis() < deadline && customerCenterConfigCalls <= initialCalls) {
            Thread.sleep(25)
        }
        assertThat(customerCenterConfigCalls).isGreaterThan(initialCalls)
    }
}
