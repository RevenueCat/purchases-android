package com.revenuecat.purchases.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class HashUtilsTest {

    @Test
    fun `md5Hex returns the expected lowercase hex digest`() {
        assertThat("abc".toByteArray().md5Hex()).isEqualTo("900150983cd24fb0d6963f7d28e17f72")
        assertThat(ByteArray(0).md5Hex()).isEqualTo("d41d8cd98f00b204e9800998ecf8427e")
    }

    @Test
    fun `md5Hex pads bytes that need a leading zero`() {
        // The first digest byte here is 0x0c, so this catches a format that drops the leading zero.
        assertThat("a".toByteArray().md5Hex()).isEqualTo("0cc175b9c0f1b6a831c399e269772661")
    }

    /**
     * Regression test for the concurrency crash in FontLoader and DefaultFileRepository. Both used to hold one
     * shared MessageDigest and call digest() from several coroutines at the same time. MessageDigest is not
     * thread-safe, so the internal buffer got corrupted and threw, most often ArrayIndexOutOfBoundsException
     * from the digest's own state.
     *
     * Against a shared instance this fails well within these iteration counts. Against a per-call instance it
     * cannot fail, because there is no shared state left.
     */
    @Test
    fun `md5Hex is thread safe when called concurrently`() {
        val input = "https://assets.pawwalls.com/fonts/Regular.ttf".toByteArray()
        val expected = input.md5Hex()

        val pool = Executors.newFixedThreadPool(THREADS)
        val startSignal = CountDownLatch(1)
        val finished = CountDownLatch(THREADS)
        val failures = Collections.synchronizedList(mutableListOf<Throwable>())

        repeat(THREADS) {
            pool.execute {
                try {
                    startSignal.await()
                    repeat(ITERATIONS_PER_THREAD) {
                        assertThat(input.md5Hex()).isEqualTo(expected)
                    }
                } catch (@Suppress("TooGenericExceptionCaught") t: Throwable) {
                    failures.add(t)
                } finally {
                    finished.countDown()
                }
            }
        }

        startSignal.countDown()
        val completed = finished.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        pool.shutdownNow()

        assertThat(completed).isTrue()
        assertThat(failures).isEmpty()
    }

    private companion object {
        const val THREADS = 8
        const val ITERATIONS_PER_THREAD = 2000
        const val TIMEOUT_SECONDS = 30L
    }
}
