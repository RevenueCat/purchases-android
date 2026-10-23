package com.revenuecat.purchases.ui.revenuecatui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.core.content.IntentCompat
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.revenuecat.purchases.DangerousSettings
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.common.workflows.WorkflowResolution
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.ui.revenuecatui.OfferingSelection
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModelImpl
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class PaywallActivityTest {

    private val mockPurchases = mockk<Purchases>()

    @Before
    fun setUp() {
        val offering = TestData.template1Offering
        val offerings = Offerings(current = offering, all = mapOf(offering.identifier to offering))
        mockkObject(Purchases)
        every { Purchases.sharedInstance } returns mockPurchases
        every { mockPurchases.purchasesAreCompletedBy } returns PurchasesAreCompletedBy.REVENUECAT
        every { mockPurchases.storefrontCountryCode } returns "US"
        every { mockPurchases.preferredUILocaleOverride } returns null
        every { mockPurchases.track(any()) } just Runs
        every { mockPurchases.currentConfiguration } returns mockk {
            every { dangerousSettings } returns DangerousSettings()
        }
        every { mockPurchases.getOfferings(any()) } answers {
            firstArg<ReceiveOfferingsCallback>().onReceived(offerings)
        }
        coEvery { mockPurchases.resolveWorkflow(any()) } returns WorkflowResolution.NoWorkflow
    }

    @After
    fun tearDown() {
        unmockkAll()
        PaywallActivityNonSerializableArgsStore.clear()
    }

    @Test
    fun `activity finishes gracefully when not launched through SDK`() {
        // Arrange - launch without SDK extras (simulating Google automated testing)
        val intent = Intent(
            ApplicationProvider.getApplicationContext<Context>(),
            PaywallActivity::class.java,
        )

        // Act - launch the activity (it should finish immediately in onCreate)
        val scenario = launchActivity<PaywallActivity>(intent)

        // Assert - activity should be destroyed (finished gracefully without crashing)
        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Test
    @Config(sdk = [24, 33, 34])
    fun `activity finishes gracefully when args extra is another Parcelable type`() {
        val intent = Intent(
            ApplicationProvider.getApplicationContext<Context>(),
            PaywallActivity::class.java,
        ).putExtra(PaywallActivity.ARGS_EXTRA, PaywallResult.Cancelled)

        val scenario = launchActivity<PaywallActivity>(intent)

        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Test
    @Config(sdk = [24, 33, 34])
    fun `activity reads args on every API level`() {
        val offeringSelection = OfferingSelection.IdAndPresentedOfferingContext(
            offeringId = TestData.template1Offering.identifier,
            presentedOfferingContext = null,
        )
        val args = PaywallActivityArgs(offeringIdAndPresentedOfferingContext = offeringSelection)
        val intent = Intent(
            ApplicationProvider.getApplicationContext<Context>(),
            PaywallActivity::class.java,
        ).putExtra(PaywallActivity.ARGS_EXTRA, args)
        val viewModelKey = PaywallOptions.Builder(dismissRequest = {})
            .setOfferingSelection(offeringSelection)
            .setShouldDisplayDismissButton(DEFAULT_DISPLAY_DISMISS_BUTTON)
            .build()
            .hashCode()
            .toString()

        val scenario = launchActivity<PaywallActivity>(intent)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        scenario.onActivity { activity ->
            assertThat(activity.isFinishing).isFalse()
            val viewModel = ViewModelProvider(activity).get(viewModelKey, PaywallViewModelImpl::class.java)
            assertThat(viewModel.state.value).isInstanceOf(PaywallState.Loaded::class.java)
        }
    }

    @Test
    fun `activity paywall resets to Loading only after activity is destroyed`() {
        val offeringSelection = OfferingSelection.IdAndPresentedOfferingContext(
            offeringId = TestData.template1Offering.identifier,
            presentedOfferingContext = null,
        )
        val args = PaywallActivityArgs(offeringIdAndPresentedOfferingContext = offeringSelection)
        val intent = Intent(
            ApplicationProvider.getApplicationContext<Context>(),
            PaywallActivity::class.java,
        ).putExtra(PaywallActivity.ARGS_EXTRA, args)
        val viewModelKey = PaywallOptions.Builder(dismissRequest = {})
            .setOfferingSelection(offeringSelection)
            .setShouldDisplayDismissButton(DEFAULT_DISPLAY_DISMISS_BUTTON)
            .build()
            .hashCode()
            .toString()
        lateinit var viewModel: PaywallViewModelImpl

        val scenario = launchActivity<PaywallActivity>(intent)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        scenario.onActivity { activity ->
            viewModel = ViewModelProvider(activity).get(viewModelKey, PaywallViewModelImpl::class.java)
            assertThat(viewModel.state.value).isInstanceOf(PaywallState.Loaded::class.java)

            viewModel.closePaywall()

            assertThat(activity.isFinishing).isTrue()
            assertThat(viewModel.state.value).isInstanceOf(PaywallState.Loaded::class.java)
        }

        scenario.moveToState(Lifecycle.State.DESTROYED)
        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        assertThat(viewModel.state.value).isEqualTo(PaywallState.Loading)
    }

    @Test
    fun `activity configuration change retains loaded paywall without reloading`() {
        val offeringSelection = OfferingSelection.IdAndPresentedOfferingContext(
            offeringId = TestData.template1Offering.identifier,
            presentedOfferingContext = null,
        )
        val args = PaywallActivityArgs(offeringIdAndPresentedOfferingContext = offeringSelection)
        val intent = Intent(
            ApplicationProvider.getApplicationContext<Context>(),
            PaywallActivity::class.java,
        ).putExtra(PaywallActivity.ARGS_EXTRA, args)
        val viewModelKey = PaywallOptions.Builder(dismissRequest = {})
            .setOfferingSelection(offeringSelection)
            .setShouldDisplayDismissButton(DEFAULT_DISPLAY_DISMISS_BUTTON)
            .build()
            .hashCode()
            .toString()
        lateinit var retainedViewModel: PaywallViewModelImpl
        lateinit var renderedState: PaywallState

        val scenario = launchActivity<PaywallActivity>(intent)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        scenario.onActivity { activity ->
            retainedViewModel = ViewModelProvider(activity).get(viewModelKey, PaywallViewModelImpl::class.java)
            renderedState = retainedViewModel.state.value
            assertThat(renderedState).isInstanceOf(PaywallState.Loaded::class.java)
        }

        scenario.recreate()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        scenario.onActivity { activity ->
            val recreatedViewModel = ViewModelProvider(activity).get(viewModelKey, PaywallViewModelImpl::class.java)
            assertThat(recreatedViewModel).isSameAs(retainedViewModel)
            assertThat(recreatedViewModel.state.value).isSameAs(renderedState)
        }
    }

    @Test
    fun `activity dismissed by back press returns cancelled result referencing non serializable args`() {
        val key = PaywallActivityNonSerializableArgsStore.store(
            PaywallActivityNonSerializableArgs(listener = mockk<PaywallListener>(relaxed = true)),
        )

        val scenario = launchActivity<PaywallActivity>(intentWithNonSerializableArgsKey(key))
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()

            assertThat(activity.isFinishing).isTrue()
            val shadowActivity = shadowOf(activity)
            assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_OK)
            val resultIntent = shadowActivity.resultIntent
            assertThat(
                IntentCompat.getParcelableExtra(resultIntent, PaywallActivity.RESULT_EXTRA, PaywallResult::class.java),
            ).isEqualTo(PaywallResult.Cancelled)
            assertThat(resultIntent.getIntExtra(PaywallActivity.NON_SERIALIZABLE_ARGS_KEY_EXTRA, 0)).isEqualTo(key)
        }
    }

    @Test
    fun `activity removes stored non serializable args when it finishes`() {
        val key = PaywallActivityNonSerializableArgsStore.store(
            PaywallActivityNonSerializableArgs(listener = mockk<PaywallListener>(relaxed = true)),
        )
        val scenario = launchActivity<PaywallActivity>(intentWithNonSerializableArgsKey(key))
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        scenario.onActivity { activity -> activity.onBackPressedDispatcher.onBackPressed() }
        scenario.moveToState(Lifecycle.State.DESTROYED)

        assertThat(PaywallActivityNonSerializableArgsStore.get(key)).isNull()
    }

    @Test
    fun `activity keeps stored non serializable args across recreation`() {
        val args = PaywallActivityNonSerializableArgs(listener = mockk<PaywallListener>(relaxed = true))
        val key = PaywallActivityNonSerializableArgsStore.store(args)
        val scenario = launchActivity<PaywallActivity>(intentWithNonSerializableArgsKey(key))
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        scenario.recreate()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertThat(PaywallActivityNonSerializableArgsStore.get(key)).isSameAs(args)
        scenario.onActivity { activity -> assertThat(activity.isFinishing).isFalse() }
    }

    @Test
    fun `exit offer args get their own stored non serializable args entry`() {
        val args = PaywallActivityNonSerializableArgs(listener = mockk<PaywallListener>(relaxed = true))
        val key = PaywallActivityNonSerializableArgsStore.store(args)
        val currentArgs = PaywallActivityArgs(requiredEntitlementIdentifier = "pro", nonSerializableArgsKey = key)

        val exitOfferArgs = PaywallActivity.createExitOfferArgs(currentArgs, TestData.template2Offering)

        val exitOfferKey = requireNotNull(exitOfferArgs.nonSerializableArgsKey)
        assertThat(exitOfferKey).isNotEqualTo(key)
        assertThat(exitOfferArgs.requiredEntitlementIdentifier).isEqualTo("pro")
        assertThat(exitOfferArgs.offeringIdAndPresentedOfferingContext?.offeringId)
            .isEqualTo(TestData.template2Offering.identifier)
        assertThat(PaywallActivityNonSerializableArgsStore.get(exitOfferKey)).isEqualTo(args)
        assertThat(PaywallActivityNonSerializableArgsStore.get(key)).isSameAs(args)
    }

    @Test
    fun `exit offer args carry no stored entry when the launching paywall has none`() {
        val currentArgs = PaywallActivityArgs(requiredEntitlementIdentifier = "pro")

        val exitOfferArgs = PaywallActivity.createExitOfferArgs(currentArgs, TestData.template2Offering)

        assertThat(exitOfferArgs.nonSerializableArgsKey).isNull()
    }

    private fun intentWithNonSerializableArgsKey(key: Int): Intent {
        val args = PaywallActivityArgs(
            offeringIdAndPresentedOfferingContext = OfferingSelection.IdAndPresentedOfferingContext(
                offeringId = TestData.template1Offering.identifier,
                presentedOfferingContext = null,
            ),
            nonSerializableArgsKey = key,
        )
        return Intent(
            ApplicationProvider.getApplicationContext<Context>(),
            PaywallActivity::class.java,
        ).putExtra(PaywallActivity.ARGS_EXTRA, args)
    }
}
