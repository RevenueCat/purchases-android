package com.revenuecat.purchases.storage

import com.revenuecat.purchases.utils.CoroutineTest
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DeferredValueStoresTest : CoroutineTest() {
    private val subject = KeyedDeferredValueStore<String, Int>()

    var keyedWasCalled = false

    @Test
    fun `getOrPut sets value`() = runTest {
        subject.getOrPut("X") {
            async { 1 }
        }
        assertThat(subject.deferred["X"]).isNotNull()
    }

    @Test
    fun `getOrPut shared deferred invocation and does not overwrite the original`() = runTest {
        var keyCallCount = 0

        for (i in 1..100) {
            subject.getOrPut("X") {
                async {
                    keyCallCount++
                    i
                }
            }.await()
        }
        assertThat(keyCallCount).isEqualTo(1)
        assertThat(subject.deferred["X"]?.await()).isEqualTo(1)
    }

    @Test
    fun `getOrPut retrieves stored value from value store`() = runTest {
        subject.getOrPut("X") {
            async { 44 }
        }
        subject.getOrPut("X") {
            async {
                keyedWasCalled = true
                1
            }
        }
        val value = subject.deferred["X"]?.await()
        assertThat(44).isEqualTo(value)
        assertThat(keyedWasCalled).isFalse()
    }

    @Test
    fun `getOrPut auto clears failed tasks`() = runTest {
        assertThrows(TestException::class) { subject.getOrPut("X") { throw TestException() }.await() }
        assertThat(subject.deferred["X"]).isNull()
    }
}

class TestException : Throwable()
