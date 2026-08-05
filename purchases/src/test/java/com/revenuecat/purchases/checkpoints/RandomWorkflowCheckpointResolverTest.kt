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

    private val checkpoint = CheckpointInfo("test_checkpoint", CheckpointParams())

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
    fun `simulated error checkpoint fails with ConfigurationError`() = runTest {
        val resolution = resolver.resolve(CheckpointInfo("error_checkpoint", CheckpointParams()))

        val failed = resolution as CheckpointWorkflowResolution.Failed
        assertThat(failed.error.code).isEqualTo(PurchasesErrorCode.ConfigurationError)
    }

    @Test
    fun `simulated unknown checkpoint resolves NoMatch with NO_MATCH`() = runTest {
        val resolution = resolver.resolve(CheckpointInfo("unknown_checkpoint", CheckpointParams()))

        assertThat(noMatchReason(resolution)).isEqualTo(CheckpointResult.NoAction.Reason.NO_MATCH)
    }

    @Test
    fun `checkpoint resolves NoMatch with DISABLED when workflows are disabled`() = runTest {
        resolver = RandomWorkflowCheckpointResolver(
            workflowManager = null,
            uiConfigProvider = null,
            getOfferings = { mockOfferings },
        )

        assertThat(noMatchReason(resolver.resolve(checkpoint)))
            .isEqualTo(CheckpointResult.NoAction.Reason.DISABLED)
    }

    @Test
    fun `checkpoint resolves NoMatch with CONFIGURATION_UNAVAILABLE when no workflows exist`() =
        runTest {
            coEvery { mockWorkflowManager.offeringIdByWorkflowId() } returns emptyMap()

            assertThat(noMatchReason(resolver.resolve(checkpoint)))
                .isEqualTo(CheckpointResult.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
        }

    @Test
    fun `checkpoint resolves NoMatch with CONFIGURATION_UNAVAILABLE when the workflow fails to load`() =
        runTest {
            coEvery { mockWorkflowManager.getWorkflow("wf1234") } throws PurchasesException(
                PurchasesError(PurchasesErrorCode.UnknownError, "Workflow unavailable."),
            )

            assertThat(noMatchReason(resolver.resolve(checkpoint)))
                .isEqualTo(CheckpointResult.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
        }

    @Test
    fun `checkpoint resolves NoMatch with CONFIGURATION_UNAVAILABLE when ui config is unavailable`() =
        runTest {
            coEvery { mockUiConfigProvider.getUiConfig() } returns null

            assertThat(noMatchReason(resolver.resolve(checkpoint)))
                .isEqualTo(CheckpointResult.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
        }

    @Test
    fun `checkpoint resolves Matched with the workflow and its offering`() = runTest {
        val resolution = resolver.resolve(checkpoint)

        val presentation = (resolution as CheckpointWorkflowResolution.Matched).presentation
        assertThat(presentation.checkpoint).isEqualTo(checkpoint)
        assertThat(presentation.workflow).isEqualTo(mockWorkflow)
        assertThat(presentation.uiConfig).isEqualTo(mockUiConfig)
        assertThat(presentation.offering).isEqualTo(mockOffering)
    }

    @Test
    fun `workflows without an offering identifier are not picked`() = runTest {
        coEvery { mockWorkflowManager.offeringIdByWorkflowId() } returns mapOf("wf1234" to null)

        assertThat(noMatchReason(resolver.resolve(checkpoint)))
            .isEqualTo(CheckpointResult.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
        assertThat(offeringsFetched).isFalse()
    }

    @Test
    fun `checkpoint resolves NoMatch with CONFIGURATION_UNAVAILABLE when the offerings fetch fails`() = runTest {
        offeringsFetchError = PurchasesError(PurchasesErrorCode.NetworkError, "Offline.")

        assertThat(noMatchReason(resolver.resolve(checkpoint)))
            .isEqualTo(CheckpointResult.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
    }

    @Test
    fun `checkpoint resolves NoMatch with CONFIGURATION_UNAVAILABLE when the fetched offerings lack the identifier`() =
        runTest {
            every { mockOfferings.all } returns emptyMap()

            assertThat(noMatchReason(resolver.resolve(checkpoint)))
                .isEqualTo(CheckpointResult.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
        }

    private fun noMatchReason(resolution: CheckpointWorkflowResolution): CheckpointResult.NoAction.Reason =
        (resolution as CheckpointWorkflowResolution.NoMatch).reason
}
