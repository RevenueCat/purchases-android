package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.PaywallPresenter.Completion.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class DefaultPaywallPresenterTest {

    private val offering = mockk<Offering>()
    private val params = PaywallPresenter.Params(
        offering,
        "test_checkpoint",
        mapOf("goal" to CustomVariableValue.String("test")),
    )
    private val events = mutableListOf<String>()
    private val results = mutableListOf<Result>()
    private val completion = PaywallPresenter.Completion {
        results += it
        events += "reported"
    }

    private lateinit var mockActivity: Activity
    private lateinit var mockPurchases: Purchases
    private lateinit var mockWindow: CheckpointWorkflowPresenter
    private var windowCallId: String? = null
    private lateinit var presenter: DefaultPaywallPresenter

    @Before
    fun setup() {
        mockkObject(Logger)
        every { Logger.e(any()) } just runs
        mockActivity = mockk(relaxed = true)
        mockPurchases = mockk { every { currentActivity } returns mockActivity }
        cachedActiveEntitlements()
        mockWindow = mockk(relaxed = true)
        presenter = DefaultPaywallPresenter(mockPurchases) { callId, host ->
            windowCallId = callId
            assertThat(host).isSameAs(presenter)
            mockWindow
        }
    }

    @After
    fun tearDown() {
        unmockkObject(Logger)
    }

    @Test
    fun `presenting shows the offering's paywall in a window over the current activity`() {
        present()

        verify(exactly = 1) { mockWindow.show(mockActivity) }
        val presentation = presenter.presentation(windowCallId!!)!!
        assertThat((presentation.content as CheckpointFlowContent.OfferingFlow).offering).isEqualTo(offering)
        assertThat(presentation.customVariables).isEqualTo(params.customVariables)
        assertThat(results).isEmpty()
    }

    @Test
    fun `presenting without a started activity throws and shows nothing`() {
        every { mockPurchases.currentActivity } returns null

        assertThatThrownBy { present() }
            .isInstanceOf(PurchasesException::class.java)
            .matches { (it as PurchasesException).error.code == PurchasesErrorCode.ConfigurationError }
        verify(exactly = 0) { mockWindow.show(any()) }
        assertThat(results).isEmpty()
    }

    @Test
    fun `a closed window reports Closed once the window is down`() {
        present()

        finished(navigatedBack = false) { events += "window down" }

        assertThat(events).containsExactly("window down", "reported")
        assertThat(results).containsExactly(Result.Closed)
    }

    @Test
    fun `a window left through back navigation reports NavigatedBack`() {
        present()

        finished(navigatedBack = true)

        assertThat(results).containsExactly(Result.NavigatedBack)
    }

    @Test
    fun `a purchase reports Continued even when leaving through back navigation`() {
        present()
        record(CheckpointFlowOutcome.Purchased(customerInfoWithActive("pro"), mockk()))

        finished(navigatedBack = true)

        assertThat(results).containsExactly(Result.Continued)
        verify(exactly = 0) { mockWindow.dismiss() }
    }

    @Test
    fun `a restore that grants a new entitlement closes the paywall and reports Continued`() {
        cachedActiveEntitlements("plus")
        present()

        record(CheckpointFlowOutcome.Restored(customerInfoWithActive("plus", "pro")))
        verify(exactly = 1) { mockWindow.dismiss() }
        finished(navigatedBack = false)

        assertThat(results).containsExactly(Result.Continued)
    }

    @Test
    fun `a restore that grants nothing new changes nothing`() {
        cachedActiveEntitlements("plus")
        present()

        record(CheckpointFlowOutcome.Restored(customerInfoWithActive("plus")))
        verify(exactly = 0) { mockWindow.dismiss() }
        finished(navigatedBack = true)

        assertThat(results).containsExactly(Result.NavigatedBack)
    }

    @Test
    fun `a restore counts every active entitlement as new when there is no cached customer info`() {
        noCachedCustomerInfo()
        present()

        record(CheckpointFlowOutcome.Restored(customerInfoWithActive("plus")))

        verify(exactly = 1) { mockWindow.dismiss() }
        finished(navigatedBack = true)
        assertThat(results).containsExactly(Result.Continued)
    }

    @Test
    fun `an error or web checkout does not change how leaving is reported`() {
        present()
        record(CheckpointFlowOutcome.Error(PurchasesError(PurchasesErrorCode.StoreProblemError, "boom")))
        record(CheckpointFlowOutcome.WebCheckoutOpened)

        finished(navigatedBack = true)

        assertThat(results).containsExactly(Result.NavigatedBack)
        verify(exactly = 0) { mockWindow.dismiss() }
    }

    @Test
    fun `a later error does not erase a recorded grant`() {
        present()
        record(CheckpointFlowOutcome.Restored(customerInfoWithActive("pro")))
        record(CheckpointFlowOutcome.Error(PurchasesError(PurchasesErrorCode.StoreProblemError, "boom")))

        finished(navigatedBack = true)

        assertThat(results).containsExactly(Result.Continued)
    }

    @Test
    fun `a window that could not be kept on screen reports Closed and logs the error`() {
        present()
        val error = PurchasesError(PurchasesErrorCode.ConfigurationError, "Re-present failed.")

        presenter.onPresentationFailed(windowCallId!!, error)

        assertThat(results).containsExactly(Result.Closed)
        verify(exactly = 1) { Logger.e(match { it.contains(error.toString()) }) }
    }

    @Test
    fun `a window that could not be kept on screen after a purchase reports Continued`() {
        present()
        record(CheckpointFlowOutcome.Purchased(customerInfoWithActive("pro"), mockk()))

        presenter.onPresentationFailed(
            windowCallId!!,
            PurchasesError(PurchasesErrorCode.ConfigurationError, "Re-present failed."),
        )

        assertThat(results).containsExactly(Result.Continued)
    }

    @Test
    fun `only the first report reaches the completion`() {
        present()

        finished(navigatedBack = true)
        finished(navigatedBack = false)
        presenter.onPresentationFailed(
            windowCallId!!,
            PurchasesError(PurchasesErrorCode.ConfigurationError, "Re-present failed."),
        )

        assertThat(results).containsExactly(Result.NavigatedBack)
    }

    @Test
    fun `outcomes recorded after the report are ignored`() {
        present()
        finished(navigatedBack = false)

        record(CheckpointFlowOutcome.Restored(customerInfoWithActive("pro")))

        verify(exactly = 0) { mockWindow.dismiss() }
        assertThat(results).containsExactly(Result.Closed)
    }

    @Test
    fun `the presentation is gone once the window has reported`() {
        present()

        finished(navigatedBack = false)

        assertThat(presenter.presentation(windowCallId!!)).isNull()
    }

    @Test
    fun `a window that fails to show is taken down before the failure propagates`() {
        every { mockWindow.show(any()) } throws IllegalStateException("show failed")

        assertThatThrownBy { present() }.isInstanceOf(IllegalStateException::class.java)

        verify(exactly = 1) { mockWindow.abandon() }
        assertThat(results).isEmpty()
    }

    private fun present() = presenter.present(params, completion)

    private fun record(outcome: CheckpointFlowOutcome) = presenter.recordOutcome(windowCallId!!, outcome)

    private fun finished(navigatedBack: Boolean, finishPresentation: () -> Unit = {}) =
        presenter.onPresentationFinished(windowCallId!!, navigatedBack, finishPresentation)

    private fun cachedActiveEntitlements(vararg identifiers: String) {
        every { mockPurchases.getCustomerInfo(CacheFetchPolicy.CACHE_ONLY, any()) } answers {
            secondArg<ReceiveCustomerInfoCallback>().onReceived(customerInfoWithActive(*identifiers))
        }
    }

    private fun noCachedCustomerInfo() {
        every { mockPurchases.getCustomerInfo(CacheFetchPolicy.CACHE_ONLY, any()) } answers {
            secondArg<ReceiveCustomerInfoCallback>()
                .onError(PurchasesError(PurchasesErrorCode.CustomerInfoError, "No cache."))
        }
    }

    private fun customerInfoWithActive(vararg identifiers: String): CustomerInfo {
        val active = identifiers.associateWith { id -> mockk<EntitlementInfo> { every { identifier } returns id } }
        return mockk { every { entitlements.active } returns active }
    }
}
