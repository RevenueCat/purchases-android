package com.revenuecat.perftester

import android.os.Debug
import java.io.File

/** Process memory sampled before configuration and after all requested readiness work completes. */
internal data class ProcessMemorySnapshot(
    val rssKb: Long?,
    val rssAnonKb: Long?,
    val rssFileKb: Long?,
    val javaHeapKb: Long?,
    val nativeHeapKb: Long?,
) {

    companion object {
        fun capture(): ProcessMemorySnapshot {
            val status = runCatching {
                File("/proc/self/status").useLines { lines ->
                    lines.mapNotNull { line ->
                        val separator = line.indexOf(':')
                        if (separator < 0) {
                            null
                        } else {
                            val key = line.substring(0, separator)
                            val value = line.substring(separator + 1)
                                .trim()
                                .removeSuffix(" kB")
                                .trim()
                                .toLongOrNull()
                            key to value
                        }
                    }.toMap()
                }
            }.getOrDefault(emptyMap())

            val memoryInfo = Debug.MemoryInfo()
            Debug.getMemoryInfo(memoryInfo)
            return ProcessMemorySnapshot(
                rssKb = status["VmRSS"],
                rssAnonKb = status["RssAnon"],
                rssFileKb = status["RssFile"],
                javaHeapKb = memoryInfo.getMemoryStat("summary.java-heap")?.toLongOrNull(),
                nativeHeapKb = memoryInfo.getMemoryStat("summary.native-heap")?.toLongOrNull(),
            )
        }
    }
}

internal fun Long?.deltaFrom(before: Long?): Long? =
    if (this != null && before != null) this - before else null
