package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.UiConfig
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * WorkflowManager is now a thin adapter over [WorkflowsConfigProvider]; these cover the adapter contract
 * (offering-id resolution, success/error delivery, delegation). The end-to-end config behavior lives in
 * [WorkflowsConfigIntegrationTest].
 */
@OptIn(InternalRevenueCatAPI::class, ExperimentalCoroutinesApi::class)
class WorkflowManagerTest {

    private val provider = mockk<WorkflowsConfigProvider>()
    // Unconfined so the launched read runs eagerly and the callback fires before the call returns.
    private val manager = WorkflowManager(provider, CoroutineScope(UnconfinedTestDispatcher()))

    @Test
    fun `getWorkflow delivers the resolved workflow on success`() {
        every { provider.workflowIdForOfferingId("wf-1") } returns null
        coEvery { provider.getWorkflow("wf-1") } returns workflow("wf-1")

        var delivered: WorkflowDataResult? = null
        manager.getWorkflow("wf-1", onSuccess = { delivered = it }, onError = { })

        assertThat(delivered?.workflow?.id).isEqualTo("wf-1")
    }

    @Test
    fun `getWorkflow resolves an offering id to its workflow id before fetching`() {
        every { provider.workflowIdForOfferingId("premium") } returns "wf-9"
        coEvery { provider.getWorkflow("wf-9") } returns workflow("wf-9")

        var delivered: WorkflowDataResult? = null
        manager.getWorkflow("premium", onSuccess = { delivered = it }, onError = { })

        assertThat(delivered?.workflow?.id).isEqualTo("wf-9")
    }

    @Test
    fun `getWorkflow calls onError when the workflow is unavailable`() {
        every { provider.workflowIdForOfferingId("missing") } returns null
        coEvery { provider.getWorkflow("missing") } returns null

        var error: PurchasesError? = null
        manager.getWorkflow("missing", onSuccess = { }, onError = { error = it })

        assertThat(error).isNotNull
    }

    @Test
    fun `workflowIdForOfferingId delegates to the provider`() {
        every { provider.workflowIdForOfferingId("premium") } returns "wf-9"

        assertThat(manager.workflowIdForOfferingId("premium")).isEqualTo("wf-9")
    }

    private fun workflow(id: String) = WorkflowDataResult(
        workflow = PublishedWorkflow(
            id = id,
            displayName = id,
            initialStepId = "step-1",
            steps = emptyMap(),
            screens = emptyMap(),
            uiConfig = UiConfig(),
        ),
        enrolledVariants = null,
    )
}
