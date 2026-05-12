package com.revenuecat.purchases.common.remoteconfig

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import kotlin.random.Random

class WeightedSourceSelectionTest {

    @Test
    fun `empty list returns null`() {
        val sources: List<TestSource> = emptyList()
        assertThat(sources.selectWeighted(Random.Default)).isNull()
    }

    @Test
    fun `single source with positive weight is selected`() {
        val only = TestSource(priority = 0, weight = 10)
        assertThat(listOf(only).selectWeighted(FakeRandom(0))).isSameAs(only)
        assertThat(listOf(only).selectWeighted(FakeRandom(1))).isSameAs(only)
    }

    @Test
    fun `single source with zero weight is still selected`() {
        val only = TestSource(priority = 0, weight = 0)
        assertThat(listOf(only).selectWeighted(FakeRandom(0))).isSameAs(only)
    }

    @Test
    fun `highest priority wins regardless of weight`() {
        val low = TestSource(priority = 1, weight = 999)
        val high = TestSource(priority = 5, weight = 1)
        assertThat(listOf(low, high).selectWeighted(FakeRandom(0))).isSameAs(high)
    }

    @Test
    fun `weighted pick lands in the first bucket at lower boundary`() {
        val a = TestSource(priority = 0, weight = 30)
        val b = TestSource(priority = 0, weight = 70)
        assertThat(listOf(a, b).selectWeighted(FakeRandom(0))).isSameAs(a)
    }

    @Test
    fun `weighted pick lands in the first bucket just below its upper boundary`() {
        val a = TestSource(priority = 0, weight = 30)
        val b = TestSource(priority = 0, weight = 70)
        assertThat(listOf(a, b).selectWeighted(FakeRandom(29))).isSameAs(a)
    }

    @Test
    fun `weighted pick crosses into the second bucket exactly at boundary`() {
        val a = TestSource(priority = 0, weight = 30)
        val b = TestSource(priority = 0, weight = 70)
        assertThat(listOf(a, b).selectWeighted(FakeRandom(30))).isSameAs(b)
    }

    @Test
    fun `weighted pick lands in the second bucket near upper end`() {
        val a = TestSource(priority = 0, weight = 30)
        val b = TestSource(priority = 0, weight = 70)
        assertThat(listOf(a, b).selectWeighted(FakeRandom(99))).isSameAs(b)
    }

    @Test
    fun `falls back to uniform random when all top priority weights are zero`() {
        val topA = TestSource(priority = 5, weight = 0)
        val topB = TestSource(priority = 5, weight = 0)
        val low = TestSource(priority = 1, weight = 100)
        val sources = listOf(topA, topB, low)

        assertThat(sources.selectWeighted(FakeRandom(0))).isSameAs(topA)
        assertThat(sources.selectWeighted(FakeRandom(1))).isSameAs(topB)
    }

    @Test
    fun `ignores lower priority sources even when their combined weight dominates`() {
        val high = TestSource(priority = 10, weight = 1)
        val low1 = TestSource(priority = 0, weight = 100)
        val low2 = TestSource(priority = 0, weight = 100)
        assertThat(listOf(low1, high, low2).selectWeighted(FakeRandom(0))).isSameAs(high)
    }

    @Test
    fun `negative total weight falls back to uniform random`() {
        val a = TestSource(priority = 0, weight = -10)
        val b = TestSource(priority = 0, weight = 5)
        val sources = listOf(a, b)

        assertThat(sources.selectWeighted(FakeRandom(0))).isSameAs(a)
        assertThat(sources.selectWeighted(FakeRandom(1))).isSameAs(b)
    }

    @Test
    fun `any negative weight falls back to uniform random even when total is positive`() {
        val a = TestSource(priority = 0, weight = -10)
        val b = TestSource(priority = 0, weight = 30)
        val sources = listOf(a, b)

        assertThat(sources.selectWeighted(FakeRandom(0))).isSameAs(a)
        assertThat(sources.selectWeighted(FakeRandom(1))).isSameAs(b)
    }

    @Test
    fun `selectWeightedExcluding skips excluded id within the top priority tier`() {
        val a = TestSource(id = "a", priority = 5, weight = 30)
        val b = TestSource(id = "b", priority = 5, weight = 70)
        val sources = listOf(a, b)

        val picked = sources.selectWeightedExcluding(
            excludedIds = setOf("a"),
            idOf = TestSource::id,
            random = FakeRandom(0),
        )

        assertThat(picked).isSameAs(b)
    }

    @Test
    fun `selectWeightedExcluding drops to the next priority tier when top tier is fully excluded`() {
        val high = TestSource(id = "high", priority = 5, weight = 50)
        val midA = TestSource(id = "midA", priority = 3, weight = 10)
        val midB = TestSource(id = "midB", priority = 3, weight = 90)
        val low = TestSource(id = "low", priority = 1, weight = 99)
        val sources = listOf(high, midA, midB, low)

        val picked = sources.selectWeightedExcluding(
            excludedIds = setOf("high"),
            idOf = TestSource::id,
            random = FakeRandom(9),
        )

        assertThat(picked).isSameAs(midA)
    }

    @Test
    fun `selectWeightedExcluding returns null when every id is excluded`() {
        val sources = listOf(
            TestSource(id = "a", priority = 5, weight = 10),
            TestSource(id = "b", priority = 5, weight = 10),
        )

        val picked = sources.selectWeightedExcluding(
            excludedIds = setOf("a", "b"),
            idOf = TestSource::id,
            random = FakeRandom(0),
        )

        assertThat(picked).isNull()
    }

    private data class TestSource(
        val id: String = "",
        override val priority: Int,
        override val weight: Int,
    ) : WeightedSource

    private class FakeRandom(private val value: Int) : Random() {
        override fun nextBits(bitCount: Int): Int = error("nextBits should not be used")
        override fun nextInt(until: Int): Int = value
    }
}
