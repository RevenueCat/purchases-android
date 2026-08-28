package com.revenuecat.purchases.perf

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.reflect.Method

/**
 * Measures the allocation cost of one cold `configure()` + `getOfferings()` cycle on the default
 * (remote-config-on) path, summed across every thread the SDK touches — not just the calling
 * thread, since config/offerings work runs on background executors and coroutine dispatchers, so a
 * single-thread measurement would miss most of it. Complements [GetOfferingsPerfTest]'s round-trip
 * count gate: a change that keeps the same 3 requests but starts allocating far more (an extra
 * payload copy, a leak-shaped retry, an unbounded buffer) would slip past that gate but trip this
 * one.
 *
 * Allocation is tracked via the JDK's `com.sun.management.ThreadMXBean#getThreadAllocatedBytes`,
 * which counts cumulative bytes allocated per thread and is unaffected by GC timing. Accessed via
 * reflection — including `java.lang.management.ThreadMXBean` itself and
 * `java.lang.management.ManagementFactory` — because this module's Kotlin JVM target (1.8)
 * restricts the compile-time JDK API surface to `java.base`, which doesn't expose the
 * `java.management`/`jdk.management` modules these types live in. Same pattern as
 * `ETagManagerMemoryTest`, extended here to sum across all threads instead of just the current one.
 */
@RunWith(RobolectricTestRunner::class)
@OptIn(InternalRevenueCatAPI::class)
class GetOfferingsMemoryTest {

    private companion object {
        // Measured 2026-08-28 on this machine (two full standalone runs of this test):
        //   run 1 cold-cycle median = 2_875_288 bytes, run 2 cold-cycle median = 2_874_648 bytes
        //   (0.02% apart — stable run to run). Budget = ~median * 1.3, rounded to a clean number,
        // to absorb ordinary run-to-run noise (GC bookkeeping, JIT warm-up differences, Robolectric
        // shadow allocations) without masking a real regression. If this legitimately grows (e.g. a
        // deliberate new payload copy on the config/offerings path), re-baseline deliberately:
        // re-run this test standalone a few times, take the new median, and update this constant
        // with a fresh comment documenting the new observed value and date — never silently raise
        // it just to make CI pass.
        const val MAX_COLD_CYCLE_ALLOCATED_BYTES = 3_800_000L

        const val COLD_SAMPLES = 3
        const val TOP_THREADS_TO_PRINT = 8

        val managementFactoryGetThreadMXBean: Method =
            Class.forName("java.lang.management.ManagementFactory").getMethod("getThreadMXBean")
        val threadMXBeanGetAllThreadIds: Method =
            Class.forName("java.lang.management.ThreadMXBean").getMethod("getAllThreadIds")
        val getThreadAllocatedBytesMethod: Method =
            Class.forName("com.sun.management.ThreadMXBean")
                .getMethod("getThreadAllocatedBytes", Long::class.javaPrimitiveType)
    }

    private data class MeasuredCycle(
        val result: CycleResult,
        val allocatedBytes: Long,
        val elapsedMs: Long,
        val perThreadDeltas: Map<Long, Long>,
    )

    private val server = MockWebServer()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun harness() = PerfHarness(context, server)

    @After fun tearDown() {
        Purchases.backingFieldSharedInstance?.close()
        Purchases.proxyURL = null
        server.shutdown()
    }

    @Test
    fun coldCycleAllocationsStayUnderBudget() {
        server.dispatcher = PerfFixtures.dispatcher(server.url("/").toString())
        assertAllocationTrackingAvailable()

        // Warm-up: first-use classloading/static-init allocations (multi-MB: Kotlin reflection,
        // OkHttp/MockWebServer, Robolectric shadows, coroutine dispatcher machinery) happen once
        // here and are discarded, so they aren't misattributed to the measured cycles below. This
        // is essential — without it, the first measured sample is dominated by one-time setup cost.
        harness().runCycle(cold = true)

        // Cold cycle: median of COLD_SAMPLES samples. PerfHarness.runCycle(cold = true) closes the
        // shared instance and wipes disk at its very start, so each sample starts from a clean
        // singleton — no extra teardown needed between samples here.
        val coldSamples = (1..COLD_SAMPLES).map { measureCycle(cold = true) }
        val coldMedian = coldSamples.map { it.allocatedBytes }.sorted()[coldSamples.size / 2]
        val coldElapsedMedian = coldSamples.map { it.elapsedMs }.sorted()[coldSamples.size / 2]

        // Warm cycle: reuses the disk state left behind by the last cold sample above.
        val warm = measureCycle(cold = false)

        println(
            "PERF_MEMORY cold_median_bytes=$coldMedian " +
                "cold_samples_bytes=${coldSamples.map { it.allocatedBytes }} " +
                "warm_bytes=${warm.allocatedBytes} budget_bytes=$MAX_COLD_CYCLE_ALLOCATED_BYTES",
        )
        // Wall-clock time is informational only — printed for humans, never asserted (see README:
        // CI machine speed makes absolute-ms assertions flaky).
        println(
            "PERF_MEMORY_ELAPSED cold_median_elapsedMs=$coldElapsedMedian warm_elapsedMs=${warm.elapsedMs} " +
                "(not asserted)",
        )
        printTopThreads("cold (last sample)", coldSamples.last().perThreadDeltas)
        printTopThreads("warm", warm.perThreadDeltas)

        assertThat(coldMedian)
            .withFailMessage(
                "Cold configure()+getOfferings() allocated %d bytes across all threads (budget %d). If " +
                    "this is a deliberate change to the config/offerings path, re-baseline " +
                    "MAX_COLD_CYCLE_ALLOCATED_BYTES rather than silently raising it — see the comment on " +
                    "that constant.",
                coldMedian, MAX_COLD_CYCLE_ALLOCATED_BYTES,
            )
            .isLessThan(MAX_COLD_CYCLE_ALLOCATED_BYTES)
    }

    private fun measureCycle(cold: Boolean): MeasuredCycle {
        val before = snapshotAllocatedBytesByThread()
        val result = harness().runCycle(cold = cold)
        val after = snapshotAllocatedBytesByThread()

        assertThat(result.error).isNull()
        assertThat(result.offeringsCount).isGreaterThan(0)

        // Threads created during the measurement count in full (before[id] defaults to 0). A thread
        // that terminated mid-measurement is undercounted: it won't appear in `after` at all (its ID
        // is gone from getAllThreadIds() by the time we snapshot), so its allocations between the
        // last live read and its termination are lost. Documented, accepted limitation — see class
        // KDoc.
        val perThreadDeltas = after.entries.associate { (id, bytesAfter) ->
            id to (bytesAfter - (before[id] ?: 0L))
        }
        val totalDelta = perThreadDeltas.values.sum()
        return MeasuredCycle(result, totalDelta, result.elapsedMs, perThreadDeltas)
    }

    private fun printTopThreads(label: String, perThreadDeltas: Map<Long, Long>) {
        // Names for threads still alive at this call. A short-lived thread that already exited
        // (e.g. a one-shot executor task) won't be in this map and prints as "(thread exited)".
        val namesById = Thread.getAllStackTraces().keys.associate { it.id to it.name }
        val top = perThreadDeltas.entries.sortedByDescending { it.value }.take(TOP_THREADS_TO_PRINT)
        println("PERF_MEMORY_THREADS ($label):")
        top.forEach { (id, bytes) ->
            val name = namesById[id] ?: "(thread exited)"
            println("  ${bytes.toString().padStart(10)} bytes  id=$id  $name")
        }
    }

    // -1 means allocation tracking is disabled/unsupported on this JVM, which would make the gate
    // pass vacuously (every delta measures 0). Fail loudly instead, checked once up front on the
    // current thread (guaranteed alive), before any sample is measured.
    private fun assertAllocationTrackingAvailable() {
        val reading = getThreadAllocatedBytes(threadMXBean(), Thread.currentThread().id)
        check(reading >= 0) {
            "ThreadMXBean allocation tracking is unavailable on this JVM; the memory gate cannot run."
        }
    }

    private fun snapshotAllocatedBytesByThread(): Map<Long, Long> {
        val bean = threadMXBean()
        return allThreadIds(bean).asIterable().mapNotNull { id ->
            val bytes = getThreadAllocatedBytes(bean, id)
            // A thread that terminates between getAllThreadIds() and this read reports -1 (per the
            // JDK docs: "does not exist, is not started, or is terminated"). Skip it here rather
            // than fail the whole measurement — this is the documented per-thread undercount noted
            // in the class KDoc, distinct from tracking being unavailable altogether (checked once,
            // up front, by assertAllocationTrackingAvailable).
            if (bytes >= 0) id to bytes else null
        }.toMap()
    }

    // Reflection because jvmTarget 1.8 hides java.management at compile time; see the class KDoc.
    private fun threadMXBean(): Any = managementFactoryGetThreadMXBean.invoke(null)!!

    private fun allThreadIds(bean: Any): LongArray = threadMXBeanGetAllThreadIds.invoke(bean) as LongArray

    private fun getThreadAllocatedBytes(bean: Any, threadId: Long): Long =
        getThreadAllocatedBytesMethod.invoke(bean, threadId) as Long
}
