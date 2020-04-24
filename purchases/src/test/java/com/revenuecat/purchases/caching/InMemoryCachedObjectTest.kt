package com.revenuecat.purchases.caching

import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.utils.DateProvider
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.Date

class InMemoryCachedObjectTest {
    private val epoch = Date(0)


    // region isCacheStale

    @Test
    fun `cache is not set`() {
        val inMemoryCachedObject = InMemoryCachedObject<Offerings>(1)
        assertThat(inMemoryCachedObject.isCacheStale()).isTrue()
    }

    @Test
    fun `cache is stale`() {
        val inMemoryCachedObject = InMemoryCachedObject<Offerings>(1)
        inMemoryCachedObject.updateCacheTimestamp(epoch)
        assertThat(inMemoryCachedObject.isCacheStale()).isTrue()
    }

    @Test
    fun `cache is not stale`() {
        val inMemoryCachedObject = InMemoryCachedObject<Offerings>(100)
        inMemoryCachedObject.updateCacheTimestamp(Date())
        assertThat(inMemoryCachedObject.isCacheStale()).isFalse()
    }

    // endregion

    // region clearCacheTimestamp

    @Test
    fun `cache timestamp is cleared correctly`() {
        val inMemoryCachedObject = InMemoryCachedObject<Offerings>(100)
        val date = Date()
        inMemoryCachedObject.updateCacheTimestamp(date)
        assertThat(inMemoryCachedObject.lastUpdatedAt).isEqualTo(date)
        inMemoryCachedObject.clearCacheTimestamp()
        assertThat(inMemoryCachedObject.lastUpdatedAt).isNull()
    }

    // endregion

    // region clearCache

    @Test
    fun `cache is cleared correctly`() {
        val inMemoryCachedObject = InMemoryCachedObject<Offerings>(100)
        assertThat(inMemoryCachedObject.cachedInstance).isNull()
        inMemoryCachedObject.cacheInstance(mockk())
        inMemoryCachedObject.clearCache()
        assertThat(inMemoryCachedObject.cachedInstance).isNull()
    }

    // endregion

    // region cacheInstance

    @Test
    fun `instance is cached correctly`() {
        val inMemoryCachedObject = InMemoryCachedObject<Offerings>(100)
        assertThat(inMemoryCachedObject.cachedInstance).isNull()
        inMemoryCachedObject.cacheInstance(mockk())
        assertThat(inMemoryCachedObject.cachedInstance).isNotNull
    }

    @Test
    fun `timestamp is set correctly when setting instance`() {
        val now = Date()
        val inMemoryCachedObject = InMemoryCachedObject<Offerings>(100, dateProvider = object : DateProvider {
            override val now: Date
                get() {
                    return now
                }
        })
        assertThat(inMemoryCachedObject.lastUpdatedAt).isNull()
        inMemoryCachedObject.cacheInstance(mockk())
        assertThat(inMemoryCachedObject.lastUpdatedAt).isEqualTo(now)
    }

    // endregion

    // region updateCacheTimestamp

    @Test
    fun `timestamp is set correctly`() {
        val inMemoryCachedObject = InMemoryCachedObject<Offerings>(100)
        val date = Date()
        inMemoryCachedObject.updateCacheTimestamp(date)
        assertThat(inMemoryCachedObject.lastUpdatedAt).isEqualTo(date)
    }

    // endregion

}