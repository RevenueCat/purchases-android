package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.emptyUiConfig
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.common.uiconfig.UiConfigProvider
import com.revenuecat.purchases.common.uiconfig.UiConfigResolution
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * [WorkflowManager] is now a thin adapter over [WorkflowsConfigProvider], so these tests exercise the
 * consumer-facing seam (`getWorkflow`/`workflowIdForOfferingId`/`onPaywallConfigReady`) against a mocked
 * provider. The provider's own behavior (topic reads, blob resolution) is covered by
 * [WorkflowsConfigIntegrationTest].
 */
class WorkflowManagerTest {

    private val mockProvider: WorkflowsConfigProvider = mockk()
    private val mockUiConfigProvider: UiConfigProvider = mockk()
    private val mockAssetPreDownloader: WorkflowAssetPrewarmer = mockk()
    private val uiConfig = emptyUiConfig()
    private val testScope = TestScope(UnconfinedTestDispatcher())
    private lateinit var workflowManager: WorkflowManager

    // This is a plain JUnit test (no Robolectric), so the default log handler's android.util.Log calls aren't
    // mocked. Swap in a recording handler so the prewarm-failure path can log without blowing up, and so the
    // readiness gate's error logging can be asserted — including its absence, which assertLogs can't express.
    private val originalLogHandler = currentLogHandler
    private val errorLogs = mutableListOf<String>()

    @Before
    fun setUp() {
        errorLogs.clear()
        currentLogHandler = object : LogHandler {
            override fun v(tag: String, msg: String) {}
            override fun d(tag: String, msg: String) {}
            override fun i(tag: String, msg: String) {}
            override fun w(tag: String, msg: String) {}
            override fun e(tag: String, msg: String, throwable: Throwable?) {
                errorLogs += msg
            }
        }
        coEvery { mockUiConfigProvider.getUiConfig() } returns uiConfig
        coEvery { mockUiConfigProvider.resolveUiConfig() } returns UiConfigResolution.Found(uiConfig)
        every { mockUiConfigProvider.isWarm() } returns false
        every { mockAssetPreDownloader.preDownloadWorkflowAssets(any(), any()) } just Runs
        workflowManager = WorkflowManager(
            mockProvider,
            mockUiConfigProvider,
            mockAssetPreDownloader,
            scope = testScope,
        )
    }

    @After
    fun tearDown() {
        currentLogHandler = originalLogHandler
    }

    @Test
    fun `getWorkflow resolves by workflow id when the provider has no offering mapping`() = runTest {
        val expectedResult = mockk<PublishedWorkflow>(relaxed = true)
        coEvery { mockProvider.workflowIdForOfferingId("wf_1") } returns null
        coEvery { mockProvider.getWorkflow("wf_1") } returns expectedResult

        val result = workflowManager.getWorkflow("wf_1")

        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `getWorkflowBody loads directly by workflow id without preparing UI`() = runTest {
        val expectedResult = mockk<PublishedWorkflow>(relaxed = true)
        coEvery { mockProvider.getWorkflow("wf_1") } returns expectedResult

        val result = workflowManager.getWorkflowBody("wf_1")

        assertThat(result).isEqualTo(expectedResult)
        coVerify(exactly = 0) { mockProvider.workflowIdForOfferingId(any()) }
        coVerify(exactly = 0) { mockUiConfigProvider.getUiConfig() }
        verify(exactly = 0) { mockAssetPreDownloader.preDownloadWorkflowAssets(any(), any()) }
    }

    @Test
    fun `getWorkflowBody throws when the provider cannot resolve the workflow`() = runTest {
        coEvery { mockProvider.getWorkflow("wf_missing") } returns null

        val thrown = runCatching { workflowManager.getWorkflowBody("wf_missing") }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(PurchasesException::class.java)
        assertThat((thrown as PurchasesException).error.code).isEqualTo(PurchasesErrorCode.UnknownError)
    }

    @Test
    fun `getWorkflow resolves an offering id to its workflow id before fetching`() = runTest {
        val offeringId = "my-offering"
        val workflowId = "wfl-real-id"
        val expectedResult = mockk<PublishedWorkflow>(relaxed = true)
        coEvery { mockProvider.workflowIdForOfferingId(offeringId) } returns workflowId
        coEvery { mockProvider.getWorkflow(workflowId) } returns expectedResult

        val result = workflowManager.getWorkflow(offeringId)

        assertThat(result).isEqualTo(expectedResult)
        coVerify(exactly = 0) { mockProvider.getWorkflow(offeringId) }
    }

    @Test
    fun `getWorkflow throws when the provider cannot resolve the workflow`() = runTest {
        coEvery { mockProvider.workflowIdForOfferingId("wf_missing") } returns null
        coEvery { mockProvider.getWorkflow("wf_missing") } returns null

        val thrown = runCatching { workflowManager.getWorkflow("wf_missing") }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(PurchasesException::class.java)
        assertThat((thrown as PurchasesException).error.code).isEqualTo(PurchasesErrorCode.UnknownError)
    }

    @Test
    fun `getWorkflow throws when ui_config is unavailable`() = runTest {
        val expectedResult = mockk<PublishedWorkflow>(relaxed = true)
        coEvery { mockProvider.workflowIdForOfferingId("wf_1") } returns null
        coEvery { mockProvider.getWorkflow("wf_1") } returns expectedResult
        coEvery { mockUiConfigProvider.getUiConfig() } returns null

        val thrown = runCatching { workflowManager.getWorkflow("wf_1") }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(PurchasesException::class.java)
        assertThat((thrown as PurchasesException).error.code).isEqualTo(PurchasesErrorCode.UnknownError)
        assertThat(thrown.error.underlyingErrorMessage).contains("UI config is unavailable")
        verify(exactly = 0) { mockAssetPreDownloader.preDownloadWorkflowAssets(any(), any()) }
    }

    @Test
    fun `getWorkflow throws when ui_config loading fails`() = runTest {
        val expectedResult = mockk<PublishedWorkflow>(relaxed = true)
        coEvery { mockProvider.workflowIdForOfferingId("wf_1") } returns null
        coEvery { mockProvider.getWorkflow("wf_1") } returns expectedResult
        coEvery { mockUiConfigProvider.getUiConfig() } throws RuntimeException("boom")

        val thrown = runCatching { workflowManager.getWorkflow("wf_1") }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(PurchasesException::class.java)
        assertThat((thrown as PurchasesException).error.code).isEqualTo(PurchasesErrorCode.UnknownError)
        assertThat(thrown.error.underlyingErrorMessage).contains("UI config is unavailable")
        verify(exactly = 0) { mockAssetPreDownloader.preDownloadWorkflowAssets(any(), any()) }
    }

    @Test
    fun `getWorkflow rethrows cancellation when ui_config loading is cancelled`() = runTest {
        val expectedResult = mockk<PublishedWorkflow>(relaxed = true)
        coEvery { mockProvider.workflowIdForOfferingId("wf_1") } returns null
        coEvery { mockProvider.getWorkflow("wf_1") } returns expectedResult
        coEvery { mockUiConfigProvider.getUiConfig() } throws CancellationException("cancelled")

        val thrown = runCatching { workflowManager.getWorkflow("wf_1") }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(CancellationException::class.java)
        verify(exactly = 0) { mockAssetPreDownloader.preDownloadWorkflowAssets(any(), any()) }
    }

    @Test
    fun `getWorkflow pre-downloads the workflow's assets with the ui_config on success`() = runTest {
        val expectedResult = mockk<PublishedWorkflow>(relaxed = true)
        coEvery { mockProvider.workflowIdForOfferingId("wf_1") } returns null
        coEvery { mockProvider.getWorkflow("wf_1") } returns expectedResult

        workflowManager.getWorkflow("wf_1")
        testScope.testScheduler.advanceUntilIdle()

        verify(exactly = 1) {
            mockAssetPreDownloader.preDownloadWorkflowAssets(expectedResult, uiConfig)
        }
    }

    @Test
    fun `getWorkflow does not pre-download assets when the workflow cannot be resolved`() = runTest {
        coEvery { mockProvider.workflowIdForOfferingId("wf_missing") } returns null
        coEvery { mockProvider.getWorkflow("wf_missing") } returns null

        runCatching { workflowManager.getWorkflow("wf_missing") }
        testScope.testScheduler.advanceUntilIdle()

        verify(exactly = 0) { mockAssetPreDownloader.preDownloadWorkflowAssets(any(), any()) }
    }

    @Test
    fun `getWorkflow still delivers the workflow when pre-downloading assets fails`() = runTest {
        val expectedResult = mockk<PublishedWorkflow>(relaxed = true)
        coEvery { mockProvider.workflowIdForOfferingId("wf_1") } returns null
        coEvery { mockProvider.getWorkflow("wf_1") } returns expectedResult
        every { mockAssetPreDownloader.preDownloadWorkflowAssets(any(), any()) } throws RuntimeException("boom")

        val result = workflowManager.getWorkflow("wf_1")
        testScope.testScheduler.advanceUntilIdle()

        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `resolveWorkflow delegates to the provider`() = runTest {
        coEvery { mockProvider.resolveWorkflow("off_1") } returns WorkflowResolution.Found("wf_1")

        val result = workflowManager.resolveWorkflow("off_1")

        assertThat(result).isEqualTo(WorkflowResolution.Found("wf_1"))
    }

    @Test
    fun `onPaywallConfigReady fires onComplete synchronously when both caches are already warm`() {
        val mockProvider = mockk<WorkflowsConfigProvider>()
        every { mockProvider.prewarmOfferingAssets() } just Runs
        every { mockProvider.isWarmForCurrentOffering() } returns true
        every { mockUiConfigProvider.isWarm() } returns true
        val manager = WorkflowManager(mockProvider, mockUiConfigProvider, mockAssetPreDownloader, scope = testScope)

        var completed = false
        // No advanceUntilIdle: a warm cache must deliver on the caller's thread with no dispatch.
        manager.onPaywallConfigReady { completed = true }

        assertThat(completed).isTrue()
        coVerify(exactly = 0) { mockProvider.warm() }
        coVerify(exactly = 0) { mockUiConfigProvider.resolveUiConfig() }
    }

    // The fast path returns before warm(), so the prewarm must not sit behind it.
    @Test
    fun `onPaywallConfigReady prewarms the current offering's assets even on the warm fast path`() {
        val mockProvider = mockk<WorkflowsConfigProvider>()
        every { mockProvider.prewarmOfferingAssets() } just Runs
        every { mockProvider.isWarmForCurrentOffering() } returns true
        every { mockUiConfigProvider.isWarm() } returns true
        val manager = WorkflowManager(mockProvider, mockUiConfigProvider, mockAssetPreDownloader, scope = testScope)

        manager.onPaywallConfigReady { }

        verify(exactly = 1) { mockProvider.prewarmOfferingAssets() }
    }

    @Test
    fun `onPaywallConfigReady warms both providers and invokes onComplete when cold`() {
        val mockProvider = mockk<WorkflowsConfigProvider>()
        every { mockProvider.prewarmOfferingAssets() } just Runs
        every { mockProvider.isWarmForCurrentOffering() } returns false
        coEvery { mockProvider.warm() } just Runs
        val manager = WorkflowManager(mockProvider, mockUiConfigProvider, mockAssetPreDownloader, scope = testScope)

        var completed = false
        manager.onPaywallConfigReady { completed = true }
        testScope.testScheduler.advanceUntilIdle()

        assertThat(completed).isTrue()
        coVerify(exactly = 1) { mockProvider.warm() }
        coVerify(exactly = 1) { mockUiConfigProvider.resolveUiConfig() }
    }

    @Test
    fun `onPaywallConfigReady still completes and error-logs when ui_config resolution throws`() {
        val mockProvider = mockk<WorkflowsConfigProvider>()
        every { mockProvider.prewarmOfferingAssets() } just Runs
        every { mockProvider.isWarmForCurrentOffering() } returns false
        coEvery { mockProvider.warm() } just Runs
        coEvery { mockUiConfigProvider.resolveUiConfig() } throws RuntimeException("boom")
        val manager = WorkflowManager(mockProvider, mockUiConfigProvider, mockAssetPreDownloader, scope = testScope)

        var completed = false
        manager.onPaywallConfigReady { completed = true }
        testScope.testScheduler.advanceUntilIdle()

        assertThat(completed).isTrue()
        assertThat(errorLogs).anyMatch { it.contains("Failed to ready ui_config before getOfferings") }
    }

    @Test
    fun `onPaywallConfigReady still completes and error-logs when a published ui_config is unavailable`() {
        val mockProvider = mockk<WorkflowsConfigProvider>()
        every { mockProvider.prewarmOfferingAssets() } just Runs
        every { mockProvider.isWarmForCurrentOffering() } returns false
        coEvery { mockProvider.warm() } just Runs
        coEvery { mockUiConfigProvider.resolveUiConfig() } returns UiConfigResolution.Unavailable
        val manager = WorkflowManager(mockProvider, mockUiConfigProvider, mockAssetPreDownloader, scope = testScope)

        var completed = false
        manager.onPaywallConfigReady { completed = true }
        testScope.testScheduler.advanceUntilIdle()

        assertThat(completed).isTrue()
        assertThat(errorLogs).anyMatch { it.contains("Failed to ready ui_config before getOfferings") }
    }

    @Test
    fun `onPaywallConfigReady completes without error-logging when there is no ui_config to ready`() {
        // A project with no paywalls configured has no ui_config at all, and neither does one whose resolve was
        // superseded by a newer commit. Neither is a failure, so neither may reach the developer's log as an error.
        val validOutcomes = listOf(
            UiConfigResolution.NotConfigured,
            UiConfigResolution.Superseded,
        )

        validOutcomes.forEach { outcome ->
            val mockProvider = mockk<WorkflowsConfigProvider>()
        every { mockProvider.prewarmOfferingAssets() } just Runs
            every { mockProvider.isWarmForCurrentOffering() } returns false
            coEvery { mockProvider.warm() } just Runs
            coEvery { mockUiConfigProvider.resolveUiConfig() } returns outcome
            val manager = WorkflowManager(mockProvider, mockUiConfigProvider, mockAssetPreDownloader, scope = testScope)

            var completed = false
            manager.onPaywallConfigReady { completed = true }
            testScope.testScheduler.advanceUntilIdle()

            assertThat(completed).describedAs("completed for $outcome").isTrue()
            assertThat(errorLogs).describedAs("error logs for $outcome").isEmpty()
        }
    }

    @Test
    fun `onPaywallConfigReady still completes when warming the workflows cache fails`() {
        val mockProvider = mockk<WorkflowsConfigProvider>()
        every { mockProvider.prewarmOfferingAssets() } just Runs
        every { mockProvider.isWarmForCurrentOffering() } returns false
        coEvery { mockProvider.warm() } throws RuntimeException("boom")
        val manager = WorkflowManager(mockProvider, mockUiConfigProvider, mockAssetPreDownloader, scope = testScope)

        var completed = false
        manager.onPaywallConfigReady { completed = true }
        testScope.testScheduler.advanceUntilIdle()

        assertThat(completed).isTrue()
        // A failure to ready one step must not cancel the other.
        coVerify(exactly = 1) { mockUiConfigProvider.resolveUiConfig() }
    }

    @Test
    fun `onPaywallConfigReady coalesces overlapping calls so readiness work runs only once and both callbacks fire`() {
        val gate = CompletableDeferred<Unit>()
        val mockProvider = mockk<WorkflowsConfigProvider>()
        every { mockProvider.prewarmOfferingAssets() } just Runs
        every { mockProvider.isWarmForCurrentOffering() } returns false
        coEvery { mockProvider.warm() } coAnswers { gate.await() }
        val manager = WorkflowManager(mockProvider, mockUiConfigProvider, mockAssetPreDownloader, scope = testScope)

        var completed1 = false
        var completed2 = false
        manager.onPaywallConfigReady { completed1 = true }
        manager.onPaywallConfigReady { completed2 = true }

        // Readiness work is still suspended inside the gate; neither callback should have fired yet.
        assertThat(completed1).isFalse()
        assertThat(completed2).isFalse()

        gate.complete(Unit)
        testScope.testScheduler.advanceUntilIdle()

        assertThat(completed1).isTrue()
        assertThat(completed2).isTrue()
        // The underlying work must have run exactly once despite two concurrent callers.
        coVerify(exactly = 1) { mockProvider.warm() }
        coVerify(exactly = 1) { mockUiConfigProvider.resolveUiConfig() }
    }

    @Test
    fun `onPaywallConfigReady does not invoke onComplete when the manager is closed before readiness completes`() {
        val gate = CompletableDeferred<Unit>()
        // Use a dedicated scope so closing the manager doesn't cancel the shared testScope.
        val managerScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher())
        val mockProvider = mockk<WorkflowsConfigProvider>()
        every { mockProvider.prewarmOfferingAssets() } just Runs
        every { mockProvider.isWarmForCurrentOffering() } returns false
        coEvery { mockProvider.warm() } coAnswers { gate.await() }
        val manager = WorkflowManager(mockProvider, mockUiConfigProvider, mockAssetPreDownloader, scope = managerScope)

        var completed = false
        manager.onPaywallConfigReady { completed = true }

        // Cancel scope (simulates teardown / identity change) while readiness is still suspended.
        manager.close()

        assertThat(completed).isFalse()
    }
}
