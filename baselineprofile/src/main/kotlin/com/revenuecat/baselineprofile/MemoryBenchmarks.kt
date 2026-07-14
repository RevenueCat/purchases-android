package com.revenuecat.baselineprofile

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Measures target-app post-startup and peak memory during a cold startup. */
@OptIn(ExperimentalMetricApi::class)
@RunWith(AndroidJUnit4::class)
@LargeTest
class MemoryBenchmarks {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun startupMemoryLast() {
        measureMemory(MemoryUsageMetric.Mode.Last)
    }

    @Test
    fun startupMemoryPeak() {
        measureMemory(MemoryUsageMetric.Mode.Max)
    }

    private fun measureMemory(mode: MemoryUsageMetric.Mode) {
        rule.measureRepeated(
            packageName = InstrumentationRegistry.getArguments().getString("targetAppId")
                ?: throw IllegalArgumentException("targetAppId not passed as instrumentation runner arg"),
            metrics = listOf(MemoryUsageMetric(mode)),
            compilationMode = CompilationMode.None(),
            startupMode = StartupMode.COLD,
            iterations = 10,
            setupBlock = { pressHome() },
            measureBlock = { startActivityAndWait() },
        )
    }
}
