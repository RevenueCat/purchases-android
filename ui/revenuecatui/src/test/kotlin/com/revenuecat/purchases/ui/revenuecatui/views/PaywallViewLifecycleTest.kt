package com.revenuecat.purchases.ui.revenuecatui.views

import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    fun `PaywallView does not crash when recomposing while cached by a RecyclerView`() {
        val activity = Robolectric.buildActivity(PaywallViewHostActivity::class.java).setup().get()
        val paywallView = PaywallView(
            context = activity,
            offering = TestData.template1Offering,
            listener = null,
            fontProvider = null,
            shouldDisplayDismissButton = true,
            dismissHandler = {},
        )
        val recyclerView = RecyclerView(activity).apply {
            layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
            adapter = PaywallFirstAdapter(paywallView, itemCount = 3)
        }
        activity.setContentView(recyclerView)
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(paywallView.isAttachedToWindow).isTrue()

        // Like ViewPager2 moving one page: scrolling puts the paywall's item in the RecyclerView's view cache. It's
        // detached from the window but not released to the pool, so the default composition strategy keeps its
        // composition. This needs a scroll: jumping with scrollToPosition re-lays out the list, and that path detaches
        // the item from its parent first, so its composition is disposed and the bug doesn't reproduce.
        recyclerView.scrollBy(recyclerView.width, 0)
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(paywallView.isAttachedToWindow).isFalse()

        // Recomposition runs on a frame, outside of the test's call stack.
        val exceptions = mutableListOf<Throwable>()
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, throwable -> exceptions.add(throwable) }
        try {
            paywallView.setDisplayDismissButton(false)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            exceptions.add(e)
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previousHandler)
        }

        assertThat(exceptions).isEmpty()
    }
}

class PaywallViewHostActivity : ComponentActivity()

private class PaywallFirstAdapter(
    private val paywallView: PaywallView,
    private val itemCount: Int,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemCount(): Int = itemCount

    override fun getItemViewType(position: Int): Int = if (position == 0) PAYWALL_TYPE else FILLER_TYPE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = if (viewType == PAYWALL_TYPE) paywallView else View(parent.context)
        view.layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        return object : RecyclerView.ViewHolder(view) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = Unit

    private companion object {
        const val PAYWALL_TYPE = 0
        const val FILLER_TYPE = 1
    }
}
