@file:OptIn(InternalRevenueCatAPI::class, ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.checkpoints

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.interfaces.CheckpointCallback
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class CheckpointsManagerTest {

    private val checkpointId = "test_checkpoint"

    private lateinit var dispatcher: TestDispatcher
    private lateinit var mockResolver: CheckpointWorkflowResolver
    private lateinit var mockExecutor: CheckpointWorkflowExecutor
    private lateinit var mockCallback: CheckpointCallback
    private lateinit var mockListener: CheckpointListener

    private lateinit var manager: CheckpointsManager

    @Before
    fun setup() {
        dispatcher = UnconfinedTestDispatcher()
        mockResolver = mockk()
        mockExecutor = mockk()
        mockCallback = mockk(relaxed = true)
        mockListener = mockk(relaxed = true)
        manager = CheckpointsManager(resolver = mockResolver, executor = mockExecutor, mainDispatcher = dispatcher)
        manager.checkpointListener = mockListener
    }

    @Test
    fun `unmatched checkpoint resolves NoAction with the resolver's reason`() {
        coEvery { mockResolver.resolve(any()) } returns
            CheckpointWorkflowResolution.NoMatch(CheckpointResult.NoAction.Reason.NO_MATCH)
        val resultSlot = slot<CheckpointResult>()
        every { mockCallback.onResult(capture(resultSlot)) } just runs

        manager.checkpoint(checkpointId, null, mockCallback)

        val result = resultSlot.captured as CheckpointResult.NoAction
        assertThat(result.reason).isEqualTo(CheckpointResult.NoAction.Reason.NO_MATCH)
        assertThat(result.checkpoint.identifier).isEqualTo(checkpointId)
        verifyOrder {
            mockListener.onCheckpointHit(result.checkpoint)
            mockListener.onCheckpointResolved(result.checkpoint, result)
            mockCallback.onResult(result)
        }
    }

    @Test
    fun `failed resolution errors with the resolver's error`() {
        coEvery { mockResolver.resolve(any()) } returns CheckpointWorkflowResolution.Failed(
            PurchasesError(PurchasesErrorCode.ConfigurationError, "Simulated."),
        )
        val errorSlot = slot<PurchasesError>()
        every { mockCallback.onError(capture(errorSlot)) } just runs

        manager.checkpoint(checkpointId, null, mockCallback)

        assertThat(errorSlot.captured.code).isEqualTo(PurchasesErrorCode.ConfigurationError)
        verify(exactly = 0) { mockCallback.onResult(any()) }
    }

    @Test
    fun `awaiting a failed resolution throws PurchasesException`() = runTest(dispatcher) {
        coEvery { mockResolver.resolve(any()) } returns CheckpointWorkflowResolution.Failed(
            PurchasesError(PurchasesErrorCode.ConfigurationError, "Simulated."),
        )

        val exception = try {
            manager.checkpoint(checkpointId, null)
            null
        } catch (e: PurchasesException) {
            e
        }

        assertThat(exception?.code).isEqualTo(PurchasesErrorCode.ConfigurationError)
    }

    @Test
    fun `executor failure errors the callback`() {
        matchCheckpointToWorkflow()
        coEvery { mockExecutor.execute(any()) } throws PurchasesException(
            PurchasesError(PurchasesErrorCode.OperationAlreadyInProgressError, "Already presenting."),
        )
        val errorSlot = slot<PurchasesError>()
        every { mockCallback.onError(capture(errorSlot)) } just runs

        manager.checkpoint(checkpointId, null, mockCallback)

        assertThat(errorSlot.captured.code).isEqualTo(PurchasesErrorCode.OperationAlreadyInProgressError)
    }

    @Test
    fun `matched checkpoint resolves PaywallPresented when the workflow finishes`() {
        matchCheckpointToWorkflow()
        val paywallFinished = CompletableDeferred<CheckpointPaywallResult>()
        coEvery { mockExecutor.execute(any()) } coAnswers {
            CheckpointWorkflowOutcome.PaywallFinished(paywallFinished.await())
        }

        manager.checkpoint(checkpointId, CheckpointParams("goal" to "test"), mockCallback)

        verify(exactly = 0) { mockCallback.onResult(any()) }

        val paywallResult = CheckpointPaywallResult.Dismissed()
        paywallFinished.complete(paywallResult)

        val resultSlot = slot<CheckpointResult>()
        verify { mockCallback.onResult(capture(resultSlot)) }
        val result = resultSlot.captured as CheckpointResult.PaywallPresented
        assertThat(result.paywallResult).isEqualTo(paywallResult)
        assertThat(result.checkpoint.identifier).isEqualTo(checkpointId)
        assertThat(result.checkpoint.params.customProperties).isEqualTo(mapOf("goal" to "test"))
        verifyOrder {
            mockListener.onCheckpointHit(any())
            mockListener.onCheckpointPaywallFinished(result.checkpoint, paywallResult)
            mockListener.onCheckpointResolved(result.checkpoint, result)
            mockCallback.onResult(result)
        }
    }

    @Test
    fun `awaiting matched checkpoint resolves PaywallPresented when the workflow finishes`() = runTest(dispatcher) {
        matchCheckpointToWorkflow()
        coEvery { mockExecutor.execute(any()) } returns
            CheckpointWorkflowOutcome.PaywallFinished(CheckpointPaywallResult.Dismissed())

        val result = manager.checkpoint(checkpointId, null)

        assertThat(result).isInstanceOf(CheckpointResult.PaywallPresented::class.java)
    }

    private fun matchCheckpointToWorkflow() {
        coEvery { mockResolver.resolve(any()) } answers {
            CheckpointWorkflowResolution.Matched(
                CheckpointWorkflowPresentation(
                    checkpoint = firstArg(),
                    workflow = mockk(),
                    uiConfig = mockk(),
                    offering = null,
                ),
            )
        }
    }
}
