package com.revenuecat.purchases.ui.revenuecatui.data

import android.app.Activity
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseResult
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallMode
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockApplicationContext
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.extensions.getActivity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verifyOrder
import junit.framework.TestCase.fail
import kotlinx.coroutines.delay
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PaywallViewModelTest {
    private val defaultOffering = TestData.template2Offering

    private lateinit var purchases: PurchasesType
    private lateinit var customerInfo: CustomerInfo

    private lateinit var context: Context
    private lateinit var activity: Activity
    private lateinit var listener: PaywallListener

    private val offerings = Offerings(
        defaultOffering,
        mapOf(
            TestData.template1Offering.identifier to TestData.template1Offering,
            TestData.template2Offering.identifier to TestData.template2Offering
        )
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

        // Allows mocking Context.getActivity
        mockkStatic("com.revenuecat.purchases.ui.revenuecatui.extensions.ContextExtensionsKt")

        every { context.getActivity() } returns activity

        coEvery { purchases.awaitOfferings() } returns offerings
        coEvery { purchases.awaitCustomerInfo(any()) } returns customerInfo

        every { listener.onPurchaseStarted(any()) } just runs
        every { listener.onPurchaseCompleted(any(), any()) } just runs
        every { listener.onPurchaseError(any()) } just runs
        every { listener.onRestoreStarted() } just runs
        every { listener.onRestoreCompleted(any()) } just runs
        every { listener.onRestoreError(any()) } just runs
    }

    @Test
    fun `Initial state is correct`() {
        coEvery { purchases.awaitOfferings() } coAnswers {
            // Delay response to verify initial state
            delay(100)
            offerings
        }

        val model = create()

        when (model.state.value) {
            is PaywallState.Loading -> {}
            is PaywallState.Error,
            is PaywallState.Loaded,
            -> fail("Invalid state")
        }

        assertThat(model.actionInProgress.value).isFalse
    }

    @Test
    fun `Should load default offering`() {
        val model = create(activeSubscriptions = arrayOf(TestData.Packages.monthly.product.id))

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

        model.purchaseSelectedPackage(context)

        coVerify {
            purchases.awaitPurchase(any())
        }

        verifyOrder {
            listener.onPurchaseStarted(selectedPackage.rcPackage)
            listener.onPurchaseCompleted(customerInfo, transaction)
        }

        assertThat(model.actionInProgress.value).isFalse
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
    }

    private fun create(
        offering: Offering? = null,
        vararg activeSubscriptions: String,
    ): PaywallViewModelImpl {
        mockActiveSubscriptions(*activeSubscriptions)

        return PaywallViewModelImpl(
            MockApplicationContext(),
            purchases,
            PaywallOptions.Builder()
                .setListener(listener)
                .setOffering(offering)
                .build(),
            TestData.Constants.currentColorScheme
        )
    }

    private fun mockActiveSubscriptions(vararg subscriptions: String) {
        every { customerInfo.getActiveSubscriptions() } returns setOf(*subscriptions)
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
}
