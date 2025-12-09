package com.revenuecat.purchases.galaxy

import org.assertj.core.api.Assertions.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test

class GalaxySerialRequestExecutorTest {

    @Test
    fun `next request waits for previous success`() {
        val executor = GalaxySerialRequestExecutor()
        val finishFirst = AtomicReference<() -> Unit>()
        val secondStarted = CountDownLatch(1)

        executor.executeSerially { finish ->
            finishFirst.set(finish)
        }

        executor.executeSerially { finish ->
            secondStarted.countDown()
            finish()
        }

        assertThat(secondStarted.await(100, TimeUnit.MILLISECONDS)).isFalse

        finishFirst.get()?.invoke()

        assertThat(secondStarted.await(1, TimeUnit.SECONDS)).isTrue
    }

    @Test
    fun `error completion also advances queue`() {
        val executor = GalaxySerialRequestExecutor()
        val completedRequests = CopyOnWriteArrayList<String>()
        val finished = CountDownLatch(1)

        executor.executeSerially { finish ->
            completedRequests.add("first")
            finish()
        }

        executor.executeSerially { finish ->
            completedRequests.add("second")
            finish()
            finished.countDown()
        }

        assertThat(finished.await(1, TimeUnit.SECONDS)).isTrue
        assertThat(completedRequests).containsExactly("first", "second")
    }
}
