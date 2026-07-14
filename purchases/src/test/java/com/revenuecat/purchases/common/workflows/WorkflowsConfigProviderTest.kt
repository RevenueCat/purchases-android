package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.common.remoteconfig.ConfigTopic
import com.revenuecat.purchases.common.remoteconfig.RemoteConfiguration
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(InternalRevenueCatAPI::class)
internal class WorkflowsConfigProviderTest {

    private val manager = mockk<RemoteConfigManager>()
    private var currentOfferingId: String? = CURRENT_OFFERING
    private val provider = WorkflowsConfigProvider(manager, currentOfferingIdProvider = { currentOfferingId })

    private val originalLogHandler = currentLogHandler

    @Before
    fun setUp() {
        currentLogHandler = object : LogHandler {
            override fun v(tag: String, msg: String) {}
            override fun d(tag: String, msg: String) {}
            override fun i(tag: String, msg: String) {}
            override fun w(tag: String, msg: String) {}
            override fun e(tag: String, msg: String, throwable: Throwable?) {}
        }
    }

    @After
    fun tearDown() {
        currentLogHandler = originalLogHandler
    }

    @Test
    fun `warm resolves only prefetch and current-offering workflow bodies`() = runTest {
        stubTopic()
        stubWorkflowBody(WF_PREFETCH)
        stubWorkflowBody(WF_CURRENT)

        provider.warm(generation = 0)

        coVerify(exactly = 1) { manager.blobData(RemoteConfigTopic.Workflows, WF_PREFETCH, any<(ByteArray) -> ByteArray?>()) }
        coVerify(exactly = 1) { manager.blobData(RemoteConfigTopic.Workflows, WF_CURRENT, any<(ByteArray) -> ByteArray?>()) }
        // The ineligible workflow's body is never read.
        coVerify(exactly = 0) { manager.blobData(RemoteConfigTopic.Workflows, WF_OTHER, any<(ByteArray) -> ByteArray?>()) }
        assertThat(provider.isWarmForCurrentOffering()).isTrue
    }

    @Test
    fun `getWorkflow returns a warmed workflow from memory without re-reading`() = runTest {
        stubTopic()
        stubWorkflowBody(WF_PREFETCH)
        stubWorkflowBody(WF_CURRENT)
        provider.warm(generation = 0)

        val workflow = provider.getWorkflow(WF_CURRENT)

        assertThat(workflow).isNotNull
        // Only the one read during warm; the memory-first getWorkflow did not touch the config layer again.
        coVerify(exactly = 1) { manager.blobData(RemoteConfigTopic.Workflows, WF_CURRENT, any<(ByteArray) -> ByteArray?>()) }
    }

    @Test
    fun `workflowIdForOfferingId resolves from memory without reading the topic`() = runTest {
        stubTopic()
        stubWorkflowBody(WF_PREFETCH)
        stubWorkflowBody(WF_CURRENT)
        provider.warm(generation = 0)

        assertThat(provider.workflowIdForOfferingId(CURRENT_OFFERING)).isEqualTo(WF_CURRENT)
        // Even the ineligible workflow's offering mapping is cached (metadata is cheap).
        assertThat(provider.workflowIdForOfferingId(OTHER_OFFERING)).isEqualTo(WF_OTHER)
        // A strict mock: manager.topic() is never stubbed, so a cache miss would throw. It doesn't → memory hit.
        coVerify(exactly = 0) { manager.topic(RemoteConfigTopic.Workflows) }
    }

    @Test
    fun `workflowIdForOfferingId returns null from the warm cache for a non-offering id without reading the topic`() =
        runTest {
            // Regression: WorkflowManager.getWorkflow re-resolves with a workflow id (not an offering id). When
            // warm, that must return null from the complete cached map, NOT fall through to manager.topic() and
            // suspend — which would reintroduce a one-frame loading flash on an otherwise-warm render.
            stubTopic()
            stubWorkflowBody(WF_PREFETCH)
            stubWorkflowBody(WF_CURRENT)
            provider.warm(generation = 0)

            assertThat(provider.workflowIdForOfferingId(WF_CURRENT)).isNull()
            coVerify(exactly = 0) { manager.topic(RemoteConfigTopic.Workflows) }
        }

    @Test
    fun `warm is a no-op and never reads bodies when the workflows topic is not committed`() = runTest {
        coEvery { manager.committedTopicOrNull(RemoteConfigTopic.Workflows) } returns null

        provider.warm(generation = 0)

        assertThat(provider.isWarmForCurrentOffering()).isFalse
        coVerify(exactly = 0) {
            manager.blobData(RemoteConfigTopic.Workflows, any(), any<(ByteArray) -> ByteArray?>())
        }
    }

    @Test
    fun `isWarmForCurrentOffering is false when the current offering changes to an uncached workflow`() = runTest {
        stubTopic()
        stubWorkflowBody(WF_PREFETCH)
        stubWorkflowBody(WF_CURRENT)
        provider.warm(generation = 0)
        assertThat(provider.isWarmForCurrentOffering()).isTrue

        // The current offering switches to one whose workflow was not eligible (so not parsed).
        currentOfferingId = OTHER_OFFERING

        assertThat(provider.isWarmForCurrentOffering()).isFalse
    }

    @Test
    fun `onConfigInvalidated drops the cache`() = runTest {
        stubTopic()
        stubWorkflowBody(WF_PREFETCH)
        stubWorkflowBody(WF_CURRENT)
        provider.warm(generation = 0)
        assertThat(provider.isWarmForCurrentOffering()).isTrue

        provider.onConfigInvalidated(generation = 1)

        assertThat(provider.isWarmForCurrentOffering()).isFalse
    }

    @Test
    fun `a lower-generation warm does not clobber a higher-generation value`() = runTest {
        stubTopic()
        stubWorkflowBody(WF_PREFETCH)
        stubWorkflowBody(WF_CURRENT)

        provider.warm(generation = 5)
        val higher = provider.getWorkflow(WF_CURRENT)

        provider.warm(generation = 2)

        assertThat(provider.getWorkflow(WF_CURRENT)).isSameAs(higher)
    }

    @Test
    fun `getWorkflow cold miss returns the resolved workflow when the generation is unchanged`() = runTest {
        every { manager.configGeneration } returns 0
        stubWorkflowBody(WF_CURRENT)

        assertThat(provider.getWorkflow(WF_CURRENT)).isNotNull
    }

    @Test
    fun `getWorkflow does not serve a body resolved before a concurrent newer invalidation`() = runTest {
        // Cold read snapshots generation 0, then an identity-change invalidation at a newer generation lands
        // while the body is being resolved. That body may belong to the previous user, so it must not be served.
        every { manager.configGeneration } returns 0
        val body = workflowJson(WF_CURRENT).toByteArray()
        coEvery {
            manager.blobData(RemoteConfigTopic.Workflows, WF_CURRENT, any<(ByteArray) -> ByteArray?>())
        } answers {
            provider.onConfigInvalidated(generation = 5)
            thirdArg<(ByteArray) -> ByteArray?>().invoke(body)
        }

        assertThat(provider.getWorkflow(WF_CURRENT)).isNull()
    }

    @Test
    fun `workflowIdForOfferingId cold miss returns the resolved id when the generation is unchanged`() = runTest {
        every { manager.configGeneration } returns 0
        coEvery { manager.topic(RemoteConfigTopic.Workflows) } returns topicWith(
            WF_CURRENT to configItem(prefetch = false, offeringId = CURRENT_OFFERING),
        )

        assertThat(provider.workflowIdForOfferingId(CURRENT_OFFERING)).isEqualTo(WF_CURRENT)
    }

    @Test
    fun `workflowIdForOfferingId does not serve an id resolved before a concurrent newer invalidation`() = runTest {
        every { manager.configGeneration } returns 0
        coEvery { manager.topic(RemoteConfigTopic.Workflows) } answers {
            provider.onConfigInvalidated(generation = 5)
            topicWith(WF_CURRENT to configItem(prefetch = false, offeringId = CURRENT_OFFERING))
        }

        assertThat(provider.workflowIdForOfferingId(CURRENT_OFFERING)).isNull()
    }

    private fun stubTopic() {
        coEvery { manager.committedTopicOrNull(RemoteConfigTopic.Workflows) } returns topicWith(
            WF_PREFETCH to configItem(prefetch = true, offeringId = null),
            WF_CURRENT to configItem(prefetch = false, offeringId = CURRENT_OFFERING),
            WF_OTHER to configItem(prefetch = false, offeringId = OTHER_OFFERING),
        )
    }

    private fun stubWorkflowBody(workflowId: String) {
        val body = workflowJson(workflowId).toByteArray()
        coEvery {
            manager.blobData(RemoteConfigTopic.Workflows, workflowId, any<(ByteArray) -> ByteArray?>())
        } answers {
            thirdArg<(ByteArray) -> ByteArray?>().invoke(body)
        }
    }

    private fun topicWith(vararg items: Pair<String, RemoteConfiguration.ConfigItem>): ConfigTopic =
        ConfigTopic(linkedMapOf(*items))

    private fun configItem(prefetch: Boolean, offeringId: String?): RemoteConfiguration.ConfigItem =
        RemoteConfiguration.ConfigItem(
            blobRef = "ref-${offeringId ?: "none"}-$prefetch",
            prefetch = prefetch,
            metadata = offeringId?.let { buildJsonObject { put("offering_identifier", it) } } ?: JsonObject(emptyMap()),
        )

    private fun workflowJson(id: String): String =
        """{"id":"$id","display_name":"Workflow $id","initial_step_id":"step-1","steps":{},"screens":{}}"""

    private companion object {
        private const val CURRENT_OFFERING = "current_off"
        private const val OTHER_OFFERING = "other_off"
        private const val WF_PREFETCH = "wf_prefetch"
        private const val WF_CURRENT = "wf_current"
        private const val WF_OTHER = "wf_other"
    }
}
