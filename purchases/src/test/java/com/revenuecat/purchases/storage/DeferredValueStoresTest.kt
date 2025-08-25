package com.revenuecat.purchases.storage

import com.revenuecat.purchases.utils.CoroutineTest
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DeferredValueStoresTest : CoroutineTest() {
    private val subject = ExampleDeferredStore()

    var integerWasCalled = false
    var keyedWasCalled = false

    @Test
    fun `getOrPut sets value`() = runTest {
        subject.integer.getOrPut { async { 1 } }
        assertThat(subject.integer.deferred).isNotNull()

        subject.keyed.getOrPut("X") {
            async { 1 }
        }
        assertThat(subject.keyed.deferred["X"]).isNotNull()
    }

    @Test
    fun `getOrPut shared deferred invocation`() = runTest {
        var intCallCount = 0
        var keyCallCount = 0

        for (i in 1..100) {
            subject.integer.getOrPut {
                async {
                    intCallCount++
                    1
                }
            }.await()

            subject.keyed.getOrPut("X") {
                async {
                    keyCallCount++
                    1
                }
            }.await()
        }
        assertThat(intCallCount).isEqualTo(1)
        assertThat(keyCallCount).isEqualTo(1)
    }

    @Test
    fun `getOrPut retrieves stored value from single value store`() = runTest {
        subject.integer.getOrPut { async { 44 } }.await()
        subject.integer.getOrPut {
            async {
                integerWasCalled = true
                1
            }
        }

        val value = subject.integer.deferred?.await()
        assertThat(44).isEqualTo(value)
        assertThat(integerWasCalled).isFalse()
    }

    @Test
    fun `getOrPut retrieves stored value from hashed value store`() = runTest {
        subject.keyed.getOrPut("X") {
            async { 44 }
        }
        subject.keyed.getOrPut("X") {
            async {
                keyedWasCalled = true
                1
            }
        }
        val value = subject.keyed.deferred["X"]?.await()
        assertThat(44).isEqualTo(value)
        assertThat(keyedWasCalled).isFalse()
    }

    @Test
    fun `replaceValue will overwrite a previously stored and retrieved value from SingleValueStore`() = runTest {
        subject.integer.getOrPut { async { 44 } }.await()
        subject.integer.replaceValue {
            async {
                integerWasCalled = true
                1
            }
        }
        val value = subject.integer.deferred?.await()
        assertThat(1).isEqualTo(value)
        assertThat(integerWasCalled).isTrue()
    }

    @Test
    fun `replaceValue will overwrite a previously stored and retrieved value from HashedValueStore`() = runTest {
        subject.keyed.getOrPut("X") { async { 44 } }.await()
        subject.keyed.replaceValue("X") {
            async {
                keyedWasCalled = true
                1
            }
        }
        val value = subject.keyed.deferred["X"]?.await()
        assertThat(1).isEqualTo(value)
        assertThat(keyedWasCalled).isTrue()
    }

    @Test
    fun `clear removes values`() = runTest {
        subject.integer.getOrPut { async { 1 } }
        subject.keyed.getOrPut("X") {
            async { 44 }
        }

        subject.integer.clear()
        subject.keyed.clear()

        val integer = subject.integer.deferred?.await()
        val hash = subject.keyed.deferred["X"]?.await()

        assertThat(integer).isNull()
        assertThat(hash).isNull()
    }

    @Test
    fun `getOrPut auto clears failed tasks`() = runTest {

        assertThrows(TestException::class) { subject.integer.getOrPut { throw TestException() }.await() }
        assertThat(subject.integer.deferred).isNull()
        assertThrows(TestException::class) { subject.keyed.getOrPut("X") { throw TestException() }.await() }
        assertThat(subject.keyed.deferred["X"]).isNull()
    }

    @Test
    fun `replaceValue auto clears failed tasks`() = runTest {
        assertThrows(TestException::class) { subject.integer.replaceValue { throw TestException() }.await() }
        assertThat(subject.integer.deferred).isNull()
        assertThrows(TestException::class) { subject.keyed.replaceValue("X") { throw TestException() }.await() }
        assertThat(subject.keyed.deferred["X"]).isNull()
    }

    private data class ExampleDeferredStore(
        val lock: Any = object {},
        val integer: SingleDeferredValueStore<Int> = SingleDeferredValueStore(lock),
        val keyed: KeyedDeferredValueStore<String, Int> = KeyedDeferredValueStore(lock),
    )
}

class TestException : Throwable()
