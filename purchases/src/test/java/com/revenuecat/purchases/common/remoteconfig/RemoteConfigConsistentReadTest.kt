package com.revenuecat.purchases.common.remoteconfig

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class RemoteConfigConsistentReadTest {
    private val manager = mockk<RemoteConfigManager>()

    @Test
    fun `returns the first result without a retry when the generation is stable`() = runTest {
        every { manager.configGeneration } returns 0
        val generations = mutableListOf<Int>()

        val result = manager.readConsistent(what = { "a value" }) { generation ->
            generations += generation
            "value"
        }

        assertThat(result).isEqualTo("value")
        assertThat(generations).containsExactly(0)
    }

    @Test
    fun `a consistent null result is the answer and does not retry`() = runTest {
        every { manager.configGeneration } returns 0
        var reads = 0

        val result = manager.readConsistent<String>(what = { "a value" }) { reads++; null }

        assertThat(result).isNull()
        assertThat(reads).isEqualTo(1)
    }

    @Test
    fun `retries once against the new generation when the read is superseded`() = runTest {
        every { manager.configGeneration } returnsMany listOf(0, 1)
        val generations = mutableListOf<Int>()

        val result = manager.readConsistent(what = { "a value" }) { generation ->
            generations += generation
            "value at $generation"
        }

        assertThat(result).isEqualTo("value at 1")
        assertThat(generations).containsExactly(0, 1)
    }

    @Test
    fun `retries a superseded read even when it returned null`() = runTest {
        every { manager.configGeneration } returnsMany listOf(0, 1)
        var reads = 0

        val result = manager.readConsistent(what = { "a value" }) { if (reads++ == 0) null else "retried" }

        assertThat(result).isEqualTo("retried")
        assertThat(reads).isEqualTo(2)
    }

    @Test
    fun `returns null after exactly two attempts when both are superseded`() = runTest {
        every { manager.configGeneration } returnsMany listOf(0, 1, 1, 2)
        var reads = 0

        val result = manager.readConsistent(what = { "a value" }) { reads++; "value" }

        assertThat(result).isNull()
        assertThat(reads).isEqualTo(2)
    }
}
