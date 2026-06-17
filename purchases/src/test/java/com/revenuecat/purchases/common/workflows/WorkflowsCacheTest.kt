package com.revenuecat.purchases.common.workflows

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.utils.add
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@OptIn(InternalRevenueCatAPI::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], manifest = Config.NONE)
class WorkflowsCacheTest {

    private val initialDate = Date(1685098228L) // Friday, May 26, 2023 10:50:28 AM GMT
    private lateinit var currentDate: Date
    private lateinit var dateProvider: DateProvider
    private lateinit var deviceCache: DeviceCache

    private lateinit var workflowsCache: WorkflowsCache

    @Before
    fun setUp() {
        currentDate = initialDate
        dateProvider = object : DateProvider {
            override val now: Date
                get() = currentDate
        }
        deviceCache = mockk(relaxed = true)
        workflowsCache = WorkflowsCache(deviceCache = deviceCache, dateProvider = dateProvider)
    }

    @Test
    fun `cachedWorkflow returns null when nothing cached`() {
        assertThat(workflowsCache.cachedWorkflow("wf_1")).isNull()
    }

    @Test
    fun `cachedWorkflow returns cached value after cacheWorkflow`() {
        val result = mockk<WorkflowDataResult>()
        workflowsCache.cacheWorkflow("wf_1", result)
        assertThat(workflowsCache.cachedWorkflow("wf_1")).isSameAs(result)
    }

    @Test
    fun `isWorkflowCacheStale is true when nothing cached`() {
        assertThat(workflowsCache.isWorkflowCacheStale("wf_1", appInBackground = false)).isTrue
        assertThat(workflowsCache.isWorkflowCacheStale("wf_1", appInBackground = true)).isTrue
    }

    @Test
    fun `isWorkflowCacheStale is false right after caching in foreground`() {
        workflowsCache.cacheWorkflow("wf_1", mockk())
        assertThat(workflowsCache.isWorkflowCacheStale("wf_1", appInBackground = false)).isFalse
    }

    @Test
    fun `isWorkflowCacheStale is true after foreground TTL expires`() {
        workflowsCache.cacheWorkflow("wf_1", mockk())
        currentDate = currentDate.add(6.minutes)
        assertThat(workflowsCache.isWorkflowCacheStale("wf_1", appInBackground = false)).isTrue
    }

    @Test
    fun `isWorkflowCacheStale is false after foreground TTL expires when in background`() {
        workflowsCache.cacheWorkflow("wf_1", mockk())
        currentDate = currentDate.add(6.minutes)
        assertThat(workflowsCache.isWorkflowCacheStale("wf_1", appInBackground = true)).isFalse
    }

    @Test
    fun `isWorkflowCacheStale is true after background TTL expires`() {
        workflowsCache.cacheWorkflow("wf_1", mockk())
        currentDate = currentDate.add(26.hours)
        assertThat(workflowsCache.isWorkflowCacheStale("wf_1", appInBackground = true)).isTrue
    }

    @Test
    fun `invalidateWorkflowTimestamp makes a fresh entry stale`() {
        workflowsCache.cacheWorkflow("wf_1", mockk())
        assertThat(workflowsCache.isWorkflowCacheStale("wf_1", appInBackground = false)).isFalse

        workflowsCache.invalidateWorkflowTimestamp("wf_1")

        assertThat(workflowsCache.isWorkflowCacheStale("wf_1", appInBackground = false)).isTrue
        // The value itself is retained; only its freshness is cleared.
        assertThat(workflowsCache.cachedWorkflow("wf_1")).isNotNull
    }

    @Test
    fun `invalidateWorkflowTimestamp is a no-op when nothing is cached`() {
        workflowsCache.invalidateWorkflowTimestamp("wf_missing")
        assertThat(workflowsCache.cachedWorkflow("wf_missing")).isNull()
    }

    @Test
    fun `clearCache removes all cached workflows`() {
        workflowsCache.cacheWorkflow("wf_1", mockk())
        workflowsCache.cacheWorkflow("wf_2", mockk())
        workflowsCache.clearCache()
        assertThat(workflowsCache.cachedWorkflow("wf_1")).isNull()
        assertThat(workflowsCache.cachedWorkflow("wf_2")).isNull()
        assertThat(workflowsCache.isWorkflowCacheStale("wf_1", appInBackground = false)).isTrue
        assertThat(workflowsCache.isWorkflowCacheStale("wf_2", appInBackground = false)).isTrue
    }

    @Test
    fun `isWorkflowsListCacheStale is true initially and false after caching`() {
        assertThat(workflowsCache.isWorkflowsListCacheStale(appInBackground = false)).isTrue
        workflowsCache.cacheWorkflowsList(WorkflowsListResponse(workflows = emptyList()), emptyMap())
        assertThat(workflowsCache.isWorkflowsListCacheStale(appInBackground = false)).isFalse
    }

    @Test
    fun `isWorkflowsListCacheStale is true after foreground TTL expires`() {
        workflowsCache.cacheWorkflowsList(WorkflowsListResponse(workflows = emptyList()), emptyMap())
        currentDate = currentDate.add(6.minutes)
        assertThat(workflowsCache.isWorkflowsListCacheStale(appInBackground = false)).isTrue
    }

    @Test
    fun `workflowIdForOfferingId returns mapped id or null`() {
        workflowsCache.cacheWorkflowsList(
            WorkflowsListResponse(workflows = emptyList()),
            mapOf("default" to "wf_1"),
        )
        assertThat(workflowsCache.workflowIdForOfferingId("default")).isEqualTo("wf_1")
        assertThat(workflowsCache.workflowIdForOfferingId("premium")).isNull()
    }

    @Test
    fun `clearCache resets workflows list staleness and offeringId map`() {
        workflowsCache.cacheWorkflowsList(
            WorkflowsListResponse(workflows = emptyList()),
            mapOf("default" to "wf_1"),
        )
        workflowsCache.clearCache()
        assertThat(workflowsCache.isWorkflowsListCacheStale(appInBackground = false)).isTrue
        assertThat(workflowsCache.workflowIdForOfferingId("default")).isNull()
    }

    @Test
    fun `cacheWorkflowsList persists the response to disk`() {
        workflowsCache.cacheWorkflowsList(
            WorkflowsListResponse(
                workflows = listOf(
                    WorkflowSummary(id = "wf_1", displayName = "Flow", offeringId = "default", prefetch = false),
                ),
            ),
            mapOf("default" to "wf_1"),
        )
        verify(exactly = 1) { deviceCache.cacheWorkflowsListResponse(any()) }
    }

    @Test
    fun `cacheWorkflowsListInMemory populates in-memory state without writing to disk`() {
        workflowsCache.cacheWorkflowsListInMemory(
            WorkflowsListResponse(
                workflows = listOf(
                    WorkflowSummary(id = "wf_1", displayName = "Flow", offeringId = "default", prefetch = false),
                ),
            ),
            mapOf("default" to "wf_1"),
        )
        assertThat(workflowsCache.isWorkflowsListCacheStale(appInBackground = false)).isFalse
        assertThat(workflowsCache.workflowIdForOfferingId("default")).isEqualTo("wf_1")
        verify(exactly = 0) { deviceCache.cacheWorkflowsListResponse(any()) }
    }

    @Test
    fun `clearCache clears the disk cache`() {
        workflowsCache.clearCache()
        verify(exactly = 1) { deviceCache.clearWorkflowsListResponseCache() }
    }

    @Test
    fun `clearCache clears the persisted workflow detail envelopes`() {
        workflowsCache.clearCache()
        verify(exactly = 1) { deviceCache.clearWorkflowDetailEnvelopesCache() }
    }

    @Test
    fun `cachedWorkflowsListResponseFromDisk parses the persisted response`() {
        val cachedJson =
            """{"workflows":[{"id":"wf_1","display_name":"Flow","offering_id":"default","prefetch":false}]}"""
        every { deviceCache.getWorkflowsListResponseCache() } returns cachedJson
        val response = workflowsCache.cachedWorkflowsListResponseFromDisk()
        assertThat(response?.workflows?.single()?.id).isEqualTo("wf_1")
    }

    @Test
    fun `cachedWorkflowsListResponseFromDisk returns null when nothing is cached`() {
        every { deviceCache.getWorkflowsListResponseCache() } returns null
        assertThat(workflowsCache.cachedWorkflowsListResponseFromDisk()).isNull()
    }

    @Test
    fun `cachedWorkflowsListResponseFromDisk returns null when the payload is corrupt`() {
        every { deviceCache.getWorkflowsListResponseCache() } returns "not valid json"
        assertThat(workflowsCache.cachedWorkflowsListResponseFromDisk()).isNull()
    }

    private fun cdnEnvelope(url: String, hash: String = "h"): WorkflowDetailResponse =
        WorkflowDetailResponse(
            action = WorkflowResponseAction.USE_CDN,
            url = url,
            hash = hash,
            enrolledVariants = mapOf("exp" to "variant_a"),
        )

    @Test
    fun `cacheWorkflowDetailEnvelope persists the envelope keyed by workflowId`() {
        every { deviceCache.getWorkflowDetailEnvelopesCache() } returns null
        val payloadSlot = slot<String>()
        every { deviceCache.cacheWorkflowDetailEnvelopes(capture(payloadSlot)) } just Runs

        workflowsCache.cacheWorkflowDetailEnvelope("wf_1", cdnEnvelope("https://cdn/wf_1.json"))

        assertThat(payloadSlot.captured).contains("wf_1")
        assertThat(payloadSlot.captured).contains("https://cdn/wf_1.json")
    }

    @Test
    fun `cacheWorkflowDetailEnvelope merges into existing persisted envelopes`() {
        every { deviceCache.getWorkflowDetailEnvelopesCache() } returns
            """{"wf_existing":{"action":"use_cdn","url":"u0","hash":"h0"}}"""
        val payloadSlot = slot<String>()
        every { deviceCache.cacheWorkflowDetailEnvelopes(capture(payloadSlot)) } just Runs

        workflowsCache.cacheWorkflowDetailEnvelope("wf_new", cdnEnvelope("https://cdn/wf_new.json"))

        val persisted = WorkflowJsonParser.parseWorkflowDetailEnvelopes(payloadSlot.captured)
        assertThat(persisted.keys).containsExactlyInAnyOrder("wf_existing", "wf_new")
        assertThat(persisted.getValue("wf_existing").url).isEqualTo("u0")
        assertThat(persisted.getValue("wf_new").url).isEqualTo("https://cdn/wf_new.json")
    }

    @Test
    fun `cachedWorkflowDetailEnvelopesFromDisk parses persisted envelopes`() {
        every { deviceCache.getWorkflowDetailEnvelopesCache() } returns
            """{"wf_1":{"action":"use_cdn","url":"https://cdn/x.json","hash":"h","enrolled_variants":{"e":"v"}}}"""

        val envelopes = workflowsCache.cachedWorkflowDetailEnvelopesFromDisk()

        assertThat(envelopes).containsKey("wf_1")
        assertThat(envelopes!!.getValue("wf_1").url).isEqualTo("https://cdn/x.json")
        assertThat(envelopes.getValue("wf_1").enrolledVariants).isEqualTo(mapOf("e" to "v"))
    }

    @Test
    fun `cachedWorkflowDetailEnvelopesFromDisk round-trips an INLINE envelope with its workflow data`() {
        val inlineJson = """
            {"wf_inline":{"action":"inline","data":{
              "id":"wf_inline","display_name":"Test","initial_step_id":"step_1",
              "steps":{"step_1":{"id":"step_1","type":"screen"}},
              "screens":{},
              "ui_config":{"app":{"colors":{},"fonts":{}},"localizations":{},"variable_config":{}}
            }}}
        """.trimIndent()
        every { deviceCache.getWorkflowDetailEnvelopesCache() } returns inlineJson

        val envelopes = workflowsCache.cachedWorkflowDetailEnvelopesFromDisk()

        assertThat(envelopes!!.getValue("wf_inline").action).isEqualTo(WorkflowResponseAction.INLINE)
        assertThat(envelopes.getValue("wf_inline").data?.id).isEqualTo("wf_inline")
    }

    @Test
    fun `cachedWorkflowDetailEnvelopesFromDisk returns null when nothing is cached`() {
        every { deviceCache.getWorkflowDetailEnvelopesCache() } returns null
        assertThat(workflowsCache.cachedWorkflowDetailEnvelopesFromDisk()).isNull()
    }

    @Test
    fun `cachedWorkflowDetailEnvelopesFromDisk returns null when the payload is corrupt`() {
        every { deviceCache.getWorkflowDetailEnvelopesCache() } returns "not valid json"
        assertThat(workflowsCache.cachedWorkflowDetailEnvelopesFromDisk()).isNull()
    }

    @Test
    fun `cachedWorkflowDetailEnvelopeFromDisk returns the matching envelope`() {
        every { deviceCache.getWorkflowDetailEnvelopesCache() } returns
            """{"wf_1":{"action":"use_cdn","url":"https://cdn/x.json","hash":"h"},"wf_2":{"action":"use_cdn","url":"u2","hash":"h2"}}"""

        val envelope = workflowsCache.cachedWorkflowDetailEnvelopeFromDisk("wf_1")

        assertThat(envelope).isNotNull
        assertThat(envelope!!.url).isEqualTo("https://cdn/x.json")
    }

    @Test
    fun `cachedWorkflowDetailEnvelopeFromDisk returns null for an absent key`() {
        every { deviceCache.getWorkflowDetailEnvelopesCache() } returns
            """{"wf_1":{"action":"use_cdn","url":"https://cdn/x.json","hash":"h"}}"""

        assertThat(workflowsCache.cachedWorkflowDetailEnvelopeFromDisk("wf_missing")).isNull()
    }

    @Test
    fun `cacheWorkflowsList prunes persisted envelopes not in the new list`() {
        every { deviceCache.getWorkflowDetailEnvelopesCache() } returns
            """{"wf_old":{"action":"use_cdn","url":"u1","hash":"h1"},"wf_keep":{"action":"use_cdn","url":"u2","hash":"h2"}}"""
        val payloadSlot = slot<String>()
        every { deviceCache.cacheWorkflowDetailEnvelopes(capture(payloadSlot)) } just Runs

        workflowsCache.cacheWorkflowsList(
            WorkflowsListResponse(
                workflows = listOf(
                    WorkflowSummary(id = "wf_keep", displayName = "Keep", offeringId = "default", prefetch = true),
                ),
            ),
            mapOf("default" to "wf_keep"),
        )

        assertThat(payloadSlot.captured).contains("wf_keep")
        assertThat(payloadSlot.captured).doesNotContain("wf_old")
    }

    @Test
    fun `cacheWorkflowsList does not rewrite envelopes when nothing is pruned`() {
        every { deviceCache.getWorkflowDetailEnvelopesCache() } returns
            """{"wf_keep":{"action":"use_cdn","url":"u","hash":"h"}}"""

        workflowsCache.cacheWorkflowsList(
            WorkflowsListResponse(
                workflows = listOf(
                    WorkflowSummary(id = "wf_keep", displayName = "Keep", offeringId = "default", prefetch = true),
                ),
            ),
            mapOf("default" to "wf_keep"),
        )

        verify(exactly = 0) { deviceCache.cacheWorkflowDetailEnvelopes(any()) }
    }

    @Test
    fun `different workflowIds are cached independently`() {
        val first = mockk<WorkflowDataResult>()
        val second = mockk<WorkflowDataResult>()
        workflowsCache.cacheWorkflow("wf_1", first)
        currentDate = currentDate.add(6.minutes)
        workflowsCache.cacheWorkflow("wf_2", second)

        // Both values remain retrievable.
        assertThat(workflowsCache.cachedWorkflow("wf_1")).isSameAs(first)
        assertThat(workflowsCache.cachedWorkflow("wf_2")).isSameAs(second)

        // wf_1 was cached 6 minutes ago so it is stale in foreground; wf_2 is fresh.
        assertThat(workflowsCache.isWorkflowCacheStale("wf_1", appInBackground = false)).isTrue
        assertThat(workflowsCache.isWorkflowCacheStale("wf_2", appInBackground = false)).isFalse
    }
}
