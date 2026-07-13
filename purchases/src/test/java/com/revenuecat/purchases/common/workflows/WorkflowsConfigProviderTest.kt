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
    fun `warm caches only prefetch and current-offering workflows`() = runTest {
        stubTopic()
        stubWorkflowBody(WF_PREFETCH)
        stubWorkflowBody(WF_CURRENT)

        provider.warm(generation = 0)

        assertThat(provider.getCachedWorkflow(WF_PREFETCH)).isNotNull
        assertThat(provider.getCachedWorkflow(WF_CURRENT)).isNotNull
        assertThat(provider.getCachedWorkflow(WF_OTHER)).isNull()
        // The ineligible workflow's body is never read.
        coVerify(exactly = 0) {
            manager.blobData(RemoteConfigTopic.Workflows, WF_OTHER, any<(ByteArray) -> ByteArray?>())
        }
    }

    @Test
    fun `warm caches the offering-to-workflow-id map for every offering`() = runTest {
        stubTopic()
        stubWorkflowBody(WF_PREFETCH)
        stubWorkflowBody(WF_CURRENT)

        provider.warm(generation = 0)

        assertThat(provider.getCachedWorkflowIdForOffering(CURRENT_OFFERING)).isEqualTo(WF_CURRENT)
        // Even the ineligible workflow's offering mapping is cached (metadata is cheap); only its body isn't.
        assertThat(provider.getCachedWorkflowIdForOffering(OTHER_OFFERING)).isEqualTo(WF_OTHER)
    }

    @Test
    fun `warm is a no-op and never reads bodies when the workflows topic is not committed`() = runTest {
        coEvery { manager.committedTopicOrNull(RemoteConfigTopic.Workflows) } returns null

        provider.warm(generation = 0)

        assertThat(provider.getCachedWorkflow(WF_PREFETCH)).isNull()
        coVerify(exactly = 0) {
            manager.blobData(RemoteConfigTopic.Workflows, any(), any<(ByteArray) -> ByteArray?>())
        }
    }

    @Test
    fun `onConfigInvalidated drops the cache`() = runTest {
        stubTopic()
        stubWorkflowBody(WF_PREFETCH)
        stubWorkflowBody(WF_CURRENT)
        provider.warm(generation = 0)
        assertThat(provider.getCachedWorkflow(WF_PREFETCH)).isNotNull

        provider.onConfigInvalidated(generation = 1)

        assertThat(provider.getCachedWorkflow(WF_PREFETCH)).isNull()
        assertThat(provider.getCachedWorkflowIdForOffering(CURRENT_OFFERING)).isNull()
    }

    @Test
    fun `a lower-generation warm does not clobber a higher-generation value`() = runTest {
        stubTopic()
        stubWorkflowBody(WF_PREFETCH)
        stubWorkflowBody(WF_CURRENT)

        provider.warm(generation = 5)
        val higher = provider.getCachedWorkflow(WF_PREFETCH)

        provider.warm(generation = 2)

        assertThat(provider.getCachedWorkflow(WF_PREFETCH)).isSameAs(higher)
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
