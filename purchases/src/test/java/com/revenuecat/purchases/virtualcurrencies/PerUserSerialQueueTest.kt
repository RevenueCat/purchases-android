package com.revenuecat.purchases.virtualcurrencies

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PerUserSerialQueueTest {

    private val queue = PerUserSerialQueue()
    private val started = mutableListOf<String>()
    private val doneFunctions = mutableMapOf<String, () -> Unit>()

    private fun enqueue(appUserID: String, name: String) = queue.enqueue(appUserID) { done ->
        started.add(name)
        doneFunctions[name] = done
    }

    @Test
    fun `a user's second operation starts only when the first is done`() {
        enqueue("user", "first")
        enqueue("user", "second")

        assertThat(started).containsExactly("first")

        doneFunctions.getValue("first")()

        assertThat(started).containsExactly("first", "second")
    }

    @Test
    fun `operations for different users don't wait for each other`() {
        enqueue("alice", "alice's")
        enqueue("bob", "bob's")

        assertThat(started).containsExactly("alice's", "bob's")
    }

    @Test
    fun `calling done twice doesn't start an operation early`() {
        enqueue("user", "first")
        enqueue("user", "second")
        enqueue("user", "third")

        doneFunctions.getValue("first")()
        doneFunctions.getValue("first")()

        assertThat(started).containsExactly("first", "second")
    }

    @Test
    fun `the queue keeps working after it empties`() {
        enqueue("user", "first")
        doneFunctions.getValue("first")()

        enqueue("user", "second")

        assertThat(started).containsExactly("first", "second")
    }
}
