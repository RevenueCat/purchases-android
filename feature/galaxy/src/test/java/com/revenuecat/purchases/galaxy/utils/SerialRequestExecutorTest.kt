package com.revenuecat.purchases.galaxy.utils

import org.assertj.core.api.Assertions
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test

class SerialRequestExecutorTest {

    @Test
    fun `next request waits for previous success`() {
        val executor = SerialRequestExecutor()
        val finishFirst = AtomicReference<() -> Unit>()
        val secondStarted = CountDownLatch(1)

        executor.executeSerially { finish ->
            finishFirst.set(finish)
        }

        executor.executeSerially { finish ->
            secondStarted.countDown()
            finish()
        }

        Assertions.assertThat(secondStarted.await(100, TimeUnit.MILLISECONDS)).isFalse

        finishFirst.get()?.invoke()

        Assertions.assertThat(secondStarted.await(1, TimeUnit.SECONDS)).isTrue
    }

    @Test
    fun `error completion also advances queue`() {
        val executor = SerialRequestExecutor()
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

        Assertions.assertThat(finished.await(1, TimeUnit.SECONDS)).isTrue
        Assertions.assertThat(completedRequests).containsExactly("first", "second")
    }
}