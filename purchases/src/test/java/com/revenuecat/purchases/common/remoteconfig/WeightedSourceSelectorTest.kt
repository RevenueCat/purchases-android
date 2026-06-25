package com.revenuecat.purchases.common.remoteconfig

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import kotlin.random.Random

class WeightedSourceSelectorTest {

    // region Initial selection

    @Test
    fun `current is null when there are no sources`() {
        val selector = WeightedSourceSelector(emptyList<TestSource>(), FakeRandom())
        assertThat(selector.current).isNull()
    }

    @Test
    fun `single source is selected`() {
        val only = TestSource("only", priority = 0, weight = 0)
        val selector = WeightedSourceSelector(listOf(only), FakeRandom(0))
        assertThat(selector.current).isSameAs(only)
    }

    @Test
    fun `highest priority wins`() {
        val low = TestSource("low", priority = 0, weight = 100)
        val high = TestSource("high", priority = 10, weight = 1)
        val selector = WeightedSourceSelector(listOf(low, high), FakeRandom(0))
        assertThat(selector.current).isSameAs(high)
    }

    @Test
    fun `highest priority wins regardless of order`() {
        val low1 = TestSource("low1", priority = 0, weight = 100)
        val high = TestSource("high", priority = 5, weight = 1)
        val low2 = TestSource("low2", priority = 0, weight = 100)
        val selector = WeightedSourceSelector(listOf(low1, high, low2), FakeRandom(0))
        assertThat(selector.current).isSameAs(high)
    }

    // endregion

    // region Weighted random tie-breaking (weights [30, 70])

    @Test
    fun `weighted pick at lower boundary picks first`() {
        assertThat(weightedPairSelector(target = 0).current?.id).isEqualTo("a")
    }

    @Test
    fun `weighted pick just below first weight picks first`() {
        assertThat(weightedPairSelector(target = 29).current?.id).isEqualTo("a")
    }

    @Test
    fun `weighted pick at first weight picks second`() {
        assertThat(weightedPairSelector(target = 30).current?.id).isEqualTo("b")
    }

    @Test
    fun `weighted pick at upper boundary picks second`() {
        assertThat(weightedPairSelector(target = 99).current?.id).isEqualTo("b")
    }

    // endregion

    // region Zero / negative weight fallbacks

    @Test
    fun `zero weights use uniform random`() {
        val a = TestSource("a", priority = 0, weight = 0)
        val b = TestSource("b", priority = 0, weight = 0)
        assertThat(WeightedSourceSelector(listOf(a, b), FakeRandom(0)).current).isSameAs(a)
        assertThat(WeightedSourceSelector(listOf(a, b), FakeRandom(1)).current).isSameAs(b)
    }

    @Test
    fun `negative weights are clamped to zero`() {
        // sourceA has a positive weight, sourceB a negative one. sourceB clamps to 0, so sourceA
        // wins the entire range.
        val a = TestSource("a", priority = 0, weight = 50)
        val b = TestSource("b", priority = 0, weight = -50)
        val selector = WeightedSourceSelector(listOf(a, b), FakeRandom(0))
        assertThat(selector.current).isSameAs(a)
    }

    @Test
    fun `negative weight source is never picked at upper boundary`() {
        // Total weight is 50 (sourceB clamps to 0). Even drawing the top of the range, the clamped
        // source contributes no probability mass, so sourceA still wins.
        val a = TestSource("a", priority = 0, weight = 50)
        val b = TestSource("b", priority = 0, weight = -50)
        val selector = WeightedSourceSelector(listOf(a, b), FakeRandom(50))
        assertThat(selector.current).isSameAs(a)
    }

    @Test
    fun `zero weight source is last resort within its priority tier`() {
        // The zero-weight source never wins the weighted draw, but it stays reachable as the tier's
        // last resort: tried after its weighted peer, yet still before any lower-priority source.
        val weighted = TestSource("weighted", priority = 10, weight = 50)
        val zero = TestSource("zero", priority = 10, weight = 0)
        val lowerPriority = TestSource("lower", priority = 0, weight = 100)
        val selector = WeightedSourceSelector(listOf(weighted, zero, lowerPriority), FakeRandom(0))

        assertThat(selector.current).isSameAs(weighted)
        assertThat(selector.advance()).isSameAs(zero)
        assertThat(selector.advance()).isSameAs(lowerPriority)
        assertThat(selector.advance()).isNull()
    }

    @Test
    fun `weights summing beyond Int MAX_VALUE do not overflow`() {
        // The tier's weights overflow Int when summed, so the total saturates to Int.MAX_VALUE
        // instead of overflowing. Drawing target 5 skips a (weight 1) and lands on b (weight
        // Int.MAX_VALUE) without the running subtraction underflowing.
        val a = TestSource("a", priority = 0, weight = 1)
        val b = TestSource("b", priority = 0, weight = Int.MAX_VALUE)
        val selector = WeightedSourceSelector(listOf(a, b), FakeRandom(5))

        assertThat(selector.current).isSameAs(b)
        assertThat(selector.advance()).isSameAs(a)
        assertThat(selector.advance()).isNull()
    }

    // endregion

    // region advance()

    @Test
    fun `advance excludes current and picks next priority tier`() {
        val high = TestSource("high", priority = 10, weight = 1)
        val low = TestSource("low", priority = 0, weight = 1)
        val selector = WeightedSourceSelector(listOf(high, low), FakeRandom(0))

        assertThat(selector.current).isSameAs(high)
        assertThat(selector.advance()).isSameAs(low)
        assertThat(selector.current).isSameAs(low)
    }

    @Test
    fun `advance returns null when sources are exhausted`() {
        val high = TestSource("high", priority = 10, weight = 1)
        val low = TestSource("low", priority = 0, weight = 1)
        val selector = WeightedSourceSelector(listOf(high, low), FakeRandom(0))

        assertThat(selector.advance()).isSameAs(low)
        assertThat(selector.advance()).isNull()
        assertThat(selector.current).isNull()
    }

    @Test
    fun `advance walks tied sources by weight excluding tried`() {
        // sourceA(30) is drawn first via target 0; the rest of the tier is precomputed, so advancing
        // walks to the only remaining source (b) without consuming more randomness.
        val a = TestSource("a", priority = 0, weight = 30)
        val b = TestSource("b", priority = 0, weight = 70)
        val selector = WeightedSourceSelector(listOf(a, b), FakeRandom(0))

        assertThat(selector.current?.id).isEqualTo("a")
        assertThat(selector.advance()?.id).isEqualTo("b")
        assertThat(selector.advance()).isNull()
    }

    @Test
    fun `advance walks full weighted order within tier`() {
        // Three tied sources. The eager order is built by drawing without replacement: target 70
        // skips a(30) and lands on b, then among the remaining [a, c] target 0 picks a, leaving c.
        val a = TestSource("a", priority = 0, weight = 30)
        val b = TestSource("b", priority = 0, weight = 70)
        val c = TestSource("c", priority = 0, weight = 50)
        val selector = WeightedSourceSelector(listOf(a, b, c), FakeRandom(70, 0))

        assertThat(selector.current?.id).isEqualTo("b")
        assertThat(selector.advance()?.id).isEqualTo("a")
        assertThat(selector.advance()?.id).isEqualTo("c")
        assertThat(selector.advance()).isNull()
    }

    // endregion

    // region reset()

    @Test
    fun `reset rewinds to the first source`() {
        val high = TestSource("high", priority = 10, weight = 1)
        val low = TestSource("low", priority = 0, weight = 1)
        val selector = WeightedSourceSelector(listOf(high, low), FakeRandom(0))

        assertThat(selector.advance()).isSameAs(low)
        selector.reset()
        assertThat(selector.current).isSameAs(high)
        assertThat(selector.advance()).isSameAs(low)
    }

    // endregion

    private fun weightedPairSelector(target: Int): WeightedSourceSelector<TestSource> {
        val a = TestSource("a", priority = 0, weight = 30)
        val b = TestSource("b", priority = 0, weight = 70)
        return WeightedSourceSelector(listOf(a, b), FakeRandom(target))
    }

    private data class TestSource(
        val id: String,
        override val priority: Int,
        override val weight: Int,
    ) : WeightedSource

    /**
     * Returns queued values from [nextInt], clamped into range, repeating the last value once the
     * queue is drained. Mirrors the iOS `FakeRandomizer` test helper.
     */
    private class FakeRandom(vararg values: Int) : Random() {
        private val values: IntArray = if (values.isEmpty()) intArrayOf(0) else values
        private var index = 0

        override fun nextBits(bitCount: Int): Int = error("nextBits should not be used")

        override fun nextInt(until: Int): Int {
            val value = if (index < values.size) values[index] else values.last()
            index++
            return value.coerceIn(0, until - 1)
        }
    }
}
