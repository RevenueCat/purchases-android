package com.revenuecat.purchases.common.workflows

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.utils.add
import io.mockk.mockk
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
@Config(manifest = Config.NONE)
class WorkflowsCacheTest {

    private val initialDate = Date(1685098228L) // Friday, May 26, 2023 10:50:28 AM GMT
    private lateinit var currentDate: Date
    private lateinit var dateProvider: DateProvider

    private lateinit var workflowsCache: WorkflowsCache

    @Before
    fun setUp() {
        currentDate = initialDate
        dateProvider = object : DateProvider {
            override val now: Date
                get() = currentDate
        }
        workflowsCache = WorkflowsCache(dateProvider = dateProvider)
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
    fun `different workflowIds are cached independently`() {
        val first = mockk<WorkflowDataResult>()
        val second = mockk<WorkflowDataResult>()
        workflowsCache.cacheWorkflow("wf_1", first)
        currentDate = currentDate.add(6.minutes)
        workflowsCache.cacheWorkflow("wf_2", second)

        assertThat(workflowsCache.cachedWorkflow("wf_1")).isSameAs(first)
        assertThat(workflowsCache.cachedWorkflow("wf_2")).isSameAs(second)
        assertThat(workflowsCache.isWorkflowCacheStale("wf_1", appInBackground = false)).isTrue
        assertThat(workflowsCache.isWorkflowCacheStale("wf_2", appInBackground = false)).isFalse
    }

    @Test
    fun `re-caching same workflowId refreshes timestamp`() {
        workflowsCache.cacheWorkflow("wf_1", mockk())
        currentDate = currentDate.add(6.minutes)
        assertThat(workflowsCache.isWorkflowCacheStale("wf_1", appInBackground = false)).isTrue
        workflowsCache.cacheWorkflow("wf_1", mockk())
        assertThat(workflowsCache.isWorkflowCacheStale("wf_1", appInBackground = false)).isFalse
    }
}
