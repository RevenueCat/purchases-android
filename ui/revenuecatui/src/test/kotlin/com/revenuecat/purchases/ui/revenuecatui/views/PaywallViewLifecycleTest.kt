package com.revenuecat.purchases.ui.revenuecatui.views

import android.os.Looper
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
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
import java.time.Duration

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
    fun `PaywallView does not crash when recomposing after detaching inside a pooling container`() {
        val activity = Robolectric.buildActivity(PaywallViewHostActivity::class.java).setup().get()
        // RecyclerView and ViewPager2 are pooling containers. They detach their children without releasing them.
        // The tag is what androidx.customview.poolingcontainer.isPoolingContainer sets, which is not on our classpath.
        val poolingContainerTag = activity.resources.getIdentifier(
            "is_pooling_container_tag",
            "id",
            activity.packageName,
        )
        val container = FrameLayout(activity).apply { setTag(poolingContainerTag, true) }
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

        // Recomposition runs on a frame, outside of the test's call stack.
        val uncaughtExceptions = mutableListOf<Throwable>()
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, throwable -> uncaughtExceptions.add(throwable) }
        try {
            container.removeAllViews()
            paywallView.setDisplayDismissButton(false)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previousHandler)
        }

        assertThat(uncaughtExceptions).isEmpty()
    }
}

class PaywallViewHostActivity : ComponentActivity()
