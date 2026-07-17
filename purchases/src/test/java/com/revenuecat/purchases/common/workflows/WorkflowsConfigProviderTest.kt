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
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
        // Default: endpoint live. Tests exercising the 4xx kill switch override this to true.
        every { manager.isDisabled } returns false
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
    fun `warm pre-loads bytes but defers the decode until the first getWorkflow, then retains it`() = runTest {
        mockkObject(WorkflowJsonParser) {
            stubTopic()
            stubWorkflowBody(WF_PREFETCH)
            stubWorkflowBody(WF_CURRENT)

            provider.warm(generation = 0)
            // Bytes are in memory (the offering is warm), but nothing has been decoded yet.
            assertThat(provider.isWarmForCurrentOffering()).isTrue
            verify(exactly = 0) { WorkflowJsonParser.parsePublishedWorkflow(any()) }

            assertThat(provider.getWorkflow(WF_CURRENT)).isNotNull
            verify(exactly = 1) { WorkflowJsonParser.parsePublishedWorkflow(any()) }

            // A second read is served from the retained decode, not re-parsed.
            assertThat(provider.getWorkflow(WF_CURRENT)).isNotNull
            verify(exactly = 1) { WorkflowJsonParser.parsePublishedWorkflow(any()) }
        }
    }

    @Test
    fun `a malformed workflow body warms as bytes but reads back null`() = runTest {
        stubTopic()
        stubWorkflowBody(WF_PREFETCH)
        // WF_CURRENT's body resolves but is not valid PublishedWorkflow JSON.
        coEvery {
            manager.blobData(RemoteConfigTopic.Workflows, WF_CURRENT, any<(ByteArray) -> ByteArray?>())
        } answers { thirdArg<(ByteArray) -> ByteArray?>().invoke("not a workflow".toByteArray()) }

        provider.warm(generation = 0)

        // The body was cached (bytes resolved), so the gate treats the current offering as warm...
        assertThat(provider.isWarmForCurrentOffering()).isTrue
        // ...but the deferred decode fails synchronously on read, surfacing as null (no warm-time exclusion).
        assertThat(provider.getWorkflow(WF_CURRENT)).isNull()
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
    fun `warm(generation) never triggers a sync`() = runTest {
        // The commit/init overload must stay a pure read: a strict mock would throw if it touched the
        // sync-triggering path, but assert it explicitly to lock the contract in.
        stubTopic()
        stubWorkflowBody(WF_PREFETCH)
        stubWorkflowBody(WF_CURRENT)

        provider.warm(generation = 0)

        coVerify(exactly = 0) { manager.awaitTopicAndPrefetchBlobsReady(any()) }
    }

    @Test
    fun `no-arg warm triggers a sync for an uncommitted topic, then warms from the freshly committed config`() =
        runTest {
            // Regression: the gate's no-arg warm() must be able to force a /v1/config sync. onPaywallConfigReady
            // primes the sync via getUiConfig() only on a COLD ui_config cache; when ui_config is already warm it
            // short-circuits, so the workflows step is the only thing left that can sync. A pure
            // committedTopicOrNull read (the old warm()) would see the not-yet-committed topic, no-op, and let the
            // offerings gate fire without workflows ready — reintroducing the loading flash at render time.
            every { manager.configGeneration } returns 0
            var synced = false
            coEvery { manager.committedTopicOrNull(RemoteConfigTopic.Workflows) } answers {
                if (synced) {
                    topicWith(
                        WF_PREFETCH to configItem(prefetch = true, offeringId = null),
                        WF_CURRENT to configItem(prefetch = false, offeringId = CURRENT_OFFERING),
                    )
                } else {
                    null
                }
            }
            // The sync is what commits the topic.
            coEvery { manager.awaitTopicAndPrefetchBlobsReady(RemoteConfigTopic.Workflows) } answers {
                synced = true
                null
            }
            stubWorkflowBody(WF_PREFETCH)
            stubWorkflowBody(WF_CURRENT)

            provider.warm()

            coVerify(exactly = 1) { manager.awaitTopicAndPrefetchBlobsReady(RemoteConfigTopic.Workflows) }
            assertThat(provider.isWarmForCurrentOffering()).isTrue
        }

    @Test
    fun `isWarmForCurrentOffering is false when the current offering changes to an uncached workflow`() = runTest {
        stubTopic()
        stubWorkflowBody(WF_PREFETCH)
        stubWorkflowBody(WF_CURRENT)
        provider.warm(generation = 0)
        assertThat(provider.isWarmForCurrentOffering()).isTrue

        // The current offering switches to one whose workflow was not eligible (so its body was not cached).
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

    @Test
    fun `resolveWorkflow returns Found when the topic maps the offering to a workflow`() = runTest {
        every { manager.configGeneration } returns 0
        coEvery { manager.topic(RemoteConfigTopic.Workflows) } returns topicWith(
            WF_CURRENT to configItem(prefetch = false, offeringId = CURRENT_OFFERING),
        )

        assertThat(provider.resolveWorkflow(CURRENT_OFFERING))
            .isEqualTo(WorkflowResolution.Found(WF_CURRENT))
    }

    @Test
    fun `resolveWorkflow returns NoWorkflow when the topic is readable but has no mapping`() = runTest {
        every { manager.configGeneration } returns 0
        coEvery { manager.topic(RemoteConfigTopic.Workflows) } returns topicWith(
            WF_CURRENT to configItem(prefetch = false, offeringId = CURRENT_OFFERING),
        )

        // The topic committed fine; this offering simply has no workflow, so it is workflowless (not unknown).
        assertThat(provider.resolveWorkflow(OTHER_OFFERING)).isEqualTo(WorkflowResolution.NoWorkflow)
    }

    @Test
    fun `resolveWorkflow returns Disabled when the topic cannot be read and remote config is disabled`() = runTest {
        every { manager.configGeneration } returns 0
        coEvery { manager.topic(RemoteConfigTopic.Workflows) } returns null
        // A 4xx kill switch: the offering's components were skipped, so the caller can reload to recover them.
        every { manager.isDisabled } returns true

        assertThat(provider.resolveWorkflow(CURRENT_OFFERING)).isEqualTo(WorkflowResolution.Disabled)
    }

    @Test
    fun `resolveWorkflow returns Disabled from a warm cache when remote config is disabled`() = runTest {
        // Race: on a 4xx kill switch the disabled flag is set before the workflow cache is invalidated, so a
        // concurrent resolution can still see a warm cache. The warm-cache fast path must honor isDisabled and
        // yield Disabled (→ offerings reload) instead of the stale Found/NoWorkflow, or the components skipped
        // while workflows were enabled are never recovered.
        stubTopic()
        stubWorkflowBody(WF_PREFETCH)
        stubWorkflowBody(WF_CURRENT)
        provider.warm(generation = 0)
        assertThat(provider.isWarmForCurrentOffering()).isTrue

        every { manager.isDisabled } returns true

        // CURRENT_OFFERING maps to WF_CURRENT in the warm cache, so the fast path would return Found without the
        // isDisabled guard; OTHER_OFFERING is unmapped and would return NoWorkflow. Both must yield Disabled.
        assertThat(provider.resolveWorkflow(CURRENT_OFFERING)).isEqualTo(WorkflowResolution.Disabled)
        assertThat(provider.resolveWorkflow("unmapped_off")).isEqualTo(WorkflowResolution.Disabled)
    }

    @Test
    fun `resolveWorkflow returns Unavailable when the topic cannot be read and remote config is not disabled`() =
        runTest {
            every { manager.configGeneration } returns 0
            coEvery { manager.topic(RemoteConfigTopic.Workflows) } returns null
            // A transient failure: reloading would recover nothing, so the caller surfaces an error.
            every { manager.isDisabled } returns false

            assertThat(provider.resolveWorkflow(CURRENT_OFFERING)).isEqualTo(WorkflowResolution.Unavailable)
        }

    @Test
    fun `warm notifies onCurrentWorkflowLoaded with only the current offering's workflow`() = runTest {
        var announcedId: String? = null
        val providerWithListener = WorkflowsConfigProvider(
            manager,
            currentOfferingIdProvider = { currentOfferingId },
            onCurrentWorkflowLoaded = { workflowId, _ -> announcedId = workflowId },
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
        )
        stubTopic()
        stubWorkflowBody(WF_PREFETCH)
        stubWorkflowBody(WF_CURRENT)

        providerWithListener.warm(generation = 0)

        // Mirrors the offerings path: only the current offering's workflow is announced for asset prewarming.
        // WF_PREFETCH's bytes are cached too, but a prefetch-only workflow (not the current offering's) is not
        // the paywall about to be shown, so its assets are not warmed.
        assertThat(announcedId).isEqualTo(WF_CURRENT)
    }

    @Test
    fun `warm does not notify onCurrentWorkflowLoaded when the current offering has no workflow`() = runTest {
        var announced = false
        currentOfferingId = "offering_without_workflow"
        val providerWithListener = WorkflowsConfigProvider(
            manager,
            currentOfferingIdProvider = { currentOfferingId },
            onCurrentWorkflowLoaded = { _, _ -> announced = true },
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
        )
        stubTopic()
        stubWorkflowBody(WF_PREFETCH)

        providerWithListener.warm(generation = 0)

        assertThat(announced).isFalse
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
