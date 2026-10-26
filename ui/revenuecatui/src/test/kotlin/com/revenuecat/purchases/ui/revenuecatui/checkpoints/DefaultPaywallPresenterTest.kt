package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.common.offlineentitlements.ProductEntitlementMapping
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.PaywallDismissReason
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResult
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

    // Named apart from Offering.monthly, which the offering mock's block would otherwise resolve to.
    private val monthlyProduct = mockk<StoreProduct> {
        every { id } returns "monthly"
        every { purchasingData.productId } returns "monthly"
    }
    private val offering = mockk<Offering> {
        every { availablePackages } returns listOf(mockk<Package> { every { product } returns monthlyProduct })
    }
    private val params = PaywallPresenter.Params(
        offering,
        "test_checkpoint",
        mapOf("goal" to CustomVariableValue.String("test")),
        FlowPresentationMode.MODAL_FULL_SCREEN,
    )
    private val events = mutableListOf<String>()
    private val results = mutableListOf<Result>()
    private val completion = PaywallPresenter.Completion {
        results += it
        events += "reported"
    }

    // What the window would do with the dismissal the paywall requests: navigatedBack per request.
    private val dismissals = mutableListOf<Boolean>()

    private lateinit var mockActivity: Activity
    private lateinit var mockPurchases: Purchases
    private lateinit var mockWindow: CheckpointWorkflowPresenter
    private var windowCallId: String? = null
    private var windowMode: FlowPresentationMode? = null
    private lateinit var presenter: DefaultPaywallPresenter

    @Before
    fun setup() {
        mockkObject(Logger)
        every { Logger.e(any()) } just runs
        mockActivity = mockk(relaxed = true)
        mockPurchases = mockk { every { currentActivity } returns mockActivity }
        cachedActiveEntitlements()
        noMapping()
        mockWindow = mockk(relaxed = true)
        presenter = DefaultPaywallPresenter(mockPurchases, errorPresenter = { _, _ -> }) { callId, host, mode ->
            windowCallId = callId
            windowMode = mode
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
        assertThat(options().offeringSelection.offering).isEqualTo(offering)
        assertThat(results).isEmpty()
    }

    @Test
    fun `the paywall is configured through the options an app would use`() {
        present()

        val options = options()

        assertThat(options.offeringSelection.offering).isEqualTo(offering)
        assertThat(options.customVariables).isEqualTo(params.customVariables)
        assertThat(options.shouldDisplayDismissButton).isTrue
        assertThat(options.listener).isNotNull
        assertThat(options.injectedWorkflow).isNull()
        assertThat(options.injectedWorkflowOfferings).isNull()
    }

    @Test
    fun `the paywall hands its errors to the checkpoint's error presenter with the checkpoint's context`() {
        val presented = mutableListOf<ErrorPresenter.Params>()
        val completions = mutableListOf<ErrorPresenter.Completion>()
        presenter = DefaultPaywallPresenter(
            mockPurchases,
            errorPresenter = { errorParams, completion ->
                presented += errorParams
                completions += completion
            },
        ) { callId, _, _ ->
            windowCallId = callId
            mockWindow
        }
        present()
        val error = PurchasesError(PurchasesErrorCode.StoreProblemError, "boom")
        val completion = ErrorPresenter.Completion {}

        options().errorPresenter!!.present(error, CheckpointFailureKind.FLOW_ACTION_FAILED, completion)

        assertThat(presented).containsExactly(
            ErrorPresenter.Params(
                error,
                params.checkpointIdentifier,
                params.customVariables,
                CheckpointFailureKind.FLOW_ACTION_FAILED,
            ),
        )
        assertThat(completions).containsExactly(completion)
    }

    @Test
    fun `the window is presented the way the params ask`() {
        present()
        assertThat(windowMode).isEqualTo(FlowPresentationMode.MODAL_FULL_SCREEN)

        val sheetParams = PaywallPresenter.Params(
            offering,
            params.checkpointIdentifier,
            params.customVariables,
            FlowPresentationMode.MODAL_SHEET,
        )
        presenter.present(sheetParams, completion)
        assertThat(windowMode).isEqualTo(FlowPresentationMode.MODAL_SHEET)
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
    fun `a close request is wired as a plain dismissal`() {
        present()

        options().dismissRequest()
        options().dismissRequestWithExitOffering!!(null, null, PaywallDismissReason.CLOSE)

        assertThat(dismissals).containsExactly(false, false)
    }

    @Test
    fun `a back navigation is wired as a backed-out dismissal`() {
        present()

        options().dismissRequestWithExitOffering!!(null, null, PaywallDismissReason.NAVIGATED_BACK)

        assertThat(dismissals).containsExactly(true)
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
        options().listener!!.onPurchaseCompleted(customerInfoWithActive("pro"), mockk())

        finished(navigatedBack = true)

        assertThat(results).containsExactly(Result.Continued)
        assertThat(dismissals).isEmpty()
    }

    @Test
    fun `a restore that grants a presented entitlement closes the paywall and reports Continued`() {
        cachedActiveEntitlements("plus")
        mappingGrants("monthly" to listOf("pro"))
        present()

        options().listener!!.onRestoreCompleted(customerInfoWithActive("plus", "pro"))
        assertThat(dismissals).containsExactly(false)
        finished(navigatedBack = false)

        assertThat(results).containsExactly(Result.Continued)
    }

    @Test
    fun `a restore that grants nothing new changes nothing`() {
        cachedActiveEntitlements("plus")
        mappingGrants("monthly" to listOf("plus"))
        present()

        options().listener!!.onRestoreCompleted(customerInfoWithActive("plus"))
        assertThat(dismissals).isEmpty()
        finished(navigatedBack = true)

        assertThat(results).containsExactly(Result.NavigatedBack)
    }

    @Test
    fun `a restore counts every presented entitlement as new when there is no cached customer info`() {
        noCachedCustomerInfo()
        mappingGrants("monthly" to listOf("pro"))
        present()

        options().listener!!.onRestoreCompleted(customerInfoWithActive("pro"))

        assertThat(dismissals).containsExactly(false)
        finished(navigatedBack = true)
        assertThat(results).containsExactly(Result.Continued)
    }

    @Test
    fun `a restore that grants an entitlement the offering's products do not grant changes nothing`() {
        mappingGrants("monthly" to listOf("pro"))
        present()

        options().listener!!.onRestoreCompleted(customerInfoWithActive("plus"))
        assertThat(dismissals).isEmpty()
        finished(navigatedBack = true)

        assertThat(results).containsExactly(Result.NavigatedBack)
    }

    @Test
    fun `without a cached product-entitlement mapping a granting restore changes nothing`() {
        present()

        options().listener!!.onRestoreCompleted(customerInfoWithActive("pro"))
        assertThat(dismissals).isEmpty()
        finished(navigatedBack = true)

        assertThat(results).containsExactly(Result.NavigatedBack)
    }

    @Test
    fun `a granting restore closes the window that is currently on screen`() {
        mappingGrants("monthly" to listOf("pro"))
        present()
        val firstDismissals = mutableListOf<Boolean>()
        presenter.paywallOptions(windowCallId!!) { firstDismissals += it }

        // A re-present after a configuration change asks for options again with the new window's wiring.
        options().listener!!.onRestoreCompleted(customerInfoWithActive("pro"))

        assertThat(firstDismissals).isEmpty()
        assertThat(dismissals).containsExactly(false)
    }

    @Test
    fun `a later error does not erase a recorded grant`() {
        mappingGrants("monthly" to listOf("pro"))
        present()
        options().listener!!.onRestoreCompleted(customerInfoWithActive("pro"))
        options().listener!!.onPurchaseError(PurchasesError(PurchasesErrorCode.StoreProblemError, "boom"))

        finished(navigatedBack = true)

        assertThat(results).containsExactly(Result.Continued)
    }

    @Test
    fun `a window that could not be kept on screen presents the error and reports Failed once acknowledged`() {
        val presented = mutableListOf<ErrorPresenter.Params>()
        var errorCompletion: ErrorPresenter.Completion? = null
        presenter = DefaultPaywallPresenter(
            mockPurchases,
            errorPresenter = { errorParams, completion ->
                presented += errorParams
                errorCompletion = completion
            },
        ) { callId, _, _ ->
            windowCallId = callId
            mockWindow
        }
        present()
        val error = PurchasesError(PurchasesErrorCode.ConfigurationError, "Re-present failed.")

        presenter.onPresentationFailed(windowCallId!!, error)

        assertThat(presented).containsExactly(
            ErrorPresenter.Params(
                error,
                params.checkpointIdentifier,
                params.customVariables,
                CheckpointFailureKind.FLOW_ACTION_FAILED,
            ),
        )
        assertThat(results).isEmpty()
        verify(exactly = 1) { Logger.e(match { it.contains(error.toString()) }) }
        errorCompletion!!.complete()
        assertThat(results).hasSize(1)
        assertThat((results.single() as Result.Failed).error).isEqualTo(error)
    }

    @Test
    fun `a window that could not be kept on screen reports Failed right away when the error cannot be shown`() {
        presenter = DefaultPaywallPresenter(mockPurchases, errorPresenter = { _, _ -> error("Simulated.") }) {
                callId, _, _ ->
            windowCallId = callId
            mockWindow
        }
        present()
        val error = PurchasesError(PurchasesErrorCode.ConfigurationError, "Re-present failed.")

        presenter.onPresentationFailed(windowCallId!!, error)

        assertThat((results.single() as Result.Failed).error).isEqualTo(error)
    }

    @Test
    fun `a paywall that could not load reports Failed with its error`() {
        present()
        val error = PurchasesError(PurchasesErrorCode.ConfigurationError, "No packages available")

        options().dismissRequestWithExitOffering!!(null, PaywallResult.Error(error), PaywallDismissReason.CLOSE)
        assertThat(dismissals).containsExactly(false)
        finished(navigatedBack = false)

        assertThat((results.single() as Result.Failed).error).isEqualTo(error)
    }

    @Test
    fun `a paywall that could not load after a purchase still reports Continued`() {
        present()
        options().listener!!.onPurchaseCompleted(customerInfoWithActive("pro"), mockk())

        options().dismissRequestWithExitOffering!!(
            null,
            PaywallResult.Error(PurchasesError(PurchasesErrorCode.ConfigurationError, "boom")),
            PaywallDismissReason.CLOSE,
        )
        finished(navigatedBack = false)

        assertThat(results).containsExactly(Result.Continued)
    }

    @Test
    fun `a window that could not be kept on screen after a purchase reports Continued`() {
        present()
        options().listener!!.onPurchaseCompleted(customerInfoWithActive("pro"), mockk())

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
    fun `paywall events after the report are ignored`() {
        present()
        val listener = options().listener!!
        finished(navigatedBack = false)

        listener.onRestoreCompleted(customerInfoWithActive("pro"))

        assertThat(dismissals).isEmpty()
        assertThat(results).containsExactly(Result.Closed)
    }

    @Test
    fun `the presentation is gone once the window has reported`() {
        present()

        finished(navigatedBack = false)

        assertThat(presenter.paywallOptions(windowCallId!!) {}).isNull()
    }

    @Test
    fun `a window that fails to show is taken down before the failure propagates`() {
        every { mockWindow.show(any()) } throws IllegalStateException("show failed")

        assertThatThrownBy { present() }.isInstanceOf(IllegalStateException::class.java)

        verify(exactly = 1) { mockWindow.abandon() }
        assertThat(results).isEmpty()
    }

    private fun present() = presenter.present(params, completion)

    // What the window asks its host for on each show.
    private fun options(): PaywallOptions = presenter.paywallOptions(windowCallId!!) { dismissals += it }!!

    private fun finished(navigatedBack: Boolean, finishPresentation: () -> Unit = {}) =
        presenter.onPresentationFinished(windowCallId!!, navigatedBack, finishPresentation)

    private fun cachedActiveEntitlements(vararg identifiers: String) {
        every { mockPurchases.getCustomerInfo(CacheFetchPolicy.CACHE_ONLY, any()) } answers {
            secondArg<ReceiveCustomerInfoCallback>().onReceived(customerInfoWithActive(*identifiers))
        }
    }

    private fun mappingGrants(vararg entries: Pair<String, List<String>>) {
        every { mockPurchases.internalProductEntitlementMapping() } returns ProductEntitlementMapping(
            entries.associate { (productId, entitlements) ->
                productId to ProductEntitlementMapping.Mapping(productId, null, entitlements)
            },
        )
    }

    private fun noMapping() {
        every { mockPurchases.internalProductEntitlementMapping() } returns null
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
