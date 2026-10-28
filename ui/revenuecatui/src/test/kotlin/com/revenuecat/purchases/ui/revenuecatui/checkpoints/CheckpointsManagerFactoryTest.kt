@file:OptIn(ExperimentalCoroutinesApi::class, InternalRevenueCatAPI::class, InviteOnlyCheckpointsAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.checkpoints.CheckpointResolution
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Integration tests for [ErrorDialogFactory] wired through [CheckpointsManager]. Covers the precedence matrix
 * (per-call errorPresenter > per-call errorDialogFactory > registered errorPresenter > SDK default) and the
 * "no factory → default untouched" case.
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class CheckpointsManagerFactoryTest {

    private val checkpointId = "test_checkpoint"
    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var mockPurchases: Purchases
    private lateinit var mockActivity: Activity
    private lateinit var mockPresenter: CheckpointWorkflowPresenter

    private val defaultErrorPresenters = mutableListOf<DefaultErrorPresenter>()
    private val defaultPresentedErrors = mutableListOf<ErrorPresenter.Params>()

    private lateinit var manager: CheckpointsManager

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        mockkObject(Logger)
        every { Logger.e(any()) } just runs
        every { Logger.w(any()) } just runs
        mockActivity = mockk(relaxed = true)
        mockPresenter = mockk(relaxed = true)
        defaultErrorPresenters.clear()
        defaultPresentedErrors.clear()
        val customerInfo = mockk<CustomerInfo> {
            every { entitlements.active } returns emptyMap()
            every { getPurchaseDateForProductId(any()) } returns null
        }
        mockPurchases = mockk {
            every { currentActivity } returns mockActivity
            every { getCustomerInfo(CacheFetchPolicy.CACHED_OR_FETCHED, any()) } answers {
                secondArg<ReceiveCustomerInfoCallback>().onReceived(customerInfo)
            }
        }
        manager = CheckpointsManager(
            presenterFactory = { _, _, _ -> mockPresenter },
            defaultPresenterFactory = { purchases, errorPresenter ->
                DefaultPaywallPresenter(purchases, errorPresenter) { _, _, _ -> mockPresenter }
            },
            defaultErrorPresenterFactory = { _ ->
                mockk<DefaultErrorPresenter> {
                    every { present(any(), any()) } answers {
                        defaultPresentedErrors += firstArg<ErrorPresenter.Params>()
                        secondArg<ErrorPresenter.Completion>().complete()
                    }
                }.also { defaultErrorPresenters += it }
            },
        )
        coEvery { mockPurchases.internalResolveCp(any(), any()) } returns
            CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
    }

    @After
    fun tearDown() {
        unmockkObject(Logger)
        Dispatchers.resetMain()
    }

    // region: precedence matrix

    @Test
    fun `no factory and no presenter uses the SDK default dialog`() = runTest(dispatcher) {
        manager.runCheckpoint(mockPurchases, checkpointId, params = null)

        assertThat(defaultErrorPresenters).hasSize(1)
        assertThat(defaultPresentedErrors).hasSize(1)
    }

    @Test
    fun `per-call errorDialogFactory is used when no errorPresenter is set`() = runTest(dispatcher) {
        val factoryAsked = mutableListOf<ErrorDialogFactory.Params>()
        val params = CheckpointParams {
            errorDialogFactory { p, completion ->
                factoryAsked += p
                completion.complete(null)
            }
        }

        manager.runCheckpoint(mockPurchases, checkpointId, params)

        // Factory was asked; SDK default was chosen (null completion) so defaultErrorPresenterFactory ran
        assertThat(factoryAsked).hasSize(1)
        assertThat(factoryAsked[0].checkpointIdentifier).isEqualTo(checkpointId)
        assertThat(factoryAsked[0].kind).isEqualTo(CheckpointFailureKind.FLOW_COULD_NOT_BE_SHOWN)
        assertThat(defaultErrorPresenters).hasSize(1)
    }

    @Test
    fun `per-call errorPresenter takes precedence over per-call errorDialogFactory`() = runTest(dispatcher) {
        var presenterAsked = false
        var factoryAsked = false
        val params = CheckpointParams {
            errorPresenter { _, completion ->
                presenterAsked = true
                completion.complete()
            }
            errorDialogFactory { _, completion ->
                factoryAsked = true
                completion.complete(null)
            }
        }

        manager.runCheckpoint(mockPurchases, checkpointId, params)

        assertThat(presenterAsked).isTrue
        assertThat(factoryAsked).isFalse
        assertThat(defaultErrorPresenters).isEmpty()
    }

    @Test
    fun `per-call errorDialogFactory takes precedence over registered errorPresenter`() = runTest(dispatcher) {
        var registeredAsked = false
        var factoryAsked = false
        manager.errorPresenter = ErrorPresenter { _, _ -> registeredAsked = true }
        val params = CheckpointParams {
            errorDialogFactory { _, completion ->
                factoryAsked = true
                completion.complete(null)
            }
        }

        manager.runCheckpoint(mockPurchases, checkpointId, params)

        assertThat(factoryAsked).isTrue
        assertThat(registeredAsked).isFalse
    }

    @Test
    fun `per-call errorDialogFactory takes precedence over the SDK default`() = runTest(dispatcher) {
        var factoryAsked = false
        val params = CheckpointParams {
            errorDialogFactory { _, completion ->
                factoryAsked = true
                completion.complete(null)
            }
        }

        manager.runCheckpoint(mockPurchases, checkpointId, params)

        assertThat(factoryAsked).isTrue
        // Factory returned null so SDK default is shown (defaultErrorPresenterFactory creates the presenter)
        assertThat(defaultErrorPresenters).hasSize(1)
    }

    // endregion

    // region: factory-supplied dialog content

    @Test
    fun `factory returning a non-null dialog creates a DefaultErrorPresenter for its content`() = runTest(dispatcher) {
        val dialog = CheckpointErrorDialog(message = "Custom error", buttonTitle = "Got it")
        val params = CheckpointParams {
            errorDialogFactory { _, completion -> completion.complete(dialog) }
        }

        // defaultErrorPresenterFactory is for the SDK default; a non-null dialog creates a different presenter
        // with custom content. Since DefaultErrorPresenter needs an activity (which we've mocked to return
        // mockActivity), this exercises the full path without throwing.
        manager.runCheckpoint(mockPurchases, checkpointId, params)

        // The factory path used a custom DefaultErrorPresenter (not the SDK's factory), so
        // defaultErrorPresenterFactory was NOT invoked.
        assertThat(defaultErrorPresenters).isEmpty()
    }

    // endregion

    // region: factory throwing falls back to SDK default

    @Test
    fun `a per-call factory that throws falls back to the SDK default`() = runTest(dispatcher) {
        val params = CheckpointParams {
            errorDialogFactory { _, _ -> error("Simulated.") }
        }

        manager.runCheckpoint(mockPurchases, checkpointId, params)

        assertThat(defaultErrorPresenters).hasSize(1)
        assertThat(defaultPresentedErrors).hasSize(1)
    }

    // endregion
}
