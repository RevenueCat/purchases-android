package com.revenuecat.purchases.ui.revenuecatui.data

import android.app.Activity
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseResult
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.models.Transaction
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.events.PaywallEvent
import com.revenuecat.purchases.paywalls.events.PaywallEventType
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallMode
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogic
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogicResult
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogicWithCallback
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.UiConfig
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifyOrder
import junit.framework.TestCase.fail
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL
import java.util.Date
import java.util.UUID

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@RunWith(AndroidJUnit4::class)
class PaywallViewModelTest {
    private val defaultOffering = TestData.template2Offering
    private val defaultLocaleIdentifier = LocaleId("en_US")
    private val localizations = nonEmptyMapOf(
        defaultLocaleIdentifier to nonEmptyMapOf(
            LocalizationKey("dummy_text") to LocalizationData.Text("dummy text"),
        )
    )
    private val emptyPaywallComponentsData = PaywallComponentsData(
        templateName = "template",
        assetBaseURL = URL("https://assets.pawwalls.com"),
        componentsConfig = ComponentsConfig(
            base = PaywallComponentsConfig(
                stack = StackComponent(components = emptyList()),
                background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                stickyFooter = null,
            ),
        ),
        componentsLocalizations = localizations,
        defaultLocaleIdentifier = defaultLocaleIdentifier,
    )

    private lateinit var purchases: PurchasesType
    private lateinit var customerInfo: CustomerInfo

    private lateinit var context: Context
    private lateinit var activity: Activity
    private lateinit var listener: PaywallListener

    private var dismissInvoked = false

    private val offerings = Offerings(
        defaultOffering,
        mapOf(
            TestData.template1Offering.identifier to TestData.template1Offering,
            TestData.template2Offering.identifier to TestData.template2Offering
        ),
    )

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        purchases = mockk()
        customerInfo = mockk()

        activity = mockk()
        context = mockk()

        listener = mockk()

        dismissInvoked = false

        coEvery { purchases.awaitOfferings() } returns offerings
        coEvery { purchases.awaitCustomerInfo(any()) } returns customerInfo
        every { purchases.purchasesAreCompletedBy } returns PurchasesAreCompletedBy.REVENUECAT

        every { customerInfo.activeSubscriptions } returns setOf()
        every { customerInfo.nonSubscriptionTransactions } returns listOf()

        every { purchases.storefrontCountryCode } returns "US"
        every { purchases.track(any()) } just Runs
        every { purchases.syncPurchases() } just Runs

        every { listener.onPurchaseStarted(any()) } just runs
        every { listener.onPurchaseCompleted(any(), any()) } just runs
        every { listener.onPurchaseError(any()) } just runs
        every { listener.onRestoreStarted() } just runs
        every { listener.onRestoreCompleted(any()) } just runs
        every { listener.onRestoreError(any()) } just runs
        every { listener.onPurchaseCancelled() } just runs
    }

    @After
    internal fun tearDown() {
        clearAllMocks()
    }

    // Completion Handler Callback Tests

    @Test
    fun `Custom completion handler restore purchases logic success triggers syncPurchases`() = runTest {
        every { purchases.purchasesAreCompletedBy } returns PurchasesAreCompletedBy.MY_APP

        val customRestoreCalled = MutableStateFlow(false)

        val myAppPurchaseLogic = TestAppPurchaseLogicWithCallbacks(
            null,
            customRestoreCalled,
            null,
            PurchaseLogicResult.Success
        )

        val model = create(
            customPurchaseLogic = myAppPurchaseLogic,
        )

        model.restorePurchases()

        customRestoreCalled.first { it }

        coVerify(exactly = 1) { purchases.syncPurchases() }
        coVerify(exactly = 0) { listener.onRestoreStarted() }
        coVerify(exactly = 0) { listener.onRestoreCompleted(customerInfo) }
    }

    @Test
    fun `Custom completion handler restore purchases logic error does not trigger syncPurchases`() = runTest {
        every { purchases.purchasesAreCompletedBy } returns PurchasesAreCompletedBy.MY_APP

        val customRestoreCalled = MutableStateFlow(false)

        val myAppPurchaseLogic = TestAppPurchaseLogicWithCallbacks(
            null,
            customRestoreCalled,
            null,
            PurchaseLogicResult.Error()
        )

        val model = create(
            customPurchaseLogic = myAppPurchaseLogic,
        )

        model.restorePurchases()

        customRestoreCalled.first { it }

        coVerify(exactly = 0) { purchases.syncPurchases() }
        coVerify(exactly = 0) { listener.onRestoreStarted() }
        coVerify(exactly = 0) { listener.onRestoreCompleted(customerInfo) }
    }

    @Test
    fun `Custom completion handler purchase logic success triggers syncPurchases`() = runTest {
        every { purchases.purchasesAreCompletedBy } returns PurchasesAreCompletedBy.MY_APP

        val customPurchaseCalled = MutableStateFlow(false)

        val myAppPurchaseLogic = TestAppPurchaseLogicWithCallbacks(
            customPurchaseCalled,
            null,
            PurchaseLogicResult.Success,
            null,
        )

        val model = create(
            customPurchaseLogic = myAppPurchaseLogic,
        )

        model.purchaseSelectedPackage(activity)

        customPurchaseCalled.first { it }

        coVerify(exactly = 1)  { purchases.syncPurchases() }
        coVerify(exactly = 0) { listener.onPurchaseStarted(any()) }
        coVerify(exactly = 0) { listener.onPurchaseCompleted(customerInfo, any()) }

        assertThat(model.actionInProgress.value).isFalse
        assertThat(dismissInvoked).isTrue
    }

    @Test
    fun `Custom completion handler purchase logic cancelled doesn't trigger syncPurchases`() = runTest {
        every { purchases.purchasesAreCompletedBy } returns PurchasesAreCompletedBy.MY_APP

        val customPurchaseCalled = MutableStateFlow(false)

        val myAppPurchaseLogic = TestAppPurchaseLogicWithCallbacks(
            customPurchaseCalled,
            null,
            PurchaseLogicResult.Cancellation,
            null,
        )

        val model = create(
            customPurchaseLogic = myAppPurchaseLogic,
        )

        model.purchaseSelectedPackage(activity)

        customPurchaseCalled.first { it }

        coVerify(exactly = 0) { purchases.syncPurchases() }
        coVerify(exactly = 0) { listener.onPurchaseStarted(any()) }
        coVerify(exactly = 0) { listener.onPurchaseCancelled() }
    }

    @Test
    fun `Custom completion handler purchase logic error doesn't trigger syncPurchases`() = runTest {
        every { purchases.purchasesAreCompletedBy } returns PurchasesAreCompletedBy.MY_APP

        val customPurchaseCalled = MutableStateFlow(false)

        val myAppPurchaseLogic = TestAppPurchaseLogicWithCallbacks(
            customPurchaseCalled,
            null,
            PurchaseLogicResult.Error(),
            null,
        )

        val model = create(
            customPurchaseLogic = myAppPurchaseLogic,
        )

        model.purchaseSelectedPackage(activity)

        customPurchaseCalled.first { it }

        coVerify(exactly = 0) { purchases.syncPurchases() }
        coVerify(exactly = 0) { listener.onPurchaseStarted(any()) }
        coVerify(exactly = 0) { listener.onPurchaseError(any()) }
    }

    // Suspend (co-routine) Tests

    @Test
    fun `Custom suspend restore purchases logic success triggers syncPurchases`() = runTest {
        every { purchases.purchasesAreCompletedBy } returns PurchasesAreCompletedBy.MY_APP

        val customRestoreCalled = MutableStateFlow(false)

        val myAppPurchaseLogic = TestAppPurchaseLogicWithSuspend(
            null,
            customRestoreCalled,
            null,
            PurchaseLogicResult.Success
        )

        val model = create(
            customPurchaseLogic = myAppPurchaseLogic,
        )

        model.restorePurchases()

        customRestoreCalled.first { it }

        coVerify(exactly = 1) { purchases.syncPurchases() }
        coVerify(exactly = 0) { listener.onRestoreStarted() }
        coVerify(exactly = 0) { listener.onRestoreCompleted(customerInfo) }

    }

    @Test
    fun `Custom suspend restore purchases logic error does not trigger syncPurchases`() = runTest {
        every { purchases.purchasesAreCompletedBy } returns PurchasesAreCompletedBy.MY_APP
        val customRestoreCalled = MutableStateFlow(false)

        val myAppPurchaseLogic = TestAppPurchaseLogicWithSuspend(
            null,
            customRestoreCalled,
            null,
            PurchaseLogicResult.Error()
        )

        val model = create(
            customPurchaseLogic = myAppPurchaseLogic,
        )

        model.restorePurchases()

        customRestoreCalled.first { it }

        coVerify(exactly = 0) { purchases.syncPurchases() }
        coVerify(exactly = 0) { listener.onRestoreStarted() }
        coVerify(exactly = 0) { listener.onRestoreError(any()) }

        assertThat(model.actionInProgress.value).isFalse
    }

    @Test
    fun `Custom suspend purchase logic success triggers syncPurchases`() = runTest {
        every { purchases.purchasesAreCompletedBy } returns PurchasesAreCompletedBy.MY_APP

        val customPurchaseCalled = MutableStateFlow(false)

        val myAppPurchaseLogic = TestAppPurchaseLogicWithSuspend(
            customPurchaseCalled,
            null,
            PurchaseLogicResult.Success,
            null
        )

        val model = create(
            customPurchaseLogic = myAppPurchaseLogic,
        )

        model.purchaseSelectedPackage(activity)

        customPurchaseCalled.first { it }

        coVerify(exactly = 1)  { purchases.syncPurchases() }
        coVerify(exactly = 0) { listener.onPurchaseStarted(any()) }
        coVerify(exactly = 0) { listener.onPurchaseCompleted(customerInfo, any()) }

        assertThat(model.actionInProgress.value).isFalse
        assertThat(dismissInvoked).isTrue
    }

    @Test
    fun `Custom suspend purchase logic cancelled doesn't trigger syncPurchases`() = runTest {
        every { purchases.purchasesAreCompletedBy } returns PurchasesAreCompletedBy.MY_APP

        val customPurchaseCalled = MutableStateFlow(false)

        val myAppPurchaseLogic = TestAppPurchaseLogicWithSuspend(
            customPurchaseCalled,
            null,
            PurchaseLogicResult.Cancellation,
            null
        )

        val model = create(
            customPurchaseLogic = myAppPurchaseLogic,
        )

        model.purchaseSelectedPackage(activity)

        customPurchaseCalled.first { it }

        coVerify(exactly = 0) { purchases.syncPurchases() }
        coVerify(exactly = 0) { listener.onPurchaseStarted(any()) }
        coVerify(exactly = 0) { listener.onPurchaseCancelled() }

        assertThat(model.actionInProgress.value).isFalse
    }

    @Test
    fun `Custom suspend purchase logic error doesn't trigger syncPurchases`() = runTest {
        every { purchases.purchasesAreCompletedBy } returns PurchasesAreCompletedBy.MY_APP

        val customPurchaseCalled = MutableStateFlow(false)

        val myAppPurchaseLogic = TestAppPurchaseLogicWithSuspend(
            customPurchaseCalled,
            null,
            PurchaseLogicResult.Error(),
            null
        )

        val model = create(
            customPurchaseLogic = myAppPurchaseLogic,
        )

        model.purchaseSelectedPackage(activity)

        customPurchaseCalled.first { it }

        coVerify(exactly = 0) { purchases.syncPurchases() }
        coVerify(exactly = 0) { listener.onPurchaseStarted(any()) }
        coVerify(exactly = 0) { listener.onPurchaseError(any()) }

        assertThat(model.actionInProgress.value).isFalse
    }

    @Test
    fun `Initial state is correct`() {
        delayFetchingOfferings()

        val model = create()

        when (model.state.value) {
            is PaywallState.Loading -> {}
            is PaywallState.Error,
            is PaywallState.Loaded,
            -> fail("Invalid state")
        }

        assertThat(model.actionInProgress.value).isFalse
        assertThat(dismissInvoked).isFalse
    }

    @Test
    fun `updateState does not update if same state`() {
        val options = PaywallOptions.Builder(dismissRequest = { dismissInvoked = true })
            .setListener(listener)
            .build()
        val model = PaywallViewModelImpl(
            MockResourceProvider(),
            purchases,
            options,
            TestData.Constants.currentColorScheme,
            isDarkMode = false,
            shouldDisplayBlock = null,
        )
        coVerify(exactly = 1) { purchases.awaitOfferings() }
        model.updateOptions(options)
        coVerify(exactly = 1) { purchases.awaitOfferings() }
    }

    @Test
    fun `updateState does update if different state`() {
        val options1 = PaywallOptions.Builder(dismissRequest = { dismissInvoked = true })
            .setListener(listener)
            .build()
        val options2 = PaywallOptions.Builder(dismissRequest = { dismissInvoked = true })
            .setListener(object : PaywallListener {})
            .build()
        val model = PaywallViewModelImpl(
            MockResourceProvider(),
            purchases,
            options1,
            TestData.Constants.currentColorScheme,
            isDarkMode = false,
            shouldDisplayBlock = null,
        )
        coVerify(exactly = 1) { purchases.awaitOfferings() }
        model.updateOptions(options1)
        coVerify(exactly = 1) { purchases.awaitOfferings() }
        model.updateOptions(options2)
        coVerify(exactly = 2) { purchases.awaitOfferings() }
    }

    @Test
    fun `Should load default offering`() {
        val model = create(
            activeSubscriptions = setOf(TestData.Packages.monthly.product.id),
            nonSubscriptionTransactionProductIdentifiers = setOf(TestData.Packages.lifetime.product.id)
        )

        coVerify { purchases.awaitOfferings() }

        val state = model.state.value
        if (state !is PaywallState.Loaded.Legacy) {
            fail("Invalid state")
            return
        }

        val expectedPaywall = defaultOffering.paywall!!

        verifyPaywall(state, expectedPaywall)
        assertThat(state.templateConfiguration.packages.packageIsCurrentlySubscribed(TestData.Packages.monthly))
            .isTrue
        assertThat(state.templateConfiguration.packages.packageIsCurrentlySubscribed(TestData.Packages.annual))
            .isFalse
        assertThat(state.templateConfiguration.packages.packageIsCurrentlySubscribed(TestData.Packages.lifetime))
            .isTrue
    }

    @Test
    fun `Error loading offerings`() {
        coEvery { purchases.awaitOfferings() } throws PurchasesException(
            PurchasesError(PurchasesErrorCode.NetworkError
        ))

        val model = create(
            activeSubscriptions = setOf(TestData.Packages.monthly.product.id),
            nonSubscriptionTransactionProductIdentifiers = setOf(TestData.Packages.lifetime.product.id)
        )

        coVerify { purchases.awaitOfferings() }

        val state = model.state.value
        if (state !is PaywallState.Error) {
            fail("Invalid state")
            return
        }

        assertThat(state.errorMessage).isEqualTo("Error 10: Error performing request.")
    }

    @Test
    fun `Error loading empty offerings`() {
        coEvery { purchases.awaitOfferings() } returns Offerings(
            null,
            mapOf(),
        )

        val model = create(
            activeSubscriptions = setOf(TestData.Packages.monthly.product.id),
            nonSubscriptionTransactionProductIdentifiers = setOf(TestData.Packages.lifetime.product.id)
        )

        coVerify { purchases.awaitOfferings() }

        val state = model.state.value
        if (state !is PaywallState.Error) {
            fail("Invalid state")
            return
        }

        assertThat(state.errorMessage).isEqualTo("The RevenueCat dashboard does not have a current offering configured.")
    }

    @Test
    fun `Should load selected offering`() {
        val offering = TestData.template1Offering
        val model = create(offering = offering)

        coVerify(exactly = 0) { purchases.awaitOfferings() }

        val state = model.state.value
        if (state !is PaywallState.Loaded.Legacy) {
            fail("Invalid state")
            return
        }

        val expectedPaywall = offering.paywall!!

        verifyPaywall(state, expectedPaywall)
    }

    @Test
    fun `Should load paywall components if using components paywall in full screen mode`(): Unit = runBlocking {
        // Arrange
        val offering = Offering(
            identifier = "offering-id",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly, TestData.Packages.annual),
            paywallComponents = Offering.PaywallComponents(UiConfig(), emptyPaywallComponentsData),
        )

        // Act
        val model = create(offering = offering, mode = PaywallMode.FULL_SCREEN)

        // Assert
        assertThat(model.state.value).isInstanceOf(PaywallState.Loaded.Components::class.java)
    }

    @Test
    fun `Should load fallback paywall if using components paywall in footer mode`(): Unit = runBlocking {
        // Arrange
        val offering = Offering(
            identifier = "offering-id",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly, TestData.Packages.annual),
            paywallComponents = Offering.PaywallComponents(UiConfig(), emptyPaywallComponentsData),
        )

        // Act
        val model = create(offering = offering, mode = PaywallMode.FOOTER)

        // Assert
        assertThat(model.state.value).isInstanceOf(PaywallState.Loaded.Legacy::class.java)
        assertThat(
            (model.state.value as PaywallState.Loaded.Legacy).templateConfiguration.packages.all.size
        ).isEqualTo(2)
    }

    @Test
    fun `Should load fallback paywall if using components paywall in footer condensed mode`(): Unit = runBlocking {
        // Arrange
        val offering = Offering(
            identifier = "offering-id",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly, TestData.Packages.annual),
            paywallComponents = Offering.PaywallComponents(UiConfig(), emptyPaywallComponentsData),
        )

        // Act
        val model = create(offering = offering, mode = PaywallMode.FOOTER_CONDENSED)

        // Assert
        assertThat(model.state.value).isInstanceOf(PaywallState.Loaded.Legacy::class.java)
        assertThat(
            (model.state.value as PaywallState.Loaded.Legacy).templateConfiguration.packages.all.size
        ).isEqualTo(2)
    }

    @Test
    fun `selectPackage`() {
        val model = create()

        val state = model.state.value
        if (state !is PaywallState.Loaded.Legacy) {
            fail("Invalid state")
            return
        }

        val packageToSelect = state.templateConfiguration.packages.all[0]
        assertThat(packageToSelect).isNotEqualTo(state.selectedPackage.value)

        model.selectPackage(packageToSelect)
        assertThat(state.selectedPackage.value).isSameAs(packageToSelect)
    }

    @Test
    fun `purchasePackage`() {
        val model = create()

        val state = model.state.value
        if (state !is PaywallState.Loaded.Legacy) {
            fail("Invalid state")
            return
        }

        val transaction = mockk<StoreTransaction>()
        val selectedPackage = state.selectedPackage.value

        coEvery {
            purchases.awaitPurchase(any())
        } returns PurchaseResult(transaction, customerInfo)

        assertThat(dismissInvoked).isFalse

        model.purchaseSelectedPackage(activity)

        coVerify {
            purchases.awaitPurchase(any())
        }

        verifyOrder {
            listener.onPurchaseStarted(selectedPackage.rcPackage)
            listener.onPurchaseCompleted(customerInfo, transaction)
        }

        assertThat(model.actionInProgress.value).isFalse
        assertThat(dismissInvoked).isTrue
    }

    @Test
    fun handlePackagePurchase(): Unit = runBlocking {
        // Arrange
        val offering = Offering(
            identifier = "offering-id",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly, TestData.Packages.annual),
            paywallComponents = Offering.PaywallComponents(UiConfig(), emptyPaywallComponentsData),
        )
        val model = create(offering = offering)
        val state = model.state.value as PaywallState.Loaded.Components
        state.update(selectedPackage = TestData.Packages.monthly)
        val selectedPackage = state.selectedPackageInfo?.rcPackage ?: error("selectedPackage is null")
        val transaction = mockk<StoreTransaction>()
        coEvery {
            purchases.awaitPurchase(any())
        } returns PurchaseResult(transaction, customerInfo)
        assertThat(dismissInvoked).isFalse

        // Act
        model.handlePackagePurchase(activity)

        // Assert
        coVerify { purchases.awaitPurchase(any()) }
        verifyOrder {
            listener.onPurchaseStarted(selectedPackage)
            listener.onPurchaseCompleted(customerInfo, transaction)
        }
        assertThat(model.actionInProgress.value).isFalse
        assertThat(dismissInvoked).isTrue
    }

    @Test
    fun `purchasePackage fails`() {
        val model = create()

        val state = model.state.value
        if (state !is PaywallState.Loaded.Legacy) {
            fail("Invalid state")
            return
        }

        val selectedPackage = state.selectedPackage.value
        val expectedError = PurchasesError(PurchasesErrorCode.ProductNotAvailableForPurchaseError)

        coEvery {
            purchases.awaitPurchase(any())
        } throws PurchasesException(expectedError)

        model.purchaseSelectedPackage(activity)

        coVerify {
            purchases.awaitPurchase(any())
        }

        verifyOrder {
            listener.onPurchaseStarted(selectedPackage.rcPackage)
            listener.onPurchaseError(expectedError)
        }

        assertThat(model.actionInProgress.value).isFalse
        assertThat(model.actionError.value).isEqualTo(expectedError)
    }

    @Test
    fun `handlePackagePurchase fails`(): Unit = runBlocking {
        // Arrange
        val offering = Offering(
            identifier = "offering-id",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly, TestData.Packages.annual),
            paywallComponents = Offering.PaywallComponents(UiConfig(), emptyPaywallComponentsData),
        )
        val model = create(offering = offering)
        val state = model.state.value as PaywallState.Loaded.Components
        state.update(selectedPackage = TestData.Packages.monthly)
        val selectedPackage = state.selectedPackageInfo?.rcPackage ?: error("selectedPackage is null")
        val expectedError = PurchasesError(PurchasesErrorCode.ProductNotAvailableForPurchaseError)

        coEvery {
            purchases.awaitPurchase(any())
        } throws PurchasesException(expectedError)

        // Act
        model.handlePackagePurchase(activity)

        // Assert
        coVerify { purchases.awaitPurchase(any()) }
        verifyOrder {
            listener.onPurchaseStarted(selectedPackage)
            listener.onPurchaseError(expectedError)
        }
        assertThat(model.actionInProgress.value).isFalse
        assertThat(model.actionError.value).isEqualTo(expectedError)
    }

    @Test
    fun `restorePurchases`() {
        val model = create()

        val state = model.state.value
        if (state !is PaywallState.Loaded.Legacy) {
            fail("Invalid state")
            return
        }

        coEvery {
            purchases.awaitRestore()
        } returns customerInfo

        model.restorePurchases()

        coVerify {
            purchases.awaitRestore()
        }

        verifyOrder {
            listener.onRestoreStarted()
            listener.onRestoreCompleted(customerInfo)
        }

        assertThat(model.actionInProgress.value).isFalse
        assertThat(dismissInvoked).isFalse
    }

    @Test
    fun handleRestorePurchases(): Unit = runBlocking {
        // Arrange
        val offering = Offering(
            identifier = "offering-id",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly, TestData.Packages.annual),
            paywallComponents = Offering.PaywallComponents(UiConfig(), emptyPaywallComponentsData),
        )
        val model = create(offering = offering)
        assertThat(model.state.value).isInstanceOf(PaywallState.Loaded.Components::class.java)
        coEvery {
            purchases.awaitRestore()
        } returns customerInfo

        // Act
        model.handleRestorePurchases()

        // Assert
        coVerify { purchases.awaitRestore() }
        verifyOrder {
            listener.onRestoreStarted()
            listener.onRestoreCompleted(customerInfo)
        }
        assertThat(model.actionInProgress.value).isFalse
        assertThat(dismissInvoked).isFalse
    }

    @Test
    fun `restorePurchases calls onDismiss if shouldDisplayBlock condition false`() {
        val model = create {
            false
        }

        coEvery {
            purchases.awaitRestore()
        } returns customerInfo

        model.restorePurchases()

        assertThat(dismissInvoked).isTrue()
    }

    @Test
    fun `handleRestorePurchases calls onDismiss if shouldDisplayBlock condition false`(): Unit = runBlocking {
        // Arrange
        val offering = Offering(
            identifier = "offering-id",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly, TestData.Packages.annual),
            paywallComponents = Offering.PaywallComponents(UiConfig(), emptyPaywallComponentsData),
        )
        val model = create(
            offering = offering,
            shouldDisplayBlock = { false }
        )
        coEvery {
            purchases.awaitRestore()
        } returns customerInfo

        // Act
        model.handleRestorePurchases()

        // Assert
        assertThat(dismissInvoked).isTrue()
    }

    @Test
    fun `restorePurchases does not call onDismiss if shouldDisplayBlock condition true`() {
        val model = create {
            true
        }

        coEvery {
            purchases.awaitRestore()
        } returns customerInfo

        model.restorePurchases()

        assertThat(dismissInvoked).isFalse()
    }

    @Test
    fun `handleRestorePurchases does not call onDismiss if shouldDisplayBlock condition true`(): Unit = runBlocking {
        // Arrange
        val offering = Offering(
            identifier = "offering-id",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly, TestData.Packages.annual),
            paywallComponents = Offering.PaywallComponents(UiConfig(), emptyPaywallComponentsData),
        )
        val model = create(
            offering = offering,
            shouldDisplayBlock = { true }
        )
        coEvery {
            purchases.awaitRestore()
        } returns customerInfo

        // Act
        model.handleRestorePurchases()

        // Assert
        assertThat(dismissInvoked).isFalse()
    }


    @Test
    fun `restorePurchases fails`() {
        val model = create()

        val state = model.state.value
        if (state !is PaywallState.Loaded.Legacy) {
            fail("Invalid state")
            return
        }

        val expectedError = PurchasesError(PurchasesErrorCode.NetworkError)

        coEvery {
            purchases.awaitRestore()
        } throws PurchasesException(expectedError)

        model.restorePurchases()

        coVerify {
            purchases.awaitRestore()
        }

        verifyOrder {
            listener.onRestoreStarted()
            listener.onRestoreError(expectedError)
        }

        assertThat(model.actionInProgress.value).isFalse
        assertThat(model.actionError.value).isEqualTo(expectedError)
    }

    @Test
    fun `handleRestorePurchases fails`(): Unit = runBlocking {
        // Arrange
        val offering = Offering(
            identifier = "offering-id",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly, TestData.Packages.annual),
            paywallComponents = Offering.PaywallComponents(UiConfig(), emptyPaywallComponentsData),
        )
        val model = create(offering = offering)
        assertThat(model.state.value).isInstanceOf(PaywallState.Loaded.Components::class.java)
        val expectedError = PurchasesError(PurchasesErrorCode.NetworkError)
        coEvery {
            purchases.awaitRestore()
        } throws PurchasesException(expectedError)

        // Act
        model.handleRestorePurchases()

        // Assert
        coVerify { purchases.awaitRestore() }
        verifyOrder {
            listener.onRestoreStarted()
            listener.onRestoreError(expectedError)
        }
        assertThat(model.actionInProgress.value).isFalse
        assertThat(model.actionError.value).isEqualTo(expectedError)
    }

    @Test
    fun `clearActionError`() {
        val model = create()

        val state = model.state.value
        if (state !is PaywallState.Loaded.Legacy) {
            fail("Invalid state")
            return
        }

        coEvery {
            purchases.awaitRestore()
        } throws PurchasesException(PurchasesError(PurchasesErrorCode.NetworkError))

        model.restorePurchases()

        assertThat(model.actionError.value).isNotNull
        model.clearActionError()
        assertThat(model.actionError.value).isNull()
    }

    @Test
    fun `close button pressed`() {
        val model = create()

        assertThat(dismissInvoked).isFalse
        model.closePaywall()
        assertThat(dismissInvoked).isTrue
    }

    // region events

    @Test
    fun `trackPaywallImpression tracks event with correct data`() {
        val model = create()
        model.trackPaywallImpressionIfNeeded()
        verifyEventTracked(PaywallEventType.IMPRESSION, 1)
    }

    @Test
    fun `trackPaywallImpression multiple times in a row only tracks once`() {
        val model = create()
        model.trackPaywallImpressionIfNeeded()
        model.trackPaywallImpressionIfNeeded()
        model.trackPaywallImpressionIfNeeded()
        verify(exactly = 1) {
            purchases.track(any())
        }
    }

    @Test
    fun `trackPaywallImpression after close tracks again`() {
        val model = create()
        model.trackPaywallImpressionIfNeeded()
        model.closePaywall()
        model.trackPaywallImpressionIfNeeded()
        verifyEventTracked(PaywallEventType.IMPRESSION, 2)
    }

    @Test
    fun `close tracks close event`() {
        val model = create()
        model.trackPaywallImpressionIfNeeded()
        model.closePaywall()
        verifyEventTracked(PaywallEventType.CLOSE, 1)
    }

    @Test
    fun `close tracks close event only once before another impression`() {
        val model = create()
        model.trackPaywallImpressionIfNeeded()
        model.closePaywall()
        model.closePaywall()
        model.closePaywall()
        verify(exactly = 1) {
            purchases.track(
                withArg { event ->
                    val paywallEvent = event as? PaywallEvent
                        ?: error("Expected PaywallEvent but got ${event::class.simpleName}")

                    assertThat(paywallEvent.data.offeringIdentifier).isEqualTo(defaultOffering.identifier)
                    assertThat(paywallEvent.data.paywallRevision).isEqualTo(defaultOffering.paywall!!.revision)
                    assertThat(paywallEvent.data.displayMode).isEqualTo("full_screen")
                    assertThat(paywallEvent.data.darkMode).isFalse
                    assertThat(paywallEvent.type).isEqualTo(PaywallEventType.CLOSE)
                }
            )
        }
        model.trackPaywallImpressionIfNeeded()
        model.closePaywall()
        verifyEventTracked(PaywallEventType.CLOSE, 2)
    }

    @Test
    fun `purchase cancellation tracks cancel event`() {
        val model = create()
        model.trackPaywallImpressionIfNeeded()
        val expectedError = PurchasesError(PurchasesErrorCode.PurchaseCancelledError)
        coEvery {
            purchases.awaitPurchase(any())
        } throws PurchasesException(expectedError)

        model.purchaseSelectedPackage(activity)

        verifyEventTracked(PaywallEventType.CANCEL, 1)

        assertThat(model.actionError.value).isNull()
        verify(exactly = 0) {
            listener.onPurchaseError(any())
        }
        verify(exactly = 1) {
            listener.onPurchaseCancelled()
        }
    }

    @Test
    fun `handlePackagePurchase cancellation tracks cancel event`(): Unit = runBlocking {
        // Arrange
        val offering = Offering(
            identifier = "offering-id",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly, TestData.Packages.annual),
            paywallComponents = Offering.PaywallComponents(UiConfig(), emptyPaywallComponentsData),
        )
        val model = create(offering = offering).apply {
            val state = state.value as PaywallState.Loaded.Components
            state.update(selectedPackage = TestData.Packages.monthly)
            trackPaywallImpressionIfNeeded()
        }
        val expectedError = PurchasesError(PurchasesErrorCode.PurchaseCancelledError)
        coEvery {
            purchases.awaitPurchase(any())
        } throws PurchasesException(expectedError)

        // Act
        model.handlePackagePurchase(activity)

        // Assert
        verifyEventTracked(
            eventType = PaywallEventType.CANCEL,
            times = 1,
            offeringIdentifier = offering.identifier,
            paywallRevision = offering.paywallComponents!!.data.revision
        )
        assertThat(model.actionError.value).isNull()
        verify(exactly = 0) { listener.onPurchaseError(any()) }
        verify(exactly = 1) { listener.onPurchaseCancelled() }
    }

    @Test
    fun `purchase errors other than cancellation do not track cancel event`() {
        val model = create()
        model.trackPaywallImpressionIfNeeded()
        val expectedError = PurchasesError(PurchasesErrorCode.StoreProblemError)
        coEvery {
            purchases.awaitPurchase(any())
        } throws PurchasesException(expectedError)

        model.purchaseSelectedPackage(activity)

        verifyNoEventsOfTypeTracked(PaywallEventType.CANCEL)
    }

    @Test
    fun `handlePackagePurchase errors other than cancellation do not track cancel event`(): Unit = runBlocking {
        // Arrange
        val offering = Offering(
            identifier = "offering-id",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly, TestData.Packages.annual),
            paywallComponents = Offering.PaywallComponents(UiConfig(), emptyPaywallComponentsData),
        )
        val model = create(offering = offering)
        model.trackPaywallImpressionIfNeeded()
        val expectedError = PurchasesError(PurchasesErrorCode.StoreProblemError)
        coEvery {
            purchases.awaitPurchase(any())
        } throws PurchasesException(expectedError)

        // Act
        model.handlePackagePurchase(activity)

        // Assert
        verifyNoEventsOfTypeTracked(PaywallEventType.CANCEL)
    }

    @Test
    fun `trackPaywallImpression does nothing if state is loading`() {
        delayFetchingOfferings()
        val model = create()
        model.trackPaywallImpressionIfNeeded()
        assertThat(model.state.value).isInstanceOf(PaywallState.Loading::class.java)
        verify(exactly = 0) { purchases.track(any()) }
    }

    @Test
    fun `trackPaywallImpression does nothing if offering does not have a paywall`() {
        val model = create(offering = defaultOffering.copy(paywall = null))
        model.trackPaywallImpressionIfNeeded()
        verify(exactly = 0) { purchases.track(any()) }
    }

    // endregion events

    private fun create(
        offering: Offering? = null,
        activeSubscriptions: Set<String> = setOf(),
        nonSubscriptionTransactionProductIdentifiers: Set<String> = setOf(),
        customPurchaseLogic: PurchaseLogic? = null,
        mode: PaywallMode = PaywallMode.default,
        shouldDisplayBlock: ((CustomerInfo) -> Boolean)? = null,
    ): PaywallViewModelImpl {
        mockActiveSubscriptions(activeSubscriptions)
        mockNonSubscriptionTransactions(nonSubscriptionTransactionProductIdentifiers)

        return PaywallViewModelImpl(
            MockResourceProvider(),
            purchases,
            PaywallOptions.Builder(dismissRequest = { dismissInvoked = true })
                .setListener(listener)
                .setOffering(offering)
                .setPurchaseLogic(customPurchaseLogic)
                .setMode(mode)
                .build(),
            TestData.Constants.currentColorScheme,
            isDarkMode = false,
            shouldDisplayBlock = shouldDisplayBlock,
        )
    }

    private fun mockActiveSubscriptions(subscriptions: Set<String>) {
        every { customerInfo.activeSubscriptions } returns subscriptions
    }

    private fun mockNonSubscriptionTransactions(productIdentifiers: Set<String>) {
        every { customerInfo.nonSubscriptionTransactions } returns productIdentifiers
            .map { productIdentifier ->
                Transaction(
                    transactionIdentifier = UUID.randomUUID().toString(),
                    revenuecatId = UUID.randomUUID().toString(),
                    productIdentifier = productIdentifier,
                    productId = productIdentifier,
                    purchaseDate = Date(),
                    storeTransactionId = UUID.randomUUID().toString(),
                    store = Store.PLAY_STORE,
                    displayName = "Product $productIdentifier",
                    isSandbox = false,
                    originalPurchaseDate = Date(),
                    price = (1..100).random().toDouble().let {
                        Price("$it", it.toLong() * 1_000_000, "USD")
                    },
                )
            }
    }

    /**
     * Note: this is O(n), for testing only
     */
    private fun TemplateConfiguration.PackageConfiguration.packageIsCurrentlySubscribed(
        rcPackage: Package,
    ): Boolean {
        return all.first { it.rcPackage.identifier == rcPackage.identifier }.currentlySubscribed
    }

    private fun verifyPaywall(
        state: PaywallState.Loaded.Legacy,
        expectedPaywall: PaywallData,
    ) {
        assertThat(state.selectedPackage.value.rcPackage.identifier)
            .isEqualTo(expectedPaywall.config.defaultPackage)
        assertThat(state.templateConfiguration.template.id).isEqualTo(expectedPaywall.templateName)
        assertThat(state.templateConfiguration.mode).isEqualTo(PaywallMode.FULL_SCREEN)
        assertThat(state.templateConfiguration.packages.all).hasSameSizeAs(expectedPaywall.config.packageIds)
    }

    private fun verifyEventTracked(
        eventType: PaywallEventType,
        times: Int,
        offeringIdentifier: String = defaultOffering.identifier,
        paywallRevision: Int = defaultOffering.paywall!!.revision,
    ) {
        verify(exactly = times) {
            purchases.track(
                withArg { event ->
                    val paywallEvent = event as? PaywallEvent
                        ?: error("Expected PaywallEvent but got ${event::class.simpleName}")

                    assertThat(paywallEvent.data.offeringIdentifier).isEqualTo(offeringIdentifier)
                    assertThat(paywallEvent.data.paywallRevision).isEqualTo(paywallRevision)
                    assertThat(paywallEvent.data.displayMode).isEqualTo("full_screen")
                    assertThat(paywallEvent.data.darkMode).isFalse
                    assertThat(paywallEvent.type).isEqualTo(eventType)
                }
            )
        }
    }

    private fun verifyNoEventsOfTypeTracked(eventType: PaywallEventType) {
        verify(exactly = 0) {
            purchases.track(
                withArg { event ->
                    val paywallEvent = event as? PaywallEvent
                        ?: error("Expected PaywallEvent but got ${event::class.simpleName}")
                    assertThat(paywallEvent.type).isEqualTo(eventType)
                }
            )
        }
    }

    private fun delayFetchingOfferings() {
        coEvery { purchases.awaitOfferings() } coAnswers {
            // Delay response to verify initial state
            delay(100)
            offerings
        }
    }

    private class TestAppPurchaseLogicWithCallbacks(
        private val customPurchaseCalled: MutableStateFlow<Boolean>? = null,
        private val customRestoreCalled: MutableStateFlow<Boolean>? = null,
        private val purchaseResult: PurchaseLogicResult? = null,
        private val restoreResult: PurchaseLogicResult? = null
    ) :  PurchaseLogicWithCallback() {

        override fun performPurchaseWithCompletion(activity: Activity,
            rcPackage: Package,
            completion: (PurchaseLogicResult) -> Unit
        ) {
            val purchaseFlow = customPurchaseCalled
                ?: throw IllegalArgumentException("customPurchaseCalled cannot be null")
            val result = purchaseResult
                ?: throw IllegalArgumentException("purchaseResult cannot be null")

            purchaseFlow.value = true
            completion(result)
        }

        override fun performRestoreWithCompletion(
            customerInfo: CustomerInfo,
            completion: (PurchaseLogicResult) -> Unit
        ) {
            val restoreFlow = customRestoreCalled
                ?: throw IllegalArgumentException("customRestoreCalled cannot be null")
            val result = restoreResult
                ?: throw IllegalArgumentException("restoreResult cannot be null")

            restoreFlow.value = true

            completion(result)
        }
    }

    private class TestAppPurchaseLogicWithSuspend(
        private val customPurchaseCalled: MutableStateFlow<Boolean>? = null,
        private val customRestoreCalled: MutableStateFlow<Boolean>? = null,
        private val purchaseResult: PurchaseLogicResult? = null,
        private val restoreResult: PurchaseLogicResult? = null
    ) : PurchaseLogic {

        override suspend fun performPurchase(activity: Activity, rcPackage: Package): PurchaseLogicResult {
            val purchaseFlow = customPurchaseCalled
                ?: throw IllegalArgumentException("customPurchaseCalled cannot be null")
            val result = purchaseResult
                ?: throw IllegalArgumentException("purchaseResult cannot be null")

            purchaseFlow.value = true
            return result
        }

        override suspend fun performRestore(customerInfo: CustomerInfo): PurchaseLogicResult {
            val restoreFlow = customRestoreCalled
                ?: throw IllegalArgumentException("customRestoreCalled cannot be null")
            val result = restoreResult
                ?: throw IllegalArgumentException("restoreResult cannot be null")

            restoreFlow.value = true
            return result
        }
    }
}
