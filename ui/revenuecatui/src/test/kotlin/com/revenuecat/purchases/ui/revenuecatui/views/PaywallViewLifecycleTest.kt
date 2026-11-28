package com.revenuecat.purchases.ui.revenuecatui.views

import android.app.Activity
import android.os.Looper
import android.view.KeyEvent
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.lifecycle.ViewModelProvider
import com.revenuecat.purchases.DangerousSettings
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.common.workflows.WorkflowResolution
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
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
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class PaywallViewLifecycleTest {

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
    }

    @Test
    fun `PaywallView resets to Loading only after detaching from the window`() {
        val offering = TestData.template1Offering
        val activity = Robolectric.buildActivity(PaywallViewHostActivity::class.java).setup().get()
        val container = FrameLayout(activity)
        activity.setContentView(container)
        container.addView(
            PaywallView(
                context = activity,
                offering = offering,
                listener = null,
                fontProvider = null,
                shouldDisplayDismissButton = true,
                dismissHandler = {},
            ),
        )
        shadowOf(Looper.getMainLooper()).idle()
        val viewModelKey = activity.viewModelStore.keys().single { key -> key.toIntOrNull() != null }
        val viewModel = ViewModelProvider(activity).get(viewModelKey, PaywallViewModelImpl::class.java)
        assertThat(viewModel.state.value).isInstanceOf(PaywallState.Loaded::class.java)

        viewModel.closePaywall()
        assertThat(viewModel.state.value).isInstanceOf(PaywallState.Loaded::class.java)

        container.removeAllViews()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(viewModel.state.value).isEqualTo(PaywallState.Loading)
    }

    @Test
    fun `PaywallView composes when the host is not a ComponentActivity`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val container = FrameLayout(activity)
        activity.setContentView(container)
        val paywallView = PaywallView(
            context = activity,
            offering = TestData.template1Offering,
            listener = null,
            fontProvider = null,
            shouldDisplayDismissButton = true,
            dismissHandler = {},
        )

        container.addView(paywallView)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(container.findViewTreeOnBackPressedDispatcherOwner()).isNotNull
        assertThat(paywallView.childCount).isEqualTo(1)

        container.removeAllViews()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(container.findViewTreeOnBackPressedDispatcherOwner()).isNull()
    }

    @Test
    fun `back press dismisses PaywallView once when the host is not a ComponentActivity`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val container = FrameLayout(activity)
        activity.setContentView(container)
        var dismissCount = 0
        val paywallView = PaywallView(
            context = activity,
            offering = TestData.template1Offering,
            listener = null,
            fontProvider = null,
            shouldDisplayDismissButton = true,
            dismissHandler = { dismissCount++ },
        )
        container.addView(paywallView)
        shadowOf(Looper.getMainLooper()).idle()

        paywallView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK))
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(dismissCount).isEqualTo(1)
    }

    @Test
    fun `PaywallView leaves the host's OnBackPressedDispatcherOwner in place`() {
        val activity = Robolectric.buildActivity(PaywallViewHostActivity::class.java).setup().get()
        val container = FrameLayout(activity)
        activity.setContentView(container)

        container.addView(
            PaywallView(
                context = activity,
                offering = TestData.template1Offering,
                listener = null,
                fontProvider = null,
                shouldDisplayDismissButton = true,
                dismissHandler = {},
            ),
        )
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(container.findViewTreeOnBackPressedDispatcherOwner()).isSameAs(activity)
    }
}

class PaywallViewHostActivity : ComponentActivity()
