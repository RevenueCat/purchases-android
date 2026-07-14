package com.revenuecat.purchases.common.remoteconfig

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class GenerationGuardedCacheTest {

    @Test
    fun `starts cold`() {
        val cache = GenerationGuardedCache<String>()

        assertThat(cache.cached).isNull()
        assertThat(cache.isWarm()).isFalse
    }

    @Test
    fun `store makes the cache warm`() {
        val cache = GenerationGuardedCache<String>()

        cache.store(generation = 0, newValue = "a")

        assertThat(cache.cached).isEqualTo("a")
        assertThat(cache.isWarm()).isTrue
    }

    @Test
    fun `store applies an equal generation`() {
        val cache = GenerationGuardedCache<String>()
        cache.store(generation = 3, newValue = "a")

        cache.store(generation = 3, newValue = "b")

        assertThat(cache.cached).isEqualTo("b")
    }

    @Test
    fun `store applies a newer generation`() {
        val cache = GenerationGuardedCache<String>()
        cache.store(generation = 3, newValue = "a")

        cache.store(generation = 4, newValue = "b")

        assertThat(cache.cached).isEqualTo("b")
    }

    @Test
    fun `a lower-generation store does not clobber a higher-generation value`() {
        val cache = GenerationGuardedCache<String>()
        cache.store(generation = 5, newValue = "high")

        cache.store(generation = 2, newValue = "low")

        assertThat(cache.cached).isEqualTo("high")
    }

    @Test
    fun `invalidate clears the value and advances the generation`() {
        val cache = GenerationGuardedCache<String>()
        cache.store(generation = 1, newValue = "a")

        cache.invalidate(generation = 2)

        assertThat(cache.cached).isNull()
        assertThat(cache.isWarm()).isFalse
    }

    @Test
    fun `a stale store cannot repopulate after a newer invalidation`() {
        val cache = GenerationGuardedCache<String>()

        cache.invalidate(generation = 5)
        cache.store(generation = 3, newValue = "stale")

        assertThat(cache.cached).isNull()
    }

    @Test
    fun `isAtOrAbove reflects the newest acted-on generation`() {
        val cache = GenerationGuardedCache<String>()
        assertThat(cache.isAtOrAbove(0)).isFalse

        cache.store(generation = 4, newValue = "a")

        assertThat(cache.isAtOrAbove(3)).isTrue
        assertThat(cache.isAtOrAbove(4)).isTrue
        assertThat(cache.isAtOrAbove(5)).isFalse
    }

    @Test
    fun `isCurrent is true only until a newer generation is acted on`() {
        val cache = GenerationGuardedCache<String>()
        // Nothing acted on yet: any non-negative snapshot is still current.
        assertThat(cache.isCurrent(0)).isTrue

        cache.store(generation = 4, newValue = "a")

        // A snapshot taken at or after the acted-on generation is still current.
        assertThat(cache.isCurrent(4)).isTrue
        assertThat(cache.isCurrent(5)).isTrue
        // A snapshot taken before a newer store/invalidation is stale.
        assertThat(cache.isCurrent(3)).isFalse
    }
}
