package com.revenuecat.purchases.ui.revenuecatui.data

import android.app.Activity
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
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
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.models.Transaction
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.paywalls.events.PaywallEventType
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.MyAppPurchaseLogic
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallMode
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
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
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import java.util.UUID

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class, ExperimentalPreviewRevenueCatPurchasesAPI::class)
@RunWith(AndroidJUnit4::class)
class PaywallViewModelTest {
    private val defaultOffering = TestData.template2Offering

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

    @Before //james
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

        every { purchases.track(any()) } just Runs

        every { listener.onPurchaseStarted(any()) } just runs
        every { listener.onPurchaseCompleted(any(), any()) } just runs
        every { listener.onPurchaseError(any()) } just runs
        every { listener.onRestoreStarted() } just runs
        every { listener.onRestoreCompleted(any()) } just runs
        every { listener.onRestoreError(any()) } just runs
    }

    @After
    internal fun tearDown() {
        clearAllMocks()
    }
    @Test
    fun `Purchases will complete purchases using my app`() {
        every { purchases.purchasesAreCompletedBy } returns PurchasesAreCompletedBy.MY_APP

        assert(purchases.purchasesAreCompletedBy == PurchasesAreCompletedBy.MY_APP)
    }

    @Test
    fun `Calls custom restore logic when purchasesAreCompletedBy == MY_APP`() = runTest {
        every { purchases.purchasesAreCompletedBy } returns PurchasesAreCompletedBy.MY_APP

        val myAppPurchaseLogic = mockk<MyAppPurchaseLogic>(relaxed = true)

        coEvery { myAppPurchaseLogic.performRestore(any()) } just runs

        val model = create(
            customPurchaseLogic = myAppPurchaseLogic,
            activeSubscriptions = setOf(TestData.Packages.monthly.product.id),
            nonSubscriptionTransactionProductIdentifiers = setOf(TestData.Packages.lifetime.product.id)
        )

        model.awaitRestorePurchases()

        coVerify { myAppPurchaseLogic.performRestore(any()) }
    }

    @Test
    fun `Calls custom purchase logic when purchasesAreCompletedBy == MY_APP`() = runTest {
        // Arrange
        every { purchases.purchasesAreCompletedBy } returns PurchasesAreCompletedBy.MY_APP

        val myAppPurchaseLogic = mockk<MyAppPurchaseLogic>(relaxed = true)

        coEvery { myAppPurchaseLogic.performPurchase(any(), any()) } just runs

        val model = create(
            customPurchaseLogic = myAppPurchaseLogic
        )

        model.awaitPurchaseSelectedPackage(activity)

        coVerify { myAppPurchaseLogic.performPurchase(any(), any()) }
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
        if (state !is PaywallState.Loaded) {
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
        if (state !is PaywallState.Loaded) {
            fail("Invalid state")
            return
        }

        val expectedPaywall = offering.paywall!!

        verifyPaywall(state, expectedPaywall)
    }

    @Test
    fun `selectPackage`() {
        val model = create()

        val state = model.state.value
        if (state !is PaywallState.Loaded) {
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
        if (state !is PaywallState.Loaded) {
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
    fun `purchasePackage fails`() {
        val model = create()

        val state = model.state.value
        if (state !is PaywallState.Loaded) {
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
    fun `restorePurchases`() {
        val model = create()

        val state = model.state.value
        if (state !is PaywallState.Loaded) {
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
    fun `restorePurchases fails`() {
        val model = create()

        val state = model.state.value
        if (state !is PaywallState.Loaded) {
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
    fun `clearActionError`() {
        val model = create()

        val state = model.state.value
        if (state !is PaywallState.Loaded) {
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
                withArg {
                    assertThat(it.data.offeringIdentifier).isEqualTo(defaultOffering.identifier)
                    assertThat(it.data.paywallRevision).isEqualTo(defaultOffering.paywall!!.revision)
                    assertThat(it.data.displayMode).isEqualTo("full_screen")
                    assertThat(it.data.darkMode).isFalse
                    assertThat(it.type).isEqualTo(PaywallEventType.CLOSE)
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
        customPurchaseLogic: MyAppPurchaseLogic? = null,
        shouldDisplayBlock: ((CustomerInfo) -> Boolean)? = null
    ): PaywallViewModelImpl {
        mockActiveSubscriptions(activeSubscriptions)
        mockNonSubscriptionTransactions(nonSubscriptionTransactionProductIdentifiers)

        return PaywallViewModelImpl(
            MockResourceProvider(),
            purchases,
            PaywallOptions.Builder(dismissRequest = { dismissInvoked = true })
                .setListener(listener)
                .setOffering(offering)
                .setMyAppPurchaseLogic(customPurchaseLogic)
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
            .map {
                Transaction(
                    UUID.randomUUID().toString(),
                    UUID.randomUUID().toString(),
                    it,
                    it,
                    Date()
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
        state: PaywallState.Loaded,
        expectedPaywall: PaywallData,
    ) {
        assertThat(state.selectedPackage.value.rcPackage.identifier)
            .isEqualTo(expectedPaywall.config.defaultPackage)
        assertThat(state.templateConfiguration.template.id).isEqualTo(expectedPaywall.templateName)
        assertThat(state.templateConfiguration.mode).isEqualTo(PaywallMode.FULL_SCREEN)
        assertThat(state.templateConfiguration.packages.all).hasSameSizeAs(expectedPaywall.config.packageIds)
    }

    private fun verifyEventTracked(eventType: PaywallEventType, times: Int) {
        verify(exactly = times) {
            purchases.track(
                withArg {
                    assertThat(it.data.offeringIdentifier).isEqualTo(defaultOffering.identifier)
                    assertThat(it.data.paywallRevision).isEqualTo(defaultOffering.paywall!!.revision)
                    assertThat(it.data.displayMode).isEqualTo("full_screen")
                    assertThat(it.data.darkMode).isFalse
                    assertThat(it.type).isEqualTo(eventType)
                }
            )
        }
    }

    private fun verifyNoEventsOfTypeTracked(eventType: PaywallEventType) {
        verify(exactly = 0) {
            purchases.track(
                withArg {
                    assertThat(it.type).isEqualTo(eventType)
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
}
