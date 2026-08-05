@file:OptIn(InternalRevenueCatAPI::class, ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.checkpoints

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class UiCheckpointWorkflowExecutorTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var mockPresenter: CheckpointPresenter
    private lateinit var mockActivity: Activity
    private var currentActivity: Activity? = null
    private lateinit var presentation: CheckpointWorkflowPresentation

    private lateinit var executor: UiCheckpointWorkflowExecutor

    @Before
    fun setup() {
        mockPresenter = mockk()
        mockActivity = mockk()
        currentActivity = mockActivity
        presentation = CheckpointWorkflowPresentation(
            checkpoint = CheckpointInfo("test_checkpoint", CheckpointParams()),
            workflow = mockk(),
            uiConfig = mockk(),
            offering = mockk(),
        )
        executor = UiCheckpointWorkflowExecutor(
            currentActivityProvider = { currentActivity },
            presenterProvider = { mockPresenter },
        )
    }

    @Test
    fun `execute errors with ConfigurationError when presenter is missing`() = runTest(dispatcher) {
        executor = UiCheckpointWorkflowExecutor(
            currentActivityProvider = { currentActivity },
            presenterProvider = { null },
        )

        assertThat(executionErrorCode()).isEqualTo(PurchasesErrorCode.ConfigurationError)
    }

    @Test
    fun `execute errors with ConfigurationError when no activity is available`() = runTest(dispatcher) {
        currentActivity = null

        assertThat(executionErrorCode()).isEqualTo(PurchasesErrorCode.ConfigurationError)
    }

    @Test
    fun `execute returns PaywallFinished with the delegate-reported result`() = runTest(dispatcher) {
        val paywallOutcome = CheckpointPaywallOutcome.Dismissed
        presenterReportsImmediately(paywallOutcome)

        val outcome = executor.execute(presentation) as CheckpointWorkflowOutcome.PaywallFinished

        assertThat(outcome.paywallOutcome).isEqualTo(paywallOutcome)
        verify { mockPresenter.present(mockActivity, any(), presentation, executor) }
    }

    @Test
    fun `concurrent execute errors with OperationAlreadyInProgressError`() = runTest(dispatcher) {
        every { mockPresenter.present(any(), any(), any(), any()) } just runs
        val firstCall = launch { executor.execute(presentation) }

        assertThat(executionErrorCode()).isEqualTo(PurchasesErrorCode.OperationAlreadyInProgressError)

        firstCall.cancel()
    }

    @Test
    fun `execute can present again after the previous workflow finishes`() = runTest(dispatcher) {
        presenterReportsImmediately(CheckpointPaywallOutcome.Dismissed)

        executor.execute(presentation)
        executor.execute(presentation)

        verify(exactly = 2) { mockPresenter.present(mockActivity, any(), presentation, executor) }
    }

    @Test
    fun `execute can present again after the presenter throws`() = runTest(dispatcher) {
        every { mockPresenter.present(any(), any(), any(), any()) } throws RuntimeException("startActivity failed")

        assertThat(executionErrorCode()).isEqualTo(PurchasesErrorCode.ConfigurationError)

        presenterReportsImmediately(CheckpointPaywallOutcome.Dismissed)
        assertThat(executor.execute(presentation))
            .isInstanceOf(CheckpointWorkflowOutcome.PaywallFinished::class.java)
    }

    @Test
    fun `execute can present again after being cancelled`() = runTest(dispatcher) {
        every { mockPresenter.present(any(), any(), any(), any()) } just runs
        val firstCall = launch { executor.execute(presentation) }

        firstCall.cancel()

        presenterReportsImmediately(CheckpointPaywallOutcome.Dismissed)
        assertThat(executor.execute(presentation))
            .isInstanceOf(CheckpointWorkflowOutcome.PaywallFinished::class.java)
    }

    @Test
    fun `report for unknown callId is a no-op`() = runTest(dispatcher) {
        executor.onCheckpointPaywallFinished("unknown-call-id", CheckpointPaywallOutcome.Dismissed)

        presenterReportsImmediately(CheckpointPaywallOutcome.Dismissed)
        assertThat(executor.execute(presentation))
            .isInstanceOf(CheckpointWorkflowOutcome.PaywallFinished::class.java)
    }

    private fun presenterReportsImmediately(paywallOutcome: CheckpointPaywallOutcome) {
        every { mockPresenter.present(mockActivity, any(), any(), any()) } answers {
            lastArg<CheckpointPresenterDelegate>().onCheckpointPaywallFinished(secondArg(), paywallOutcome)
        }
    }

    private suspend fun executionErrorCode(): PurchasesErrorCode? = try {
        executor.execute(presentation)
        null
    } catch (e: PurchasesException) {
        e.code
    }
}
