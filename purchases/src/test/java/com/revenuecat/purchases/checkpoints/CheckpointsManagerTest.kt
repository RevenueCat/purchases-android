@file:OptIn(InternalRevenueCatAPI::class, ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.checkpoints

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verifyOrder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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
    private lateinit var mockListener: CheckpointListener

    private lateinit var manager: CheckpointsManager

    @Before
    fun setup() {
        dispatcher = UnconfinedTestDispatcher()
        mockResolver = mockk()
        mockExecutor = mockk()
        mockListener = mockk(relaxed = true)
        manager = CheckpointsManager(resolver = mockResolver, executor = mockExecutor, mainDispatcher = dispatcher)
        manager.checkpointListener = mockListener
    }

    @Test
    fun `unmatched checkpoint resolves NoAction with the resolver's reason`() = runTest(dispatcher) {
        coEvery { mockResolver.resolve(any()) } returns
            CheckpointWorkflowResolution.NoMatch(CheckpointResult.NoAction.Reason.NO_MATCH)

        val result = manager.checkpoint(checkpointId, null) as CheckpointResult.NoAction

        assertThat(result.reason).isEqualTo(CheckpointResult.NoAction.Reason.NO_MATCH)
        assertThat(result.checkpoint.identifier).isEqualTo(checkpointId)
        verifyOrder {
            mockListener.onCheckpointHit(result.checkpoint)
            mockListener.onCheckpointCompleted(result.checkpoint, result)
        }
    }

    @Test
    fun `failed resolution throws PurchasesException with the resolver's error`() = runTest(dispatcher) {
        coEvery { mockResolver.resolve(any()) } returns CheckpointWorkflowResolution.Failed(
            PurchasesError(PurchasesErrorCode.ConfigurationError, "Simulated."),
        )

        assertThat(checkpointErrorCode()).isEqualTo(PurchasesErrorCode.ConfigurationError)
    }

    @Test
    fun `executor failure propagates to the caller`() = runTest(dispatcher) {
        matchCheckpointToWorkflow()
        coEvery { mockExecutor.execute(any()) } throws PurchasesException(
            PurchasesError(PurchasesErrorCode.OperationAlreadyInProgressError, "Already presenting."),
        )

        assertThat(checkpointErrorCode()).isEqualTo(PurchasesErrorCode.OperationAlreadyInProgressError)
    }

    @Test
    fun `matched checkpoint resolves PaywallPresented when the workflow finishes`() = runTest(dispatcher) {
        matchCheckpointToWorkflow()
        val paywallFinished = CompletableDeferred<CheckpointPaywallOutcome>()
        coEvery { mockExecutor.execute(any()) } coAnswers {
            CheckpointWorkflowOutcome.PaywallFinished(paywallFinished.await())
        }

        var result: CheckpointResult? = null
        val call = launch { result = manager.checkpoint(checkpointId, CheckpointParams("goal" to "test")) }

        assertThat(result).isNull()

        val paywallOutcome = CheckpointPaywallOutcome.Dismissed
        paywallFinished.complete(paywallOutcome)
        call.join()

        val presented = result as CheckpointResult.PaywallPresented
        assertThat(presented.paywallOutcome).isEqualTo(paywallOutcome)
        assertThat(presented.checkpoint.identifier).isEqualTo(checkpointId)
        assertThat(presented.checkpoint.params.customProperties).isEqualTo(mapOf("goal" to "test"))
        verifyOrder {
            mockListener.onCheckpointHit(any())
            mockListener.onCheckpointCompleted(presented.checkpoint, presented)
        }
    }

    private fun matchCheckpointToWorkflow() {
        coEvery { mockResolver.resolve(any()) } answers {
            CheckpointWorkflowResolution.Matched(
                CheckpointWorkflowPresentation(
                    checkpoint = firstArg(),
                    workflow = mockk(),
                    uiConfig = mockk(),
                    offering = mockk(),
                ),
            )
        }
    }

    private suspend fun checkpointErrorCode(): PurchasesErrorCode? = try {
        manager.checkpoint(checkpointId, null)
        null
    } catch (e: PurchasesException) {
        e.code
    }
}
