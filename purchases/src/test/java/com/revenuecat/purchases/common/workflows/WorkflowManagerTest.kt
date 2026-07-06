package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test

/**
 * [WorkflowManager] is now a thin adapter over [WorkflowsConfigProvider], so these tests exercise the
 * consumer-facing seam (`getWorkflow`/`workflowIdForOfferingId`/`awaitWorkflowsReady`) against a mocked
 * provider. The provider's own behavior (topic reads, blob resolution) is covered by
 * [WorkflowsConfigIntegrationTest].
 */
class WorkflowManagerTest {

    private val mockProvider: WorkflowsConfigProvider = mockk()
    private val testScope = TestScope(UnconfinedTestDispatcher())
    private lateinit var workflowManager: WorkflowManager

    @Before
    fun setUp() {
        workflowManager = WorkflowManager(mockProvider, scope = testScope)
    }

    @Test
    fun `getWorkflow resolves by workflow id when the provider has no offering mapping`() {
        val expectedResult = mockk<PublishedWorkflow>(relaxed = true)
        coEvery { mockProvider.workflowIdForOfferingId("wf_1") } returns null
        coEvery { mockProvider.getWorkflow("wf_1") } returns expectedResult

        var result: PublishedWorkflow? = null
        workflowManager.getWorkflow(
            workflowOrOfferingId = "wf_1",
            onSuccess = { result = it },
            onError = { fail("unexpected error $it") },
        )

        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `getWorkflow resolves an offering id to its workflow id before fetching`() {
        val offeringId = "my-offering"
        val workflowId = "wfl-real-id"
        val expectedResult = mockk<PublishedWorkflow>(relaxed = true)
        coEvery { mockProvider.workflowIdForOfferingId(offeringId) } returns workflowId
        coEvery { mockProvider.getWorkflow(workflowId) } returns expectedResult

        var result: PublishedWorkflow? = null
        workflowManager.getWorkflow(
            workflowOrOfferingId = offeringId,
            onSuccess = { result = it },
            onError = { fail("unexpected error $it") },
        )

        assertThat(result).isEqualTo(expectedResult)
        coVerify(exactly = 0) { mockProvider.getWorkflow(offeringId) }
    }

    @Test
    fun `getWorkflow calls onError when the provider cannot resolve the workflow`() {
        coEvery { mockProvider.workflowIdForOfferingId("wf_missing") } returns null
        coEvery { mockProvider.getWorkflow("wf_missing") } returns null

        var error: PurchasesError? = null
        workflowManager.getWorkflow(
            workflowOrOfferingId = "wf_missing",
            onSuccess = { fail("expected error") },
            onError = { error = it },
        )

        assertThat(error).isNotNull
        assertThat(error!!.code).isEqualTo(PurchasesErrorCode.UnknownError)
    }

    @Test
    fun `workflowIdForOfferingId delegates to the provider`() = runTest {
        coEvery { mockProvider.workflowIdForOfferingId("off_1") } returns "wf_1"

        val result = workflowManager.workflowIdForOfferingId("off_1")

        assertThat(result).isEqualTo("wf_1")
    }

    @Test
    fun `awaitWorkflowsReady invokes onComplete after the provider syncs`() {
        val mockProvider = mockk<WorkflowsConfigProvider>()
        coEvery { mockProvider.awaitReady() } just Runs
        val manager = WorkflowManager(mockProvider, scope = testScope)

        var completed = false
        manager.awaitWorkflowsReady { completed = true }
        testScope.testScheduler.advanceUntilIdle()

        assertThat(completed).isTrue()
        coVerify(exactly = 1) { mockProvider.awaitReady() }
    }
}
