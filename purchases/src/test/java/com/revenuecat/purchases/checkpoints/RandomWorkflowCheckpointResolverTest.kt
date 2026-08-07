@file:OptIn(InternalRevenueCatAPI::class, ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.checkpoints

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.common.uiconfig.UiConfigProvider
import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import com.revenuecat.purchases.common.workflows.WorkflowManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class RandomWorkflowCheckpointResolverTest {

    private val checkpointId = "test_checkpoint"

    private lateinit var mockWorkflowManager: WorkflowManager
    private lateinit var mockUiConfigProvider: UiConfigProvider
    private lateinit var mockWorkflow: PublishedWorkflow
    private lateinit var mockUiConfig: UiConfig
    private lateinit var mockOffering: Offering
    private lateinit var mockOfferings: Offerings
    private var offeringsFetchError: PurchasesError? = null
    private var offeringsFetched = false

    private lateinit var resolver: RandomWorkflowCheckpointResolver

    @Before
    fun setup() {
        mockWorkflowManager = mockk()
        mockUiConfigProvider = mockk()
        mockWorkflow = mockk()
        mockUiConfig = mockk()
        mockOffering = mockk {
            every { identifier } returns "default"
        }
        mockOfferings = mockk()
        coEvery { mockWorkflowManager.offeringIdByWorkflowId() } returns mapOf("wf1234" to "default")
        coEvery { mockWorkflowManager.getWorkflow("wf1234") } returns mockWorkflow
        coEvery { mockUiConfigProvider.getUiConfig() } returns mockUiConfig
        every { mockOfferings.all } returns mapOf("default" to mockOffering)
        resolver = RandomWorkflowCheckpointResolver(
            workflowManager = mockWorkflowManager,
            uiConfigProvider = mockUiConfigProvider,
            getOfferings = {
                offeringsFetched = true
                offeringsFetchError?.let { throw PurchasesException(it) }
                mockOfferings
            },
        )
    }

    @Test
    fun `simulated error checkpoint throws ConfigurationError`() = runTest {
        val errorCode = try {
            resolver.resolve("error_checkpoint", emptyMap())
            null
        } catch (e: PurchasesException) {
            e.code
        }

        assertThat(errorCode).isEqualTo(PurchasesErrorCode.ConfigurationError)
    }

    @Test
    fun `simulated unknown checkpoint resolves NoAction with UNKNOWN_CHECKPOINT`() = runTest {
        val resolution = resolver.resolve("unknown_checkpoint", emptyMap())

        assertThat(noActionReason(resolution)).isEqualTo(CheckpointResolution.NoAction.Reason.UNKNOWN_CHECKPOINT)
    }

    @Test
    fun `checkpoint resolves NoAction with DISABLED when workflows are disabled`() = runTest {
        resolver = RandomWorkflowCheckpointResolver(
            workflowManager = null,
            uiConfigProvider = null,
            getOfferings = { mockOfferings },
        )

        assertThat(noActionReason(resolve()))
            .isEqualTo(CheckpointResolution.NoAction.Reason.DISABLED)
    }

    @Test
    fun `checkpoint resolves NoAction with CONFIGURATION_UNAVAILABLE when no workflows exist`() =
        runTest {
            coEvery { mockWorkflowManager.offeringIdByWorkflowId() } returns emptyMap()

            assertThat(noActionReason(resolve()))
                .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
        }

    @Test
    fun `checkpoint resolves NoAction with CONFIGURATION_UNAVAILABLE when the workflow fails to load`() =
        runTest {
            coEvery { mockWorkflowManager.getWorkflow("wf1234") } throws PurchasesException(
                PurchasesError(PurchasesErrorCode.UnknownError, "Workflow unavailable."),
            )

            assertThat(noActionReason(resolve()))
                .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
        }

    @Test
    fun `checkpoint resolves NoAction with CONFIGURATION_UNAVAILABLE when ui config is unavailable`() =
        runTest {
            coEvery { mockUiConfigProvider.getUiConfig() } returns null

            assertThat(noActionReason(resolve()))
                .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
        }

    @Test
    fun `checkpoint resolves Workflow with the workflow and its offering`() = runTest {
        val resolution = resolve() as CheckpointResolution.Workflow

        assertThat(resolution.workflow).isEqualTo(mockWorkflow)
        assertThat(resolution.uiConfig).isEqualTo(mockUiConfig)
        assertThat(resolution.offering).isEqualTo(mockOffering)
    }

    @Test
    fun `workflows without an offering identifier are not picked`() = runTest {
        coEvery { mockWorkflowManager.offeringIdByWorkflowId() } returns mapOf("wf1234" to null)

        assertThat(noActionReason(resolve()))
            .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
        assertThat(offeringsFetched).isFalse()
    }

    @Test
    fun `checkpoint resolves NoAction with CONFIGURATION_UNAVAILABLE when the offerings fetch fails`() = runTest {
        offeringsFetchError = PurchasesError(PurchasesErrorCode.NetworkError, "Offline.")

        assertThat(noActionReason(resolve()))
            .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
    }

    @Test
    fun `checkpoint resolves NoAction with CONFIGURATION_UNAVAILABLE when the fetched offerings lack the identifier`() =
        runTest {
            every { mockOfferings.all } returns emptyMap()

            assertThat(noActionReason(resolve()))
                .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
        }

    private suspend fun resolve(): CheckpointResolution = resolver.resolve(checkpointId, emptyMap())

    private fun noActionReason(resolution: CheckpointResolution): CheckpointResolution.NoAction.Reason =
        (resolution as CheckpointResolution.NoAction).reason
}
