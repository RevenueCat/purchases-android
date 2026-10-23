package com.revenuecat.purchases.ui.revenuecatui.views

import android.view.Choreographer
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.revenuecat.purchases.DangerousSettings
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.common.workflows.WorkflowResolution
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
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
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Regression test for https://github.com/RevenueCat/react-native-purchases/issues/1836.
 *
 * Runs on a device instead of Robolectric: in the Robolectric suite, recomposers left running by other tests kept the
 * paywall from recomposing, so the test passed even without a fix.
 */
@RunWith(AndroidJUnit4::class)
class PaywallViewPoolingContainerTest {

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
    fun paywallViewDoesNotCrashWhenRecomposingWhileCachedByARecyclerView() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            lateinit var paywallView: PaywallView
            lateinit var recyclerView: RecyclerView
            scenario.onActivity { activity ->
                paywallView = PaywallView(
                    context = activity,
                    offering = TestData.template1Offering,
                    listener = null,
                    fontProvider = null,
                    shouldDisplayDismissButton = true,
                    dismissHandler = {},
                )
                recyclerView = RecyclerView(activity).apply {
                    layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
                    adapter = PaywallFirstAdapter(paywallView, itemCount = 3)
                }
                activity.setContentView(recyclerView)
            }
            awaitFrames(FRAMES_TO_SETTLE)
            scenario.onActivity { assertThat(paywallView.isAttachedToWindow).isTrue() }

            // Like ViewPager2 moving one page: scrolling puts the paywall's item in the RecyclerView's view cache. It's
            // detached from the window but not released to the pool, so the default composition strategy keeps its
            // composition. This needs a scroll: jumping with scrollToPosition re-lays out the list, and that path
            // detaches the item from its parent first, so its composition is disposed and the bug doesn't reproduce.
            scenario.onActivity { recyclerView.scrollBy(recyclerView.width, 0) }
            awaitFrames(FRAMES_TO_SETTLE)
            scenario.onActivity { assertThat(paywallView.isAttachedToWindow).isFalse() }

            // Compose reports a failed recomposition to the uncaught exception handler from inside its coroutine,
            // which normally kills the app. Recording it instead keeps the test process alive.
            val exceptions = Collections.synchronizedList(mutableListOf<Throwable>())
            val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { _, throwable -> exceptions.add(throwable) }
            try {
                scenario.onActivity { paywallView.setDisplayDismissButton(false) }
                awaitFrames(FRAMES_TO_SETTLE)
                instrumentation.waitForIdleSync()
            } finally {
                Thread.setDefaultUncaughtExceptionHandler(previousHandler)
            }

            assertThat(exceptions).isEmpty()
        }
    }

    private fun awaitFrames(count: Int) {
        val frames = CountDownLatch(count)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            Choreographer.getInstance().postFrameCallback(object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    frames.countDown()
                    if (frames.count > 0) Choreographer.getInstance().postFrameCallback(this)
                }
            })
        }
        assertThat(frames.await(FRAME_TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
    }

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
    }

    private companion object {
        const val PAYWALL_TYPE = 0
        const val FILLER_TYPE = 1
        const val FRAMES_TO_SETTLE = 5
        const val FRAME_TIMEOUT_SECONDS = 5L
    }
}
